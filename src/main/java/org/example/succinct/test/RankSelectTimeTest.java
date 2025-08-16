package org.example.succinct.test;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import it.unimi.dsi.bits.BitVector;
import it.unimi.dsi.bits.LongArrayBitVector;
import it.unimi.dsi.sux4j.bits.HintedBsearchSelect;
import it.unimi.dsi.sux4j.bits.Rank;
import it.unimi.dsi.sux4j.bits.Rank11;
import it.unimi.dsi.sux4j.bits.Rank16;
import it.unimi.dsi.sux4j.bits.Rank9;
import it.unimi.dsi.sux4j.bits.Select;
import it.unimi.dsi.sux4j.bits.Select9;
import it.unimi.dsi.sux4j.bits.SimpleSelect;
import it.unimi.dsi.sux4j.bits.SparseRank;
import it.unimi.dsi.sux4j.bits.SparseSelect;
import org.example.succinct.api.RankSelectBitSet;
import org.example.succinct.common.*;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;

public class RankSelectTimeTest {
    public static void main(String[] args) {
        allTest();
    }

    public static void allTest() {
        String[] keys = StringGenerateUtil.randomArray(1000000, 5, 1.0f);
        RankSelectBitSet.Builder builder = new RankSelectBitSet4.Builder();
        RankSelectBitSet bitSet = createLoudsBits(builder, keys);
        System.out.printf("memory: %s\n", sizeOf(bitSet));
        long t = Timer.now();
        for (int i = 0; i < bitSet.size(); i++) {
            bitSet.get(i);
        }
        long t0 = Timer.now();
        System.out.printf("get: %d ms\n", Timer.ms(t, t0));
        for (int i = 0; i < bitSet.size(); i++) {
            bitSet.rank1(i);
        }
        long t1 = Timer.now();
        System.out.printf("rank1: %d ms\n", Timer.ms(t0, t1));
        for (int i = 0; i < bitSet.oneCount(); i++) {
            bitSet.select1(i + 1);
        }
        long t2 = Timer.now();
        System.out.printf("select1: %d ms\n", Timer.ms(t1, t2));
        for (int i = 0; i < bitSet.size(); i++) {
            bitSet.rank0(i);
        }
        long t3 = Timer.now();
        System.out.printf("rank0: %d ms\n", Timer.ms(t2, t3));
        for (int i = 0; i < bitSet.size() - bitSet.oneCount(); i++) {
            bitSet.select0(i + 1);
        }
        long t4 = Timer.now();
        System.out.printf("select0: %d ms\n", Timer.ms(t3, t4));
    }

    public static void rankTest() {
        int size = 10000000;
        BitVector bits = LongArrayBitVector.getInstance(size);
        for (int i = 0; i < size; i++) {
            bits.add(Math.random() < 0.5);
        }
        Rank rank11 = new Rank11(bits);
        Rank rank9 = new Rank9(bits);
        Rank rank16 = new Rank16(bits);
        Rank sparseRank = new SparseRank(bits);
        long t0 = Timer.now();
        for (int i = 0; i < size; i++) {
            rank11.rank(i);
        }
        long t1 = Timer.now();
        for (int i = 0; i < size; i++) {
            rank9.rank(i);
        }
        long t2 = Timer.now();
        for (int i = 0; i < size; i++) {
            rank16.rank(i);
        }
        long t3 = Timer.now();
        for (int i = 0; i < size; i++) {
            sparseRank.rank(i);
        }
        long t4 = Timer.now();
        System.out.printf("rank11: %d ms | %s\n", Timer.ms(t0, t1), sizeOf(rank11));
        System.out.printf("rank9: %d ms | %s\n", Timer.ms(t1, t2), sizeOf(rank9));
        System.out.printf("rank16: %d ms | %s\n", Timer.ms(t2, t3), sizeOf(rank16));
        System.out.printf("sparseRank: %d ms | %s\n", Timer.ms(t3, t4), sizeOf(sparseRank));
    }

    public static void selectTest() {
        int size = 10000000;
        BitVector bits = LongArrayBitVector.getInstance(size);
        for (int i = 0; i < size; i++) {
            bits.add(Math.random() < 0.5);
        }
        Rank9 rank9 = new Rank9(bits);
        Select simpleSelect = new SimpleSelect(bits);
        Select select9 = new Select9(rank9);
        Select hintedBsearchSelect = new HintedBsearchSelect(rank9);
        Select sparseSelect = new SparseSelect(bits);
        long count = rank9.count();
        long t0 = Timer.now();
        for (int i = 0; i < count; i++) {
            simpleSelect.select(i);
        }
        long t1 = Timer.now();
        for (int i = 0; i < count; i++) {
            select9.select(i);
        }
        long t2 = Timer.now();
        for (int i = 0; i < count; i++) {
            hintedBsearchSelect.select(i);
        }
        long t3 = Timer.now();
        for (int i = 0; i < count; i++) {
            sparseSelect.select(i);
        }
        long t4 = Timer.now();
        System.out.printf("simpleSelect: %d ms | %s\n", Timer.ms(t0, t1), sizeOf(simpleSelect));
        System.out.printf("select9: %d ms | %s\n", Timer.ms(t1, t2), sizeOf(select9));
        System.out.printf("hintedBsearchSelect: %d ms | %s\n", Timer.ms(t2, t3), sizeOf(hintedBsearchSelect));
        System.out.printf("sparseSelect: %d ms | %s\n", Timer.ms(t3, t4), sizeOf(sparseSelect));
    }

    private static RankSelectBitSet createLoudsBits(RankSelectBitSet.Builder builder, String[] keys) {
        Arrays.parallelSort(keys);
        Queue<Range> queue = new ArrayDeque<>();
        queue.add(new Range(0, keys.length, 0));
        int bitPos = 0;
        while (!queue.isEmpty()) {
            Range range = queue.poll();
            int L = range.L(), R = range.R(), index = range.index();
            int ptr = L;
            while (ptr < R && keys[ptr].length() == index) {
                ptr++;
            }
            int start = L;
            while (start < R) {
                if (keys[start].length() <= index) {
                    start++;
                    continue;
                }
                char currentChar = keys[start].charAt(index);
                int end = start + 1;
                while (end < R) {
                    if (keys[end].length() <= index || keys[end].charAt(index) != currentChar) {
                        break;
                    }
                    end++;
                }
                builder.set(bitPos++, false);
                queue.add(new Range(start, end, index + 1));
                start = end;
            }
            builder.set(bitPos++, true);
        }
        return builder.build(true);
    }
}
