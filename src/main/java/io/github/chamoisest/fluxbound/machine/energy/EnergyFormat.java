package io.github.chamoisest.fluxbound.machine.energy;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Compact energy numbers in the most common mod style, matching Jade: up to two decimals, trailing zeros
// dropped, rounded down, suffix without a space (950, 1.5k, 64k, 123.45k, 2.45M, 4M). Rounding down means a
// machine never looks fuller than it is (decisions.md, D5).
public final class EnergyFormat
{
    private static final String[] SUFFIXES = {"k", "M", "G", "T", "P", "E"};
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private EnergyFormat() {}

    public static String compact(long value)
    {
        BigDecimal scaled = BigDecimal.valueOf(value).abs();
        int suffix = -1;

        while (scaled.compareTo(THOUSAND) >= 0 && suffix < SUFFIXES.length - 1)
        {
            scaled = scaled.movePointLeft(3);
            suffix++;
        }

        // toPlainString avoids scientific notation such as "1E+2" after stripping zeros from 100.
        String number = scaled.setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
        return (value < 0 ? "-" : "") + number + (suffix < 0 ? "" : SUFFIXES[suffix]);
    }

    // "2.45M / 4M FE"
    public static String stored(long energy, long capacity)
    {
        return compact(energy) + " / " + compact(capacity) + " FE";
    }

    // "64k FE/t"
    public static String perTick(long amount)
    {
        return compact(amount) + " FE/t";
    }
}
