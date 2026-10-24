package io.github.chamoisest.fluxbound.machine.data;

import io.github.chamoisest.fluxbound.machine.permission.MachineRights;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntToLongFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;

// How machines change shape. Outcomes are computed as plans without touching any state, so they can be checked and
// unit-tested before MachineRegistryData applies them (P6, M2 G5-B). Merging moves here in M2-B3.
public final class MachineRestructuring
{
    public static final int MAX_SIDE = 8;

    private MachineRestructuring() {}

    // One machine that remains after a removal.
    public record Piece(String id, List<Long> members, long energy) {}

    // The outcome of removing one block. The first piece keeps the original ID. overflowEnergy no longer fits into the
    // pieces; it goes into a dropped item if there is one and is lost otherwise. packedId is the identity such an item
    // gets: the original one when the last block is removed, a new one otherwise.
    public record RemovalPlan(List<Piece> pieces, long overflowEnergy, String packedId) {}

    // The outcome of combining machines: result is the merged machine under the surviving ID; absorbedIds are the
    // machines that stop existing. FE is added up, which always fits because every machine is within its capacity.
    public record MergePlan(MachineRecord result, List<String> absorbedIds) {}

    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    // Only the affected machine changes (M2 G1-3): its remaining blocks are cut into rectangles, largest first with
    // deterministic ties, and the largest keeps the ID. FE fills the pieces one after another in this order, starting
    // with the ID-keeping piece. Capacity comes in as a function, so the plan depends neither on loaded config nor on
    // the world.
    public static RemovalPlan planRemoval(MachineRecord machine, long removedPosition, IntToLongFunction capacity,
                                          Supplier<String> newIds)
    {
        if (!machine.members().contains(removedPosition))
        {
            throw new IllegalArgumentException("Position " + removedPosition + " is not part of machine " + machine.id());
        }

        Set<Long> remaining = new HashSet<>(machine.members());
        remaining.remove(removedPosition);

        List<List<Long>> parts = remaining.isEmpty() ? List.of() : MultiblockLayout.partition(remaining, MAX_SIDE);
        List<Piece> pieces = new ArrayList<>();
        long energyLeft = machine.energy();

        // Pieces are filled one after another, largest first: the ID-keeping piece, then the next one, and so on
        // (M2 G1-3, user decisions). Empty pieces become default items when broken, so clearing up an intentional
        // split leaves stackable items. Whatever no piece can hold becomes overflow, so no FE is created or lost.
        for (int i = 0; i < parts.size(); i++)
        {
            List<Long> members = parts.get(i).stream().sorted().toList();
            long share = Math.min(energyLeft, capacity.applyAsLong(members.size()));

            pieces.add(new Piece(i == 0 ? machine.id() : newIds.get(), members, share));
            energyLeft -= share;
        }

        String packedId = remaining.isEmpty() ? machine.id() : newIds.get();
        return new RemovalPlan(List.copyOf(pieces), energyLeft, packedId);
    }

    // The rule for automatic merging (I4): same owner and identical meaningful data. From M4, links and network
    // membership join this check (spec §6). Wrench joins also use joinable.
    public static boolean compatible(MachineRecord first, MachineRecord second)
    {
        return sameOwnerAndDimension(first, second)
                && first.permissions().sameFor(MachineRights.of(first), second.permissions());
    }

    // Wrench joins only (M2-D1 to D3, user decisions 2026-10-24): a machine without meaningful settings may also join a
    // configured one of the same owner, which keeps its settings. Automatic placement never does this, so placing a
    // block never changes an existing machine's settings.
    private static boolean joinable(MachineRecord first, MachineRecord second)
    {
        return compatible(first, second)
                || (sameOwnerAndDimension(first, second)
                        && (!hasMeaningfulSettings(first) || !hasMeaningfulSettings(second)));
    }

    // Ownerless machines (placed without a player) never combine.
    private static boolean sameOwnerAndDimension(MachineRecord first, MachineRecord second)
    {
        return !first.owner().isEmpty()
                && first.owner().equals(second.owner())
                && first.dimension().equals(second.dimension());
    }

    private static boolean hasMeaningfulSettings(MachineRecord machine)
    {
        return !machine.permissions().isDefaultFor(MachineRights.of(machine));
    }

    // Where charging items go when machines merge (M3-9 a, C2): the survivor keeps its own; if it has none, the first
    // absorbed item in plan order moves into it; every other absorbed item drops. Pure, so the order stays testable.
    public record ChargingTransfer(@Nullable String movedFrom, List<String> droppedFrom) {}

    public static ChargingTransfer planChargingItems(MergePlan plan, Set<String> withItems)
    {
        boolean survivorHasItem = withItems.contains(plan.result().id());
        String movedFrom = null;
        List<String> dropped = new ArrayList<>();

        for (String id : plan.absorbedIds())
        {
            if (!withItems.contains(id))
            {
                continue;
            }

            if (!survivorHasItem && movedFrom == null)
            {
                movedFrom = id;
            }
            else
            {
                dropped.add(id);
            }
        }

        return new ChargingTransfer(movedFrom, List.copyOf(dropped));
    }

