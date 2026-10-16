package io.github.chamoisest.fluxbound.machine.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
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

    private long lastInputTick = Long.MIN_VALUE;

    public MachineEnergyStorage(int capacity, int inputPerTick, int initialEnergy,
                                LongSupplier gameTime, BooleanSupplier valid, IntConsumer onEnergyChanged)
    {
        this.gameTime = gameTime;
        this.valid = valid;

        storage = new SimpleEnergyHandler(capacity, capacity, 0, initialEnergy) {
            @Override
            protected void onEnergyChanged(int previousAmount)
            {
                onEnergyChanged.accept(getAmountAsInt());
            }
        };

        inputBudget = new SimpleEnergyHandler(inputPerTick, inputPerTick, 0);
    }

    @Override
    public long getAmountAsLong()
    {
        return valid.getAsBoolean() ? storage.getAmountAsLong() : 0;
    }

    @Override
    public long getCapacityAsLong()
    {
        return storage.getCapacityAsLong();
    }

    @Override
    public int insert(int amount, TransactionContext transaction)
    {
        if (amount < 0) {
            throw new IllegalArgumentException("Energy amount cannot be negative.");
        }

        if (!valid.getAsBoolean()) {
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
}


