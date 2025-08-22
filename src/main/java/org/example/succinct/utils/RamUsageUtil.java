package org.example.succinct.utils;

import org.apache.lucene.util.RamUsageEstimator;
import org.openjdk.jol.info.GraphLayout;


public class RamUsageUtil {
    public static String estimateSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(RamUsageEstimator.sizeOfObject(o));
    }

    public static String sizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(GraphLayout.parseInstance(o).totalSize());
    }

    public static void printSizeOf(Object o) {
        System.out.printf("%s: %s\n", o.getClass().getSimpleName(), sizeOf(o));
    }

    public static void printEstimateSizeOf(Object o) {
        System.out.printf("%s: %s\n", o.getClass().getSimpleName(), estimateSizeOf(o));
    }
}
