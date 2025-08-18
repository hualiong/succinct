package org.example.succinct;

import org.apache.commons.lang3.RandomStringUtils;
import org.example.succinct.api.SuccinctSet2;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.api.SuccinctSet;
import org.example.succinct.core.*;
import org.example.succinct.utils.RamUsageUtil;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Test;
import org.trie4j.louds.InlinedTailLOUDSTrie;
import org.trie4j.patricia.PatriciaTrie;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
    public void prefixesOfTest() {
        SuccinctSet2 set = ByteSuccinctSet4.of("he", "hebo", "hello", "helloworld");
        assertFalse(set.prefixesOf("").hasNext());
        assertFalse(set.prefixesOf("h").hasNext());

        Iterator<String> prefixes = set.prefixesOf("he");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = set.prefixesOf("hel");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = set.prefixesOf("hebo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }

        prefixes = set.prefixesOf("heboo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }

        prefixes = set.prefixesOf("hello");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }

        prefixes = set.prefixesOf("hellow");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }

        prefixes = set.prefixesOf("hellew");
        assertTrue(prefixes.hasNext() && prefixes.next().equals("he"));

        prefixes = set.prefixesOf("helloworld");
        for (String s : List.of("he", "hello", "helloworld")) {
            assertEquals(s, prefixes.next());
        }

        prefixes = set.prefixesOf("helloworlds");
        for (String s : List.of("he", "hello", "helloworld")) {
            assertEquals(s, prefixes.next());
        }
    }

    @Test
    public void memoryTest() {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 8, 0.0f);
        Arrays.parallelSort(randoms);
        PatriciaTrie pTrie = new PatriciaTrie();
        for (String random : randoms) {
            pTrie.insert(random);
        }
        InlinedTailLOUDSTrie trie = new InlinedTailLOUDSTrie(pTrie);
        ByteSuccinctSet4 bss4 = ByteSuccinctSet4.of(randoms);
        CharSuccinctSet4 css4 = CharSuccinctSet4.sortedOf(randoms);
        SimpleFSA fsa = new SimpleFSA(randoms);
        RamUsageUtil.printSizeOf(randoms);
        RamUsageUtil.printSizeOf(trie);
        RamUsageUtil.printSizeOf(bss4);
        RamUsageUtil.printSizeOf(css4);
        RamUsageUtil.printSizeOf(fsa);
    }
}
