package org.example.succinct;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.common.SuccinctSet;
import org.example.succinct.test.Timer;
import org.example.succinct.utils.StringCodecUtil;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;

public class SuccinctSetTests {
    @Test
    public void simpleTest() {
        // 测试1: 空集合
        ByteSuccinctSet emptySet = ByteSuccinctSet.of();
        assertFalse(emptySet.contains(""));
        assertFalse(emptySet.contains("any"));

        // 测试2: 单个字符串
        ByteSuccinctSet singleSet = ByteSuccinctSet.of("hello");
        assertTrue(singleSet.contains("hello"));
        assertFalse(singleSet.contains("hell"));
        assertFalse(singleSet.contains("hello!"));
        assertFalse(singleSet.contains("he"));
        assertFalse(singleSet.contains(""));

        // 测试3: 包含空字符串
        ByteSuccinctSet emptyStringSet = ByteSuccinctSet.of("");
        assertTrue(emptyStringSet.contains(""));
        assertFalse(emptyStringSet.contains("a"));

        // 测试4: 共同前缀
        String[] prefixKeys = new String[] { "apple", "app", "application", "applet", "approve" };
        ByteSuccinctSet prefixSet = ByteSuccinctSet.of(prefixKeys);
        Arrays.stream(prefixKeys).forEach(s -> assertTrue(prefixSet.contains(s)));
        assertTrue(prefixSet.contains("app"));
        assertFalse(prefixSet.contains("ap"));
        assertFalse(prefixSet.contains("appl"));
        assertFalse(prefixSet.contains("applepie"));
        assertFalse(prefixSet.contains("apps"));

        // 测试5: 包含子字符串关系
        String[] substringKeys = new String[] { "cat", "catalog", "category", "dog", "dogma" };
        ByteSuccinctSet substringSet = ByteSuccinctSet.of(substringKeys);
        assertTrue(substringSet.contains("cat"));
        assertTrue(substringSet.contains("catalog"));
        assertTrue(substringSet.contains("category"));
        assertFalse(substringSet.contains("cate"));
        assertFalse(substringSet.contains("catalyst"));
        assertTrue(substringSet.contains("dog"));
        assertTrue(substringSet.contains("dogma"));
        assertFalse(substringSet.contains("do"));

        // 测试6: 特殊字符
        String[] specialKeys = new String[] { "!", "!!", "!@#", "@", "@@", "\\n", " ", "123", "12_3" };
        ByteSuccinctSet specialSet = ByteSuccinctSet.of(specialKeys);
        for (String key : specialKeys) {
            assertTrue(specialSet.contains(key));
        }
        assertTrue(specialSet.contains("!"));
        assertTrue(specialSet.contains("!!"));
        assertFalse(specialSet.contains("!@"));
        assertTrue(specialSet.contains("@@"));
        assertTrue(specialSet.contains(" "));
        assertFalse(specialSet.contains("  "));
        assertTrue(specialSet.contains("123"));
        assertFalse(specialSet.contains("12"));
        assertTrue(specialSet.contains("12_3"));

        // 测试7: 大小写敏感
        String[] caseKeys = new String[] { "Apple", "apple", "APPLE" };
        ByteSuccinctSet caseSet = ByteSuccinctSet.of(caseKeys);
        assertTrue(caseSet.contains("Apple"));
        assertTrue(caseSet.contains("apple"));
        assertTrue(caseSet.contains("APPLE"));
        assertFalse(caseSet.contains("App"));
        assertFalse(caseSet.contains("aPpLe"));

        // 测试8: 长字符串和边界情况
        String longStr1 = "this-is-a-very-long-string-to-test-the-implementation-of-succinct-trie";
        String longStr2 = "this-is-a-very-long-string-to-test-the-implementation-of-succinct-trie-with-modification";
        String longStr3 = "antidisestablishmentarianism";
        String[] longKeys = new String[] { longStr1, longStr2, longStr3 };
        ByteSuccinctSet longSet = ByteSuccinctSet.of(longKeys);
        assertTrue(longSet.contains(longStr1));
        assertTrue(longSet.contains(longStr2));
        assertTrue(longSet.contains(longStr3));
        assertFalse(longSet.contains(longStr1.substring(0, 20)));
        assertFalse(longSet.contains(longStr2 + "x"));
        assertFalse(longSet.contains("antidisestablishment"));

        // 测试9: 随机字符串
        String[] randomKeys = new String[20];
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            int length = 5 + random.nextInt(15 - 5 + 1);
            StringBuilder sb = new StringBuilder(length);
            for (int j = 0; j < length; j++) {
                int index = random.nextInt(characters.length());
                sb.append(characters.charAt(index));
            }
            randomKeys[i] = sb.toString();
        }
        ByteSuccinctSet randomSet = ByteSuccinctSet.of(randomKeys);
        for (String key : randomKeys) {
            assertTrue(randomSet.contains(key));
        }
        assertFalse(randomSet.contains("not-in-set"));
        assertFalse(randomSet.contains("random"));

