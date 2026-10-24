package io.github.chamoisest.fluxbound.machine.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;

public final class MachineEnergyStorage implements EnergyHandler
{
    private final SimpleEnergyHandler storage;
    private final SimpleEnergyHandler inputBudget;

    private final LongSupplier gameTime;
    private final BooleanSupplier valid;

    private boolean active = true;
    private long suspendedInputThroughTick = Long.MIN_VALUE;

    private long lastInputTick = Long.MIN_VALUE;

    public MachineEnergyStorage(int capacity, int inputPerTick, int initialEnergy,
                                LongSupplier gameTime, BooleanSupplier valid, IntConsumer onEnergyChanged)
    {
        this.gameTime = gameTime;
        this.valid = valid;

        // Internally extractable for charging (chargeInto); the public extract() below still refuses everything.
        storage = new SimpleEnergyHandler(capacity, capacity, capacity, initialEnergy) {
            @Override
            protected void onEnergyChanged(int previousAmount)
            {
                if (active) {
                    onEnergyChanged.accept(getAmountAsInt());
                }
            }
        };

        inputBudget = new SimpleEnergyHandler(inputPerTick, inputPerTick, 0);
    }

    @Override
    public long getAmountAsLong()
    {
        return active && valid.getAsBoolean() ? storage.getAmountAsLong() : 0;
    }

    @Override
    public long getCapacityAsLong()
    {
        return active ? storage.getCapacityAsLong() : 0;
    }

    @Override
    public int insert(int amount, TransactionContext transaction)
    {
        if (amount < 0) {
            throw new IllegalArgumentException("Energy amount cannot be negative.");
        }

        if (!active || !valid.getAsBoolean() || gameTime.getAsLong() <= suspendedInputThroughTick) {
            return 0;
        }

        long currentTick = gameTime.getAsLong();

        if (currentTick != lastInputTick) {
            inputBudget.set(0);
            lastInputTick = currentTick;
        }

        int available = inputBudget.getCapacityAsInt() - inputBudget.getAmountAsInt();
        int accepted = storage.insert(Math.min(amount, available), transaction);

        if (inputBudget.insert(accepted, transaction) != accepted) {
            throw new IllegalStateException("Machine energy input budget became inconsistent.");
        }

        return accepted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction)
    {
        if (amount < 0) {
            throw new IllegalArgumentException("Energy amount cannot be negative.");
        }

        return 0;
    }

    // Internal only: charging moves FE from the machine into an item in one transaction (C3). Cables can't extract
    // (extract() stays 0), because stationary machines don't act as batteries (spec §11). From M5, reserved FE must be
    // excluded here (spec §13).
    public int chargeInto(EnergyHandler target, int maxAmount)
    {
        if (!active || !valid.getAsBoolean())
        {
            return 0;
        }

        return EnergyHandlerUtil.move(storage, target, maxAmount, null);
    }

    public void invalidate()
    {
        active = false;
    }

    public void suspendInputThrough(long tick)
    {
        suspendedInputThroughTick = tick;
    }
}


