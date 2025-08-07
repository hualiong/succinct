package org.example.succinct;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.common.SuccinctSet;
import org.example.succinct.test.Timer;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;

public class SuccinctSetTests {
    @Test
    public void accuracyTest() {
        int count = 20000;
        RandomStringUtils secure = RandomStringUtils.secure();
        String[] randoms = new String[count];
        Set<String> unique = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            randoms[i] = secure.next(15);
            if (i < count >>> 1)
                unique.add(randoms[i]);
        }
        String[] copyOf = Arrays.copyOf(randoms, count >>> 1);
        SuccinctSet set = ByteSuccinctSet3.of(copyOf);
        SuccinctSet charSet = CharSuccinctSet2.of(copyOf);
        for (String random : randoms) {
            boolean expected = unique.contains(random);
            assertEquals(expected, set.contains(random));
            assertEquals(expected, charSet.contains(random));
        }
    }

    @Test
    public void queryTimeTest() {
        int count = 2000000;
        String[] randoms = StringGenerateUtil.randomArray(count, 5, 0.7f);
        String[] copyOf = Arrays.copyOf(randoms, count >> 1);
        Set<String> set = Set.of(copyOf);
        SuccinctSet bss3 = ByteSuccinctSet3.of(copyOf);
        SuccinctSet bss2 = ByteSuccinctSet2.of(copyOf);
        SuccinctSet css3 = CharSuccinctSet3.of(copyOf);
        SuccinctSet css2 = CharSuccinctSet2.of(copyOf);
        SimpleFSA fsa = new SimpleFSA(copyOf);
        Timer t = new Timer();
        System.out.printf("Data: %s\n", extractSizeOf(randoms));
        t.multi(randoms, set::contains);
        System.out.printf("SetN: %dms | %s\n", t.sum(), extractSizeOf(set));
        t.reset();
        t.multi(randoms, bss3::contains);
        System.out.printf("ByteSuccinctSet3: %dms | %s\n", t.sum(), extractSizeOf(bss3));
        t.reset();
        t.multi(randoms, bss2::contains);
        System.out.printf("ByteSuccinctSet2: %dms | %s\n", t.sum(), extractSizeOf(bss2));
        t.reset();
        t.multi(randoms, css3::contains);
        System.out.printf("CharSuccinctSet3: %dms | %s\n", t.sum(), extractSizeOf(css3));
        t.reset();
        t.multi(randoms, css2::contains);
        System.out.printf("CharSuccinctSet2: %dms | %s\n", t.sum(), extractSizeOf(css2));
        t.reset();
        t.multi(randoms, fsa::contains);
        System.out.printf("FSA: %dms | %s\n", t.sum(), extractSizeOf(fsa));
    }

    @Test
    public void memoryTest() {
        String[] randoms = StringGenerateUtil.randomArray(10000000, 5, 1.0f);
        printSizeOf(randoms, r -> randoms);
        // printSizeOf(randoms, Set::of);
        printSizeOf(randoms, r -> new ByteSuccinctSet(r, "UTF-8"));
        printSizeOf(randoms, ByteSuccinctSet::of);
        printSizeOf(randoms, CharSuccinctSet::of);
    }

    public static String sizeOf(Object o) {
        long bytes = o instanceof Accountable ? RamUsageEstimator.sizeOfObject(o)
                : GraphLayout.parseInstance(o).totalSize();
        return RamUsageEstimator.humanReadableUnits(bytes);
    }

    public static String computeSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(RamUsageEstimator.sizeOfObject(o));
    }

    public static String extractSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(GraphLayout.parseInstance(o).totalSize());
    }

    // private static void printSizeOf(Object o) {
    // long bytes = o instanceof Accountable ?
    // RamUsageEstimator.sizeOf((Accountable) o)
    // : RamUsageEstimator.sizeOfObject(o);
    // System.out.printf("%s: %s\n", o,
    // RamUsageEstimator.humanReadableUnits(bytes));
    // }

    private static void printSizeOf(String[] randoms, Function<String[], Object> f) {
        long start = System.currentTimeMillis();
        Object o = f.apply(randoms);
        long bytes = o instanceof Accountable ? RamUsageEstimator.sizeOf((Accountable) o)
                : RamUsageEstimator.sizeOfObject(o);
        String name = o instanceof Accountable ? o.toString() : o.getClass().getSimpleName();
        System.out.printf("%9s | %6d ms - %s\n", RamUsageEstimator.humanReadableUnits(bytes),
                System.currentTimeMillis() - start, name);
    }

}
