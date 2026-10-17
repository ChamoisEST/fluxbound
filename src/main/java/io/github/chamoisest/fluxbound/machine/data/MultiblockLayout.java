package io.github.chamoisest.fluxbound.machine.data;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MultiblockLayout
{
    private MultiblockLayout() {}

    public static boolean isRectangle(Set<Long> members, int maxSide)
    {
        if (members.isEmpty()) {
            return false;
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int y = BlockPos.of(members.iterator().next()).getY();

        for (long packed : members) {
            BlockPos pos = BlockPos.of(packed);

            if (pos.getY() != y) {
                return false;
            }

            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        long width = (long) maxX - minX + 1;
        long depth = (long) maxZ - minZ + 1;

        if (width > maxSide || depth > maxSide || width * depth != members.size()) {
            return false;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!members.contains(BlockPos.asLong(x, y, z))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static List<List<Long>> partition(Set<Long> members, int maxSide)
    {
        Set<Long> remaining = new HashSet<>(members);
        List<List<Long>> result = new ArrayList<>();

        while (!remaining.isEmpty()) {
            List<Long> best = List.of();

            for (long packed : remaining.stream().sorted().toList()) {
                BlockPos origin = BlockPos.of(packed);

                for (int width = 1; width <= maxSide; width++) {
                    for (int depth = 1; depth <= maxSide; depth++) {
                        if (width * depth <= best.size() || width * depth > remaining.size()) {
                            continue;
                        }

                        List<Long> candidate = new ArrayList<>();
                        boolean complete = true;

                        for (int dx = 0; dx < width && complete; dx++) {
                            for (int dz = 0; dz < depth; dz++) {
                                long position = BlockPos.asLong(
                                        origin.getX() + dx, origin.getY(), origin.getZ() + dz);

                                if (!remaining.contains(position)) {
                                    complete = false;
                                    break;
                                }

                                candidate.add(position);
                            }
                        }

                        if (complete) {
                            best = candidate;
                        }
                    }
                }
            }

            result.add(best);
            remaining.removeAll(best);
        }

        result.sort(Comparator.<List<Long>>comparingInt(List::size).reversed());
        return result;
    }
}
