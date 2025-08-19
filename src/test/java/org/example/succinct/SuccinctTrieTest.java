package org.example.succinct;

import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.core.*;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SuccinctTrieTest {
    private static final int COUNT = 20000;
    private String[] randoms;
    private Set<String> unique;
    private CharSuccinctTrie cst;
    private SuccinctTrie prefixCst;

    @Before
    public void setUp() {
        // 初始化测试数据
        randoms = StringGenerateUtil.randomArray(COUNT, 10, 0.5f);
        Arrays.parallelSort(randoms);
        unique = Arrays.stream(randoms).collect(Collectors.toSet());

        // 初始化测试实例
        cst = CharSuccinctTrie.sortedOf(randoms);
        prefixCst = CharSuccinctTrie.of("he", "hebo", "hello", "helloworld");
    }

    @Test
    public void containsTest() {
        for (String random : randoms) {
            assertEquals(unique.contains(random), cst.contains(random));
        }
    }

    @Test
    public void getTest() {
        CharSuccinctSet bss4 = CharSuccinctSet.sortedOf(randoms);
        long size = bss4.labelBitmap().oneCount();
        for (int i = 0; i < size; i++) {
            assertEquals(bss4.get(i), cst.get(i));
        }
    }

    @Test
    public void dfsTest() {
        Iterator<String> iterator = cst.iterator(true);
        for (String s : randoms) {
            assertEquals(s, iterator.next());
        }
    }

    @Test
    public void prefixesOfTest() {
        assertFalse(prefixCst.prefixKeysOf("").hasNext());
        assertFalse(prefixCst.prefixKeysOf("h").hasNext());

        Iterator<String> prefixes = prefixCst.prefixKeysOf("he");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = prefixCst.prefixKeysOf("hel");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = prefixCst.prefixKeysOf("hebo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = prefixCst.prefixKeysOf("heboo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = prefixCst.prefixKeysOf("hello");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = prefixCst.prefixKeysOf("hellow");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = prefixCst.prefixKeysOf("hellew");
        assertTrue(prefixes.hasNext() && prefixes.next().equals("he"));

        prefixes = prefixCst.prefixKeysOf("helloworld");
        for (String s : List.of("he", "hello", "helloworld")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = prefixCst.prefixKeysOf("helloworlds");
        for (String s : List.of("he", "hello", "helloworld")) {
            assertEquals(s, prefixes.next());
        }
    }

    // @Test
    // public void memoryTest() {
    //     String[] largeRandoms = StringGenerateUtil.randomArray(1000000, 8, 0.0f);
    //     Arrays.parallelSort(largeRandoms);
    //     PatriciaTrie pTrie = new PatriciaTrie();
    //     for (String random : largeRandoms) {
    //         pTrie.insert(random);
    //     }
    //     InlinedTailLOUDSTrie trie = new InlinedTailLOUDSTrie(pTrie);
    //     ByteSuccinctSet4 bss4 = ByteSuccinctSet4.sortedOf(largeRandoms);
    //     CharSuccinctSet4 css4 = CharSuccinctSet4.sortedOf(largeRandoms);
    //     SimpleFSA fsa = new SimpleFSA(largeRandoms);
    //     RamUsageUtil.printSizeOf(largeRandoms);
    //     RamUsageUtil.printSizeOf(trie);
    //     RamUsageUtil.printSizeOf(bss4);
    //     RamUsageUtil.printSizeOf(css4);
    //     RamUsageUtil.printSizeOf(fsa);
    // }
}