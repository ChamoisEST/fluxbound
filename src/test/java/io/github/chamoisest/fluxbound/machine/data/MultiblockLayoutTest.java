package io.github.chamoisest.fluxbound.machine.data;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockLayoutTest
{
    private static final int MAX_SIDE = 8;
    private static final int Y = 64;

    // Positions on one horizontal layer, given as x/z pairs.
    private static Set<Long> blocks(int... xz)
    {
        Set<Long> result = new HashSet<>();

        for (int i = 0; i < xz.length; i += 2)
        {
            result.add(BlockPos.asLong(xz[i], Y, xz[i + 1]));
        }

        return result;
    }

    private static Set<Long> row(int length)
    {
        Set<Long> result = new HashSet<>();

        for (int x = 0; x < length; x++)
        {
            result.add(BlockPos.asLong(x, Y, 0));
        }

        return result;
    }

    @Test
    void fullRectangleIsValid()
    {
        assertTrue(MultiblockLayout.isRectangle(blocks(0, 0, 1, 0, 0, 1, 1, 1), MAX_SIDE));
    }

    @Test
    void lShapeIsNotARectangle()
    {
        assertFalse(MultiblockLayout.isRectangle(blocks(0, 0, 1, 0, 0, 1), MAX_SIDE));
    }

    @Test
    void sidesAreLimitedToMaxSide()
    {
        assertTrue(MultiblockLayout.isRectangle(row(8), MAX_SIDE));
        assertFalse(MultiblockLayout.isRectangle(row(9), MAX_SIDE));
    }

    @Test
    void blocksAtDifferentHeightsAreNotARectangle()
    {
        Set<Long> members = blocks(0, 0);
        members.add(BlockPos.asLong(1, Y + 1, 0));

        assertFalse(MultiblockLayout.isRectangle(members, MAX_SIDE));
    }

    // Removing the middle of a 3×3 leaves a ring: two rows of three and two single blocks.
    @Test
    void ringIsCutIntoRectanglesLargestFirst()
    {
        Set<Long> ring = blocks(0, 0, 1, 0, 2, 0, 0, 1, 2, 1, 0, 2, 1, 2, 2, 2);
        List<List<Long>> parts = MultiblockLayout.partition(ring, MAX_SIDE);

        assertEquals(List.of(3, 3, 1, 1), parts.stream().map(List::size).toList());

        Set<Long> covered = new HashSet<>();
        parts.forEach(covered::addAll);
        assertEquals(ring, covered);
        assertEquals(ring.size(), parts.stream().mapToInt(List::size).sum());
    }

    @Test
    void partitionIsDeterministic()
    {
        Set<Long> ring = blocks(0, 0, 1, 0, 2, 0, 0, 1, 2, 1, 0, 2, 1, 2, 2, 2);

        assertEquals(MultiblockLayout.partition(ring, MAX_SIDE), MultiblockLayout.partition(new HashSet<>(ring), MAX_SIDE));
    }
}
