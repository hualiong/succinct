package org.example.succinct.utils;

import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
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
        long start = System.currentTimeMillis();
        Object o = f.apply(randoms);
        long bytes = RamUsageEstimator.sizeOfObject(o);
        String name = o instanceof Accountable ? o.toString() : o.getClass().getSimpleName();
        System.out.printf("%9s | %6d ms - %s\n", RamUsageEstimator.humanReadableUnits(bytes),
                System.currentTimeMillis() - start, name);
    }
}
