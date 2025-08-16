package org.example.succinct.utils;

import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.api.SuccinctSet;
import org.openjdk.jol.info.GraphLayout;

import java.util.function.Function;

public class RamUsageUtil {
    public static String estimateSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(RamUsageEstimator.sizeOfObject(o));
    }

    public static String sizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(GraphLayout.parseInstance(o).totalSize());
    }

    public static void printSizeOf(Object o) {
        System.out.printf("%s: %s\n", o.getClass().getSimpleName(), estimateSizeOf(o));
    }

    public static void printSizeOf(String[] randoms, Function<String[], Object> f) {
        long start = Timer.now();
        Object o = f.apply(randoms);
        String name = o instanceof SuccinctSet ? o.toString() : o.getClass().getSimpleName();
        System.out.printf("%9s | %6d ms - %s\n", estimateSizeOf(o), Timer.ms(start), name);
    }
}
