package org.example.succinct.common;

import java.util.Arrays;

/**
 * ascii字符数要大于31个，否则反效果
 */
public class SortedCompressedCharArray {
    private final char[] compressed;
    public final int originalLength;
    public final int asciiBoundary;
    public final int length;

    public SortedCompressedCharArray(char[] input) {
        // 对输入数组进行排序
        Arrays.sort(input);
        this.originalLength = input.length;

        // 计算ASCII字符的边界
        int asciiCount = 0;
        for (char c : input) {
            if (c <= 127) {
                asciiCount++;
            } else {
                break; // 由于数组有序，一旦遇到非ASCII字符，后面都是非ASCII字符
            }
        }
        this.asciiBoundary = asciiCount;

        // 计算压缩后所需的数组大小
        int compressedSize = input.length - (asciiCount / 2);
        this.compressed = new char[compressedSize];
        this.length = compressedSize;

        // 压缩数据
        compress(input, asciiCount);
    }

    public char get(int i) {
        if (i < 0 || i >= originalLength) {
            throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + originalLength);
        }
        // 判断是否在ASCII区域
        if (i < asciiBoundary) {
            // 在ASCII区域
            int compressedIndex = i >>> 1; // 每个压缩字符对应两个原始字符
            return (char) ((i & 1) == 0 ? compressed[compressedIndex] >> 8 : compressed[compressedIndex] & 0xFF);
        } else {
            // 在非ASCII区域
            int offset = i - asciiBoundary;
            return compressed[(asciiBoundary + 1 >>> 1) + offset];
        }
    }

    private void compress(char[] sortedInput, int asciiCount) {
        int compressedIndex = 0;

        // 压缩ASCII部分
        int i = 0;
        while (i < asciiCount) {
            if (i + 1 < asciiCount) {
                // 压缩两个ASCII字符到一个char中
                char compressedChar = (char) ((sortedInput[i] << 8) | sortedInput[i + 1]);
                compressed[compressedIndex++] = compressedChar;
                i += 2;
            } else {
                // 单个ASCII字符，直接存储
                compressed[compressedIndex++] = (char) (sortedInput[i] << 8);
                i++;
            }
        }

        // 复制非ASCII部分
        while (i < sortedInput.length) {
            compressed[compressedIndex++] = sortedInput[i++];
        }
    }

    public char[] decompress() {
        char[] result = new char[originalLength];
        int resultIndex = 0;

        // 解压ASCII部分
        int compressedIndex = 0;
        while (resultIndex < asciiBoundary) {
            if (resultIndex + 1 < asciiBoundary) {
                // 提取高位和低位的ASCII字符
                char high = (char) (compressed[compressedIndex] >> 8);
                char low = (char) (compressed[compressedIndex] & 0xFF);

                result[resultIndex++] = high;
                result[resultIndex++] = low;
                compressedIndex++;
            } else {
                // 单个ASCII字符
                result[resultIndex++] = (char) (compressed[compressedIndex++] >> 8);
            }
        }

        // 复制非ASCII部分
        while (resultIndex < originalLength) {
            result[resultIndex++] = compressed[compressedIndex++];
        }

        return result;
    }

    @Override
    public String toString() {
        return String.format("CompressedCharArray(compressedSize=%d, originalSize=%d, asciiBoundary=%d, compressionRatio=%.2f%%)",
                compressed.length, originalLength, asciiBoundary, (double) compressed.length / originalLength * 100);
    }
}