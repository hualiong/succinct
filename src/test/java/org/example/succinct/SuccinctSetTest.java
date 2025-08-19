package org.example.succinct;

import org.apache.commons.lang3.RandomStringUtils;
import org.example.succinct.api.SuccinctTrie;
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
        SuccinctSet set = CharSuccinctTrie.of(copyOf);
        // SuccinctSet charSet = CharSuccinctSet4.of(copyOf);
        for (String random : randoms) {
            boolean expected = unique.contains(random);
            assertEquals(expected, set.contains(random));
            // assertEquals(expected, charSet.contains(random));
        }
    }

    @Test
    public void getTest() {
        int count = 20000;
        String[] randoms = StringGenerateUtil.randomArray(count, 10, 0.0f);
        CharSuccinctSet bss4 = CharSuccinctSet.of(randoms);
        CharSuccinctTrie cst = CharSuccinctTrie.of(randoms);
        long size = bss4.labelBitmap().oneCount();
        for (int i = 0; i < size; i++) {
            assertEquals(bss4.get(i), cst.get(i));
        }
    }

    @Test
    public void dfsTest() {
        int count = 20000;
        String[] randoms = StringGenerateUtil.randomArray(count, 10, 0.5f);
        Arrays.parallelSort(randoms);
        CharSuccinctTrie cst = CharSuccinctTrie.sortedOf(randoms);
        Iterator<String> iterator = cst.iterator(true);
        for (String s : randoms) {
            assertEquals(s, iterator.next());
        }
    }

    @Test
    public void prefixesOfTest() {
        SuccinctTrie set = CharSuccinctTrie.of("he", "hebo", "hello", "helloworld");
        assertFalse(set.prefixKeysOf("").hasNext());
        assertFalse(set.prefixKeysOf("h").hasNext());

        Iterator<String> prefixes = set.prefixKeysOf("he");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = set.prefixKeysOf("hel");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = set.prefixKeysOf("hebo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = set.prefixKeysOf("heboo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = set.prefixKeysOf("hello");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = set.prefixKeysOf("hellow");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = set.prefixKeysOf("hellew");
        assertTrue(prefixes.hasNext() && prefixes.next().equals("he"));

        prefixes = set.prefixKeysOf("helloworld");
        for (String s : List.of("he", "hello", "helloworld")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = set.prefixKeysOf("helloworlds");
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
        ByteSuccinctSet bss4 = ByteSuccinctSet.sortedOf(randoms);
        CharSuccinctSet css4 = CharSuccinctSet.sortedOf(randoms);
        SimpleFSA fsa = new SimpleFSA(randoms);
        RamUsageUtil.printSizeOf(randoms);
        RamUsageUtil.printSizeOf(trie);
        RamUsageUtil.printSizeOf(bss4);
        RamUsageUtil.printSizeOf(css4);
        RamUsageUtil.printSizeOf(fsa);
    }
}
