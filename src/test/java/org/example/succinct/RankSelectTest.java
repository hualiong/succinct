package org.example.succinct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.example.succinct.common.RankSelectBitSet3;
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
        RankSelectBitSet3.Builder builder = new RankSelectBitSet3.Builder();
        for (int i = 0; i < seq.length; i++) {
            builder.set(i, seq[i]);
        }
        RankSelectBitSet3 bitSet = builder.build(true);
        int k = -1, c = 0;
        while ((k = bitSet.nextSetBit(k + 1)) >= 0) {
            assertTrue(bitSet.get(k));
            c++;
        }
        assertEquals(rank1[rank1.length - 1], c);
        for (int i = 0; i < rank1.length; i++) {
            assertEquals(rank1[i], bitSet.rank1(i));
        }
        for (int i = 0; i < select1.length; i++) {
            assertEquals(select1[i], bitSet.select1(i + 1));
        }
        for (int i = 0; i < rank0.length; i++) {
            assertEquals(rank0[i], bitSet.rank0(i));
        }
        for (int i = 0; i < select0.length; i++) {
            assertEquals(select0[i], bitSet.select0(i + 1));
        }
    }

}
