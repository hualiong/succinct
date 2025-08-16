package org.example.succinct;

import org.apache.commons.lang3.RandomStringUtils;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.api.SuccinctSet;
import org.example.succinct.core.*;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Test;

import static org.example.succinct.utils.RamUsageUtil.printSizeOf;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

public class SuccinctSetTest {
    @Test
    public void containsTest() {
        int count = 20000;
        RandomStringUtils secure = RandomStringUtils.secure();
        String[] randoms = new String[count];
        Set<String> unique = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            randoms[i] = secure.next(15);
            if (i < count >>> 1) {
                unique.add(randoms[i]);
            }
        }
        String[] copyOf = Arrays.copyOf(randoms, count >>> 1);
        SuccinctSet set = ByteSuccinctSet4.of(copyOf);
        SuccinctSet charSet = CharSuccinctSet4.of(copyOf);
        for (String random : randoms) {
            boolean expected = unique.contains(random);
            assertEquals(expected, set.contains(random));
            assertEquals(expected, charSet.contains(random));
        }
    }

    @Test
    public void getTest() {
        int count = 20000;
        String[] randoms = StringGenerateUtil.randomArray(count, 10, 0.0f);
        ByteSuccinctSet4 bss4 = ByteSuccinctSet4.of(randoms);
        ByteSuccinctSet3 bss3 = ByteSuccinctSet3.of(randoms);
        long size = bss4.labelBitmap().oneCount();
        for (int i = 0; i < size; i++) {
            assertEquals(bss3.get(i), bss4.get(i));
        }
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