    // A placed block joins whole neighbouring machines only, never parts of one (M2 G1-1, I5). After each merge the
    // result looks for the next fitting neighbour as a whole, until none fits. Bounded: every step removes a machine.
    // machineAt reads the registry as it was before the placement; null means nothing merges.
    public static @Nullable MergePlan planPlacement(MachineRecord placed, LongFunction<MachineRecord> machineAt)
    {
        if (!placed.autoJoin())
        {
            return null;
        }

        MachineRecord current = placed;
        List<String> absorbed = new ArrayList<>();

        while (true)
        {
            MachineRecord neighbour = bestNeighbour(current, machineAt);

            if (neighbour == null)
            {
                break;
            }

            MachineRecord survivor = survivor(current, neighbour, absorbed.isEmpty());
            absorbed.add(survivor == current ? neighbour.id() : current.id());
            current = combine(survivor, current, neighbour);
        }

        return absorbed.isEmpty() ? null : new MergePlan(current, List.copyOf(absorbed));
    }

    // A wrench join of exactly two machines, without cascade (M2 G1-4). Sprint-placed machines may join; the result
    // joins automatically from then on.
    public static @Nullable MergePlan planJoin(MachineRecord first, MachineRecord second)
    {
        if (first.id().equals(second.id()) || !joinable(first, second) || !formsRectangle(first, second))
        {
            return null;
        }

        MachineRecord survivor = joinSurvivor(first, second);
        MachineRecord absorbed = survivor == first ? second : first;

        return new MergePlan(combine(survivor, first, second).withAutoJoin(true), List.of(absorbed.id()));
    }

    // The machine with meaningful settings survives even if it is smaller (M2-D3, spec §6). Otherwise the larger one
    // survives, the first-selected one on a tie.
    private static MachineRecord joinSurvivor(MachineRecord first, MachineRecord second)
    {
        boolean firstMeaningful = hasMeaningfulSettings(first);

        if (firstMeaningful != hasMeaningfulSettings(second))
        {
            return firstMeaningful ? first : second;
        }

        return second.blockCount() > first.blockCount() ? second : first;
    }

    // The fitting neighbour: largest first, ties by the side it touches in the order North, East, South, West
    // (M2 G1-2). It fits if it joins automatically, is compatible and the union is a rectangle within MAX_SIDE.
    private static @Nullable MachineRecord bestNeighbour(MachineRecord current, LongFunction<MachineRecord> machineAt)
    {
        Set<Long> own = new HashSet<>(current.members());
        List<Long> members = current.members().stream().sorted().toList();
        Set<String> seen = new HashSet<>();
        MachineRecord best = null;

        for (Direction side : SIDES)
        {
            for (long member : members)
            {
                long position = BlockPos.of(member).relative(side).asLong();

                // Positions inside the result still map to the machines it absorbed, because the registry isn't
                // changed while planning.
                if (own.contains(position))
                {
                    continue;
                }

                MachineRecord neighbour = machineAt.apply(position);

                if (neighbour == null || !seen.add(neighbour.id()) || !neighbour.autoJoin()
                        || !compatible(current, neighbour) || !formsRectangle(current, neighbour))
                {
                    continue;
                }

                // Sides are visited in tie order, so only a strictly larger neighbour replaces an earlier one.
                if (best == null || neighbour.blockCount() > best.blockCount())
                {
                    best = neighbour;
                }
            }
        }

        return best;
    }

    // The larger machine keeps its ID (M2 G1-2). Ties: the existing neighbour beats the placed block in the first step,
    // the alphabetically smaller ID wins later in the cascade.
    private static MachineRecord survivor(MachineRecord current, MachineRecord neighbour, boolean firstStep)
    {
        if (current.blockCount() != neighbour.blockCount())
        {
            return current.blockCount() > neighbour.blockCount() ? current : neighbour;
        }

        if (firstStep)
        {
            return neighbour;
        }

        return current.id().compareTo(neighbour.id()) <= 0 ? current : neighbour;
    }

    private static boolean formsRectangle(MachineRecord first, MachineRecord second)
    {
        Set<Long> union = new HashSet<>(first.members());
        union.addAll(second.members());

        return union.size() == first.blockCount() + second.blockCount()
                && MultiblockLayout.isRectangle(union, MAX_SIDE);
    }

    // The survivor's settings are kept. They equal the other machine's, except for rights the machine doesn't use or,
    // in a wrench join, when the other machine had no meaningful settings.
    private static MachineRecord combine(MachineRecord survivor, MachineRecord first, MachineRecord second)
    {
        List<Long> members = new ArrayList<>(first.members());
        members.addAll(second.members());

        return survivor.withMembers(members.stream().sorted().toList())
                .withEnergy(first.energy() + second.energy());
    }
}
