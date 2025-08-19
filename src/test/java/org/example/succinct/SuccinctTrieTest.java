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
    private SuccinctTrie trie;
    private SuccinctTrie trie2;

    @Before
    public void setUp() {
        // 初始化测试数据
        randoms = StringGenerateUtil.randomArray(COUNT, 10, 0.5f);
        Arrays.parallelSort(randoms);

        // 初始化测试实例
        trie = CharSuccinctTrie.sortedOf(randoms);
        trie2 = CharSuccinctTrie.of("he", "hebo", "hello", "helloworld");
    }

    @Test
    public void indexTest() {
        Set<String> unique = Arrays.stream(randoms).collect(Collectors.toSet());
        for (String random : randoms) {
            int index = trie.index(random);
            assertEquals(unique.contains(random), index > 0);
            assertEquals(random, trie.get(index));
        }
    }

    @Test
    public void getTest() {
        CharSuccinctSet css = CharSuccinctSet.sortedOf(randoms);
        long size = css.labelBitmap().oneCount();
        for (int i = 0; i < size; i++) {
            assertEquals(css.get(i), trie.get(i));
        }
    }

    @Test
    public void dfsTest() {
        Iterator<String> iterator = trie.iterator(true);
        for (String s : randoms) {
            assertEquals(s, iterator.next());
        }
    }

    @Test
    public void prefixesOfTest() {
        assertFalse(trie2.prefixKeysOf("").hasNext());
        assertFalse(trie2.prefixKeysOf("h").hasNext());

        Iterator<String> prefixes = trie2.prefixKeysOf("he");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = trie2.prefixKeysOf("hel");
        assertTrue(prefixes.hasNext() && "he".equals(prefixes.next()));

        prefixes = trie2.prefixKeysOf("hebo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = trie2.prefixKeysOf("heboo");
        for (String s : List.of("he", "hebo")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = trie2.prefixKeysOf("hello");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = trie2.prefixKeysOf("hellow");
        for (String s : List.of("he", "hello")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = trie2.prefixKeysOf("hellew");
        assertTrue(prefixes.hasNext() && prefixes.next().equals("he"));

        prefixes = trie2.prefixKeysOf("helloworld");
        for (String s : List.of("he", "hello", "helloworld")) {
            assertEquals(s, prefixes.next());
        }
        prefixes = trie2.prefixKeysOf("helloworlds");
        for (String s : List.of("he", "hello", "helloworld")) {
            assertEquals(s, prefixes.next());
        }
    }
}