package org.example.succinct;

import org.example.succinct.common.SortedCompressedCharArray;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import java.util.Arrays;

public class SortedCompressedCharArrayTest {
    private SortedCompressedCharArray asciiOnlyArray;
    private SortedCompressedCharArray mixedArray;
    private SortedCompressedCharArray nonAsciiOnlyArray;
    private SortedCompressedCharArray emptyArray;
    private SortedCompressedCharArray singleAsciiArray;
    private SortedCompressedCharArray singleNonAsciiArray;
    private SortedCompressedCharArray oddAsciiArray;

    @Before
    public void setUp() {
        // ASCII 字符数组
        char[] asciiData = {'a', 'b', 'c', 'd', 'e', '1', '2', '3'};
        asciiOnlyArray = new SortedCompressedCharArray(asciiData);

        // 混合字符数组
        char[] mixedData = {'a', 'b', 'c', 'd', 'e', '你', '好', '世', '界', '1', '2', '3'};
        mixedArray = new SortedCompressedCharArray(mixedData);

        // 非ASCII字符数组
        char[] nonAsciiData = {'你', '好', '世', '界'};
        nonAsciiOnlyArray = new SortedCompressedCharArray(nonAsciiData);

        // 空数组
        char[] emptyData = {};
        emptyArray = new SortedCompressedCharArray(emptyData);

        // 单个ASCII字符数组
        char[] singleAsciiData = {'a'};
        singleAsciiArray = new SortedCompressedCharArray(singleAsciiData);

        // 单个非ASCII字符数组
        char[] singleNonAsciiData = {'你'};
        singleNonAsciiArray = new SortedCompressedCharArray(singleNonAsciiData);

        // 奇数个ASCII字符数组
        char[] oddAsciiData = {'a', 'b', 'c'};
        oddAsciiArray = new SortedCompressedCharArray(oddAsciiData);
    }

    @Test
    public void testGet() {
        // 测试ASCII字符数组
        char[] sortedAscii = {'1', '2', '3', 'a', 'b', 'c', 'd', 'e'};
        for (int i = 0; i < sortedAscii.length; i++) {
            assertEquals(sortedAscii[i], asciiOnlyArray.get(i));
        }

        // 测试混合字符数组
        char[] sortedMixed = {'1', '2', '3', 'a', 'b', 'c', 'd', 'e', '世', '你', '好', '界'};
        for (int i = 0; i < sortedMixed.length; i++) {
            assertEquals(sortedMixed[i], mixedArray.get(i));
        }

        // 测试非ASCII字符数组
        char[] sortedNonAscii = {'世', '你', '好', '界'};
        for (int i = 0; i < sortedNonAscii.length; i++) {
            assertEquals(sortedNonAscii[i], nonAsciiOnlyArray.get(i));
        }

        // 测试单个ASCII字符
        assertEquals('a', singleAsciiArray.get(0));

        // 测试单个非ASCII字符
        assertEquals('你', singleNonAsciiArray.get(0));

        // 测试奇数个ASCII字符
        char[] sortedOddAscii = {'a', 'b', 'c'};
        for (int i = 0; i < sortedOddAscii.length; i++) {
            assertEquals(sortedOddAscii[i], oddAsciiArray.get(i));
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOutOfBoundsNegative() {
        asciiOnlyArray.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOutOfBoundsPositive() {
        asciiOnlyArray.get(asciiOnlyArray.originalLength);
    }

    @Test
    public void testDecompress() {
        // 测试ASCII字符数组的解压缩
        char[] asciiDecompressed = asciiOnlyArray.decompress();
        char[] sortedAscii = {'1', '2', '3', 'a', 'b', 'c', 'd', 'e'};
        assertArrayEquals(sortedAscii, asciiDecompressed);

        // 测试混合字符数组的解压缩
        char[] mixedDecompressed = mixedArray.decompress();
        char[] sortedMixed = {'1', '2', '3', 'a', 'b', 'c', 'd', 'e', '世', '你', '好', '界'};
        assertArrayEquals(sortedMixed, mixedDecompressed);

        // 测试非ASCII字符数组的解压缩
        char[] nonAsciiDecompressed = nonAsciiOnlyArray.decompress();
        char[] sortedNonAscii = {'世', '你', '好', '界'};
        assertArrayEquals(sortedNonAscii, nonAsciiDecompressed);

        // 测试空数组的解压缩
        char[] emptyDecompressed = emptyArray.decompress();
        assertArrayEquals(new char[0], emptyDecompressed);

        // 测试单个ASCII字符的解压缩
        char[] singleAsciiDecompressed = singleAsciiArray.decompress();
        assertArrayEquals(new char[]{'a'}, singleAsciiDecompressed);

        // 测试单个非ASCII字符的解压缩
        char[] singleNonAsciiDecompressed = singleNonAsciiArray.decompress();
        assertArrayEquals(new char[]{'你'}, singleNonAsciiDecompressed);

        // 测试奇数个ASCII字符的解压缩
        char[] oddAsciiDecompressed = oddAsciiArray.decompress();
        char[] sortedOddAscii = {'a', 'b', 'c'};
        assertArrayEquals(sortedOddAscii, oddAsciiDecompressed);
    }

    @Test
    public void testEdgeCases() {
        // 测试大量ASCII字符
        char[] largeAsciiData = new char[1000];
        for (int i = 0; i < 1000; i++) {
            largeAsciiData[i] = (char)('a' + (i % 26));
        }
        SortedCompressedCharArray largeAsciiArray = new SortedCompressedCharArray(largeAsciiData);

        // 压缩率应为50%
        assertEquals(0.5, (double) largeAsciiArray.length / largeAsciiArray.originalLength, 0.001);

        // 验证所有字符都能正确访问
        Arrays.sort(largeAsciiData);
        for (int i = 0; i < largeAsciiData.length; i++) {
            assertEquals(largeAsciiData[i], largeAsciiArray.get(i));
        }

        // 测试大量混合字符
        char[] largeMixedData = new char[1000];
        for (int i = 0; i < 1000; i++) {
            if (i < 500) {
                largeMixedData[i] = (char)('a' + (i % 26));
            } else {
                largeMixedData[i] = (char)(0x4E00 + (i % 100)); // 中文字符范围
            }
        }
        SortedCompressedCharArray largeMixedArray = new SortedCompressedCharArray(largeMixedData);

        // 验证所有字符都能正确访问
        Arrays.sort(largeMixedData);
        for (int i = 0; i < largeMixedData.length; i++) {
            assertEquals(largeMixedData[i], largeMixedArray.get(i));
        }
    }

    @Test
    public void benchmarkTest() {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 8, 0.0f);
        char[] chars = new char[randoms.length * randoms[0].length()];
        int index = 0;
        for (String str : randoms) {
            for (char c : str.toCharArray()) {
                chars[index++] = c;
            }
        }
        SortedCompressedCharArray compressed = new SortedCompressedCharArray(chars);
        System.out.println(compressed);

        long t0 = Timer.now();
        for (int i = 0; i < chars.length; i++) {
            assertNotNull(compressed.get(i));
        }
        long t1 = Timer.now();
        for (int i = 0; i < chars.length; i++) {
            assertNotNull(chars[i]);
        }
        long t2 = Timer.now();
        System.out.println(Timer.ms(t0, t1) + "ms");
        System.out.println(Timer.ms(t1, t2) + "ms");
    }
}
