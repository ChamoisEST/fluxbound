package io.github.chamoisest.fluxbound.machine.data;

import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.ChargingTransfer;
import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.MergePlan;
import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.Piece;
import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.RemovalPlan;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachinePermissions;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntToLongFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineRestructuringTest
{
    private static final int Y = 64;
    private static final long PER_BLOCK = 100_000;
    private static final IntToLongFunction CAPACITY = blocks -> PER_BLOCK * blocks;
    private static final String ID = "machine";

    private static long at(int x, int z)
    {
        return BlockPos.asLong(x, Y, z);
    }

    private static List<Long> rectangle(int width, int depth)
    {
        List<Long> result = new ArrayList<>();

        for (int x = 0; x < width; x++)
        {
            for (int z = 0; z < depth; z++)
            {
                result.add(at(x, z));
            }
        }

        return result;
    }

    private static MachineRecord machine(List<Long> members, long energy)
    {
        return new MachineRecord(ID, "minecraft:overworld", members, "owner", energy, true, MachinePermissions.DEFAULT);
    }

    // Deterministic stand-in for random UUIDs: new-1, new-2, ...
    private static Supplier<String> newIds()
    {
        int[] next = {0};
        return () -> "new-" + ++next[0];
    }

    private static RemovalPlan remove(MachineRecord machine, long position)
    {
        return MachineRestructuring.planRemoval(machine, position, CAPACITY, newIds());
    }

    private static List<Long> energies(RemovalPlan plan)
    {
        return plan.pieces().stream().map(Piece::energy).toList();
    }

    @Test
    void removingAnEdgeBlockShrinksTheMachineAndKeepsItsId()
    {
        RemovalPlan plan = remove(machine(rectangle(3, 1), 250_000), at(2, 0));

        assertEquals(1, plan.pieces().size());
        assertEquals(ID, plan.pieces().getFirst().id());
        assertEquals(List.of(at(0, 0), at(1, 0)), plan.pieces().getFirst().members());
        assertEquals(List.of(200_000L), energies(plan));
        assertEquals(50_000, plan.overflowEnergy());
    }

    @Test
    void removingTheMiddleOfA3x3FillsTheIdKeepingPieceFirst()
    {
        RemovalPlan plan = remove(machine(rectangle(3, 3), 500_000), at(1, 1));

        assertEquals(List.of(3, 3, 1, 1), plan.pieces().stream().map(piece -> piece.members().size()).toList());
        assertEquals(ID, plan.pieces().getFirst().id());
        assertEquals(List.of(300_000L, 200_000L, 0L, 0L), energies(plan));
        assertEquals(0, plan.overflowEnergy());
    }

    @Test
    void remainingPiecesAreFilledInOrder()
    {
        RemovalPlan plan = remove(machine(rectangle(3, 3), 650_000), at(1, 1));

        assertEquals(List.of(300_000L, 300_000L, 50_000L, 0L), energies(plan));
        assertEquals(0, plan.overflowEnergy());
    }

    @Test
    void energyThatNoPieceCanHoldOverflowsIntoTheItem()
    {
        RemovalPlan plan = remove(machine(rectangle(2, 1), 200_000), at(1, 0));

        assertEquals(List.of(100_000L), energies(plan));
        assertEquals(100_000, plan.overflowEnergy());
    }

    @Test
    void removingTheLastBlockPacksTheWholeMachineUnderItsId()
    {
        RemovalPlan plan = remove(machine(List.of(at(0, 0)), 70_000), at(0, 0));

        assertTrue(plan.pieces().isEmpty());
        assertEquals(70_000, plan.overflowEnergy());
        assertEquals(ID, plan.packedId());
    }

    // I1 and I2 for every block of a 3×3: no FE is created or lost, no piece is over capacity, and the pieces cover
    // exactly the remaining blocks without overlapping.
    @Test
    void everyRemovalConservesEnergyAndBlocks()
    {
        List<Long> members = rectangle(3, 3);
        long energy = 650_000;

        for (long removed : members)
        {
            RemovalPlan plan = remove(machine(members, energy), removed);

            long total = plan.overflowEnergy();
            Set<Long> covered = new HashSet<>();
            int count = 0;

            for (Piece piece : plan.pieces())
            {
                assertTrue(piece.energy() >= 0 && piece.energy() <= CAPACITY.applyAsLong(piece.members().size()));
                total += piece.energy();
                covered.addAll(piece.members());
                count += piece.members().size();
            }

            Set<Long> expected = new HashSet<>(members);
            expected.remove(removed);

            assertEquals(energy, total);
            assertEquals(expected, covered);
            assertEquals(expected.size(), count);
            assertEquals(ID, plan.pieces().getFirst().id());
        }
    }

    @Test
    void aPositionOutsideTheMachineIsRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> remove(machine(rectangle(2, 1), 0), at(5, 5)));
    }

    // ---- Merging (M2-B3). North is -z, east is +x. ----

    private static MachineRecord machine(String id, List<Long> members, long energy)
    {
        return new MachineRecord(id, "minecraft:overworld", members, "owner", energy, true, MachinePermissions.DEFAULT);
    }

    // The registry before the placement, as a lookup from position to machine.
    private static LongFunction<MachineRecord> world(MachineRecord... machines)
    {
        Map<Long, MachineRecord> byPosition = new HashMap<>();

        for (MachineRecord machine : machines)
        {
            machine.members().forEach(position -> byPosition.put(position, machine));
        }

        return byPosition::get;
    }

    private static MergePlan place(MachineRecord placed, MachineRecord... others)
    {
        MachineRecord[] all = Arrays.copyOf(others, others.length + 1);
        all[others.length] = placed;
        return MachineRestructuring.planPlacement(placed, world(all));
    }

    private static Set<Long> set(List<Long> positions)
    {
        return new HashSet<>(positions);
    }

    // The user's report behind review finding #9: a block at the free end of "aa" must give "aaa", not take a block
    // of the column "c".
    @Test
    void placementJoinsOnlyTheMachineItCompletes()
    {
        MachineRecord column = machine("c", List.of(at(0, 0), at(0, 1), at(0, 2)), 0);
        MachineRecord pair = machine("a", List.of(at(0, 3), at(1, 3)), 0);
        MachineRecord placed = machine("x", List.of(at(2, 3)), 0);

        MergePlan plan = place(placed, column, pair);

        assertEquals("a", plan.result().id());
        assertEquals(Set.of(at(0, 3), at(1, 3), at(2, 3)), set(plan.result().members()));
        assertEquals(List.of("x"), plan.absorbedIds());
    }

    @Test
    void fillingAGapCascadesThroughWholeMachines()
    {
        MachineRecord west = machine("a", List.of(at(0, 0), at(0, 1)), 10);
        MachineRecord east = machine("b", List.of(at(2, 0), at(2, 1)), 20);
        MachineRecord single = machine("x", List.of(at(1, 0)), 30);
        MachineRecord placed = machine("y", List.of(at(1, 1)), 0);

        MergePlan plan = place(placed, west, east, single);

        assertEquals(set(rectangle(3, 2)), set(plan.result().members()));
        assertEquals(60, plan.result().energy());

        // y joins the existing x (tie, first step); the pair meets b first (east before west, tie) and b's ID is
        // alphabetically smaller; the larger result then absorbs a.
        assertEquals("b", plan.result().id());
        assertEquals(List.of("y", "x", "a"), plan.absorbedIds());
    }

    @Test
    void equalNeighboursAreChosenNorthFirst()
    {
        MachineRecord west = machine("w", List.of(at(-2, 0), at(-1, 0)), 0);
        MachineRecord north = machine("n", List.of(at(0, -2), at(0, -1)), 0);

        MergePlan plan = place(machine("x", List.of(at(0, 0)), 0), west, north);

        assertEquals("n", plan.result().id());
        assertEquals(Set.of(at(0, -2), at(0, -1), at(0, 0)), set(plan.result().members()));
    }

    @Test
    void theLargerNeighbourWins()
    {
        MachineRecord west = machine("w", List.of(at(-3, 0), at(-2, 0), at(-1, 0)), 0);
        MachineRecord north = machine("n", List.of(at(0, -2), at(0, -1)), 0);

        MergePlan plan = place(machine("x", List.of(at(0, 0)), 0), west, north);

        assertEquals("w", plan.result().id());
    }

    @Test
    void machinesNeverGrowBeyondMaxSide()
    {
        List<Long> row = new ArrayList<>();

        for (int x = 0; x < 8; x++)
        {
            row.add(at(x, 0));
        }

        assertNull(place(machine("x", List.of(at(8, 0)), 0), machine("r", row, 0)));
    }

    @Test
    void placementNeverTakesPartOfAMachine()
    {
        assertNull(place(machine("x", List.of(at(2, 0)), 0), machine("s", rectangle(2, 2), 0)));
    }

    @Test
    void incompatibleNeighboursDoNotMerge()
    {
        MachineRecord placed = machine("x", List.of(at(1, 0)), 0);
        List<Long> neighbour = List.of(at(0, 0));

        MachineRecord otherOwner = new MachineRecord("o", "minecraft:overworld", neighbour, "someone else", 0, true,
                MachinePermissions.DEFAULT);
        MachineRecord otherTrusted = machine("t", neighbour, 0)
                .withPermissions(MachinePermissions.DEFAULT.withTrusted("friend", true));
        MachineRecord separate = machine("s", neighbour, 0).withAutoJoin(false);

        assertNull(place(placed, otherOwner));
        assertNull(place(placed, otherTrusted));
        assertNull(place(placed, separate));
    }

    @Test
    void aBlockPlacedWithSprintJoinsNothing()
    {
        assertNull(place(machine("x", List.of(at(1, 0)), 0).withAutoJoin(false), machine("a", List.of(at(0, 0)), 0)));
    }

    @Test
    void wrenchJoinKeepsTheSelectedMachineOnATie()
    {
        MachineRecord selected = machine("s", List.of(at(0, 0)), 5).withAutoJoin(false);
        MachineRecord target = machine("t", List.of(at(1, 0)), 7);

        MergePlan plan = MachineRestructuring.planJoin(selected, target);

        assertEquals("s", plan.result().id());
        assertEquals(List.of("t"), plan.absorbedIds());
        assertEquals(12, plan.result().energy());
        assertTrue(plan.result().autoJoin());
    }

    @Test
    void wrenchJoinKeepsTheLargerMachine()
    {
        MachineRecord selected = machine("s", List.of(at(0, 0)), 0);
        MachineRecord target = machine("t", List.of(at(1, 0), at(2, 0)), 0);

        assertEquals("t", MachineRestructuring.planJoin(selected, target).result().id());
    }

    @Test
    void wrenchJoinRefusesInvalidShapesAndOwners()
    {
        MachineRecord selected = machine("s", List.of(at(0, 0)), 0);

        assertNull(MachineRestructuring.planJoin(selected, machine("t", List.of(at(1, 1)), 0)));
        assertNull(MachineRestructuring.planJoin(selected, new MachineRecord("o", "minecraft:overworld",
                List.of(at(1, 0)), "someone else", 0, true, MachinePermissions.DEFAULT)));
    }

    // ---- Wrench joins with a default machine (M2-D1 to D3). ----

    private static MachineRecord configured(String id, List<Long> members, long energy)
    {
        return machine(id, members, energy)
                .withPermissions(MachinePermissions.DEFAULT.withMode(MachineRight.ARRIVE, AccessMode.EVERYONE));
    }

    @Test
    void wrenchJoinLetsASmallerConfiguredMachineAbsorbADefaultOne()
    {
        MachineRecord configured = configured("c", List.of(at(0, 0)), 5);
        MachineRecord plain = machine("d", List.of(at(1, 0), at(2, 0)), 7);

        // Selection order doesn't matter.
        for (MergePlan plan : List.of(MachineRestructuring.planJoin(configured, plain),
                MachineRestructuring.planJoin(plain, configured)))
        {
            assertEquals("c", plan.result().id());
            assertEquals(configured.permissions(), plan.result().permissions());
            assertEquals(List.of("d"), plan.absorbedIds());
            assertEquals(12, plan.result().energy());
            assertEquals(set(rectangle(3, 1)), set(plan.result().members()));
        }
    }

    @Test
    void wrenchJoinRefusesDifferentMeaningfulSettings()
    {
        MachineRecord trusting = machine("t", List.of(at(1, 0)), 0)
                .withPermissions(MachinePermissions.DEFAULT.withTrusted("friend", true));

        assertNull(MachineRestructuring.planJoin(configured("c", List.of(at(0, 0)), 0), trusting));
    }

    @Test
    void wrenchJoinOfADefaultMachineStillNeedsTheSameOwner()
    {
        MachineRecord foreign = new MachineRecord("o", "minecraft:overworld", List.of(at(1, 0)), "someone else", 0,
                true, MachinePermissions.DEFAULT);

        assertNull(MachineRestructuring.planJoin(configured("c", List.of(at(0, 0)), 0), foreign));
    }

    @Test
    void aStoredTeamModeCountsAsMeaningful()
    {
        MachineRecord team = machine("t", List.of(at(1, 0)), 0)
                .withPermissions(MachinePermissions.DEFAULT.withMode(MachineRight.MANAGE, AccessMode.TEAM));

        assertNull(MachineRestructuring.planJoin(configured("c", List.of(at(0, 0)), 0), team));
    }

    @Test
    void placementNeverJoinsADefaultMachineWithAConfiguredOne()
    {
        assertNull(place(machine("x", List.of(at(1, 0)), 0), configured("c", List.of(at(0, 0)), 0)));
    }

    // ---- Charging items on merges (M3-9 a). ----

    @Test
    void theSurvivorKeepsItsChargingItemAndAbsorbedOnesDrop()
    {
        MergePlan plan = new MergePlan(machine("s", List.of(at(0, 0)), 0), List.of("a", "b"));
        ChargingTransfer transfer = MachineRestructuring.planChargingItems(plan, Set.of("s", "a", "b"));

        assertNull(transfer.movedFrom());
        assertEquals(List.of("a", "b"), transfer.droppedFrom());
    }

    @Test
    void anEmptySurvivorTakesTheFirstAbsorbedItem()
    {
        MergePlan plan = new MergePlan(machine("s", List.of(at(0, 0)), 0), List.of("a", "b", "c"));
        ChargingTransfer transfer = MachineRestructuring.planChargingItems(plan, Set.of("b", "c"));

        assertEquals("b", transfer.movedFrom());
        assertEquals(List.of("c"), transfer.droppedFrom());
    }

    @Test
    void mergesWithoutChargingItemsMoveNothing()
    {
        MergePlan plan = new MergePlan(machine("s", List.of(at(0, 0)), 0), List.of("a"));
        ChargingTransfer transfer = MachineRestructuring.planChargingItems(plan, Set.of());

        assertNull(transfer.movedFrom());
        assertEquals(List.of(), transfer.droppedFrom());
    }
}