        // 测试10: 密集前缀树
        String[] denseKeys = new String[9];
        int index = 0;
        for (char c1 = 'a'; c1 <= 'c'; c1++) {
            for (char c2 = 'a'; c2 <= 'c'; c2++) {
                denseKeys[index++] = "" + c1 + c2;
            }
        }
        ByteSuccinctSet denseSet = ByteSuccinctSet.of(denseKeys);
        assertTrue(denseSet.contains("aa"));
        assertTrue(denseSet.contains("bb"));
        assertTrue(denseSet.contains("cc"));
        assertFalse(denseSet.contains("a"));
        assertFalse(denseSet.contains("abc"));
        assertFalse(denseSet.contains("dd"));

        // 测试11: 混合长度字符串
        String[] mixedKeys = new String[] { "", "a", "ab", "abc", "abcd", "abcde", "abcdef" };
        ByteSuccinctSet mixedSet = ByteSuccinctSet.of(mixedKeys);
        assertTrue(mixedSet.contains(""));
        assertTrue(mixedSet.contains("a"));
        assertTrue(mixedSet.contains("ab"));
        assertTrue(mixedSet.contains("abc"));
        assertTrue(mixedSet.contains("abcd"));
        assertTrue(mixedSet.contains("abcde"));
        assertTrue(mixedSet.contains("abcdef"));
        assertFalse(mixedSet.contains("abcdefg"));
        assertFalse(mixedSet.contains("b"));
        assertFalse(mixedSet.contains("ac"));
    }

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
        SuccinctSet set = ByteSuccinctSet2.of(copyOf);
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
        String[] randoms = StringCodecUtil.randomArray(count, 8, 0.0f);
        String[] copyOf = Arrays.copyOf(randoms, count >>> 1);
        Set<String> unique = Set.of(copyOf);
        SuccinctSet set = ByteSuccinctSet.of(copyOf);
        SuccinctSet set2 = ByteSuccinctSet2.of(copyOf);
        SuccinctSet charSet = CharSuccinctSet.of(copyOf);
        SuccinctSet charSet2 = CharSuccinctSet2.of(copyOf);
        SimpleFSA fsa = new SimpleFSA(copyOf);
        // ZFastTrie<String> trie = new ZFastTrie<>(Arrays.stream(copyOf).iterator(), TransformationStrategies.prefixFreeIso());
        Timer t1 = new Timer();
        Timer t2 = new Timer();
        Timer t22 = new Timer();
        Timer t3 = new Timer();
        Timer t32 = new Timer();
        Timer t4 = new Timer();
        // Timer t5 = new Timer();
        t1.multi(unique::contains, randoms);
        t2.multi(set::contains, randoms);
        t22.multi(set2::contains, randoms);
        t3.multi(charSet::contains, randoms);
        t32.multi(charSet2::contains, randoms);
        t4.multi(fsa::contains, randoms);
        // t5.multi(trie::contains, randoms);
        System.out.printf("copyOf: %s\n", extractSizeOf(copyOf));
        System.out.printf("SetN: %dms | %s\n", t1.sum(), extractSizeOf(unique));
        System.out.printf("ByteSuccinctSet: %dms | %s\n", t2.sum(), extractSizeOf(set));
        System.out.printf("ByteSuccinctSet2: %dms | %s\n", t22.sum(), extractSizeOf(set2));
        System.out.printf("CharSuccinctSet: %dms | %s\n", t3.sum(), extractSizeOf(charSet));
        System.out.printf("CharSuccinctSet2: %dms | %s\n", t32.sum(), extractSizeOf(charSet2));
        System.out.printf("FSA: %dms | %s\n", t4.sum(), extractSizeOf(fsa));
        // System.out.printf("ZFastTrie: %dms | %s\n", t5.sum(), extractSizeOf(trie));
    }

    @Test
    public void memoryTest() {
        String[] randoms = StringCodecUtil.randomArray(10000000, 5, 1.0f);
        printSizeOf(randoms, r -> randoms);
        // printSizeOf(randoms, Set::of);
        printSizeOf(randoms, r -> new ByteSuccinctSet(r, "UTF-8"));
        printSizeOf(randoms, ByteSuccinctSet::of);
        printSizeOf(randoms, CharSuccinctSet::of);
    }

    public static String sizeOf(Object o) {
        long bytes = o instanceof Accountable ? RamUsageEstimator.sizeOf((Accountable) o)
                : GraphLayout.parseInstance(o).totalSize();
        return RamUsageEstimator.humanReadableUnits(bytes);
    }

    private static String extractSizeOf(Object o) {
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
