package org.example.succinct;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.example.succinct.test.Timer;
import org.junit.Test;

public class RankSelectTest {

    @Test
    public void simpleTest() {
        boolean[] seq = new boolean[] {
                false, false, true, false, false, true, false, true, false,
                true, false, true, false, true, false, true, true, true, true
        };
        // seq: 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1
        int[] rank1 = new int[] { 0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8, 9, 10 };
        int[] rank0 = new int[] { 1, 2, 2, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 9, 9, 9 };
        int[] select1 = new int[] { 2, 5, 7, 9, 11, 13, 15, 16, 17, 18 };
        int[] select0 = new int[] { 0, 1, 3, 4, 6, 8, 10, 12, 14 };
        RankSelectBitSet.Builder builder = new RankSelectBitSet1.Builder(seq.length);
        for (int i = 0; i < seq.length; i++) {
            builder.set(i, seq[i]);
        }
        RankSelectBitSet bitSet = builder.build(true);
        int k = -1, c = 0;
        while ((k = bitSet.nextSetBit(k + 1)) >= 0) {
            assertTrue(bitSet.get(k));
            c++;
        }
        assertEquals(rank1[rank1.length - 1], c);
        for (int i = 0; i < bitSet.size(); i++) {
            assertEquals(rank1[i], bitSet.rank1(i));
        }
        for (int i = 0; i < bitSet.oneCount(); i++) {
            assertEquals(select1[i], bitSet.select1(i + 1));
        }
        for (int i = 0; i < bitSet.size(); i++) {
            assertEquals(rank0[i], bitSet.rank0(i));
        }
        for (int i = 0; i < bitSet.size() - bitSet.oneCount(); i++) {
            assertEquals(select0[i], bitSet.select0(i + 1));
        }
    }

    @Test
    public void speedTest() {
        int size = 10000000;
        RankSelectBitSet4.Builder builder = new RankSelectBitSet4.Builder(size);
        for (int i = 0; i < size; i++) {
            builder.set(i, Math.random() < 0.5);
        }
        RankSelectBitSet4 bitSet = builder.build(true);
        System.out.printf("memory: %s\n", sizeOf(bitSet));
        long t = Timer.now();
        for (int i = 0; i < bitSet.size; i++) {
            bitSet.get(i);
        }
        long t0 = Timer.now();
        System.out.printf("get: %d ms\n", Timer.ms(t, t0));
        for (int i = 0; i < bitSet.size; i++) {
            bitSet.rank1(i);
        }
        long t1 = Timer.now();
        System.out.printf("rank1: %d ms\n", Timer.ms(t0, t1));
        for (int i = 0; i < bitSet.oneCount; i++) {
            bitSet.select1(i + 1);
        }
        long t2 = Timer.now();
        System.out.printf("select1: %d ms\n", Timer.ms(t1, t2));
        for (int i = 0; i < bitSet.size; i++) {
            bitSet.rank0(i);
        }
        long t3 = Timer.now();
        System.out.printf("rank0: %d ms\n", Timer.ms(t2, t3));
        for (int i = 0; i < bitSet.size - bitSet.oneCount; i++) {
            bitSet.select0(i + 1);
        }
        long t4 = Timer.now();
        System.out.printf("select0: %d ms\n", Timer.ms(t3, t4));
    }

    @Test
    public void rankTest() {
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

    @Test
    public void selectTest() {
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
}
