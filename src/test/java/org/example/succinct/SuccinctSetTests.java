package org.example.succinct;

import org.apache.commons.lang3.RandomStringUtils;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.api.SuccinctSet;
import org.example.succinct.core.*;
import org.example.succinct.test.Recorder;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Test;

import static org.example.succinct.utils.RamUsageUtil.printSizeOf;
import static org.example.succinct.utils.RamUsageUtil.sizeOf;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

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
        SuccinctSet charSet = CharSuccinctSet3.of(copyOf);
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
        Recorder t = new Recorder();
        System.out.printf("Data: %s\n", sizeOf(randoms));
        t.multi(randoms, set::contains);
        System.out.printf("SetN: %dms | %s\n", t.sum(), sizeOf(set));
        t.reset();
        t.multi(randoms, bss3::contains);
        System.out.printf("ByteSuccinctSet3: %dms | %s\n", t.sum(), sizeOf(bss3));
        t.reset();
        t.multi(randoms, bss2::contains);
        System.out.printf("ByteSuccinctSet2: %dms | %s\n", t.sum(), sizeOf(bss2));
        t.reset();
        t.multi(randoms, css3::contains);
        System.out.printf("CharSuccinctSet3: %dms | %s\n", t.sum(), sizeOf(css3));
        t.reset();
        t.multi(randoms, css2::contains);
        System.out.printf("CharSuccinctSet2: %dms | %s\n", t.sum(), sizeOf(css2));
        t.reset();
        t.multi(randoms, fsa::contains);
        System.out.printf("FSA: %dms | %s\n", t.sum(), sizeOf(fsa));
    }

    @Test
    public void memoryTest() {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 8, 0.0f);
        printSizeOf(randoms, r -> randoms);
        Arrays.parallelSort(randoms);
        // printSizeOf(randoms, r -> new ByteSuccinctSet(r, "UTF-8"));
        // printSizeOf(randoms, ByteSuccinctSet::of);
        printSizeOf(randoms, r -> new CharSuccinctSet(r, true));
        printSizeOf(randoms, SimpleFSA::new);
    }

}
