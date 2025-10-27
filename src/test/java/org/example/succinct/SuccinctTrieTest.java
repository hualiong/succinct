package org.example.succinct;

import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.core.*;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public class SuccinctTrieTest {
    static final int COUNT = 10000;
    final Function<String[], SuccinctTrie> constructor = NestedSuccinctTrie::of;
    String[] unordered;
    Set<String> unique = new TreeSet<>();
    SuccinctTrie trie;

    @Before
    public void setUp() {
        unordered = StringGenerateUtil.randomArray(COUNT, 0, 10, 0.5f);
        unique = new TreeSet<>();
        String[] half = Arrays.copyOf(unordered, COUNT / 2);
        unique.addAll(Arrays.asList(half));
        trie = constructor.apply(half);
    }

    @Test
    public void indexAndGetTest() {
        int[] array = new int[trie.nodeCount()];
        Arrays.setAll(array, i -> i);
        for (String random : unordered) {
            int index = trie.index(random);
            assertEquals(unique.contains(random), index >= 0);
            if (index >= 0) {
                assertEquals(random, trie.get(index));
                array[index] = -1;
            }
        }
        Arrays.stream(array).filter(i -> i > 0).forEach(i -> assertNull(trie.get(i)));
    }

    @Test
    public void dfsTest() {
        if (!(trie instanceof ByteSuccinctTrie)) {
            Iterator<String> iterator = trie.iterator(true);
            for (String s : unique) {
                assertEquals(s, iterator.next());
            }
        }
    }

    @Test
    public void prefixesOfTest() {
        SuccinctTrie trie2 = constructor.apply(new String[]{"he", "hebo", "hello", "helloworld"});
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
