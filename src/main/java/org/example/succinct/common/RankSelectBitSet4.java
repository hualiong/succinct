package org.example.succinct.common;

import it.unimi.dsi.bits.BitVector;
import it.unimi.dsi.bits.LongArrayBitVector;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.sux4j.bits.HintedBsearchSelect;
import it.unimi.dsi.sux4j.bits.Rank9;
import it.unimi.dsi.sux4j.bits.RankSelect;
import it.unimi.dsi.sux4j.bits.SimpleSelectZero;

import org.example.succinct.api.RankSelectBitSet;

public class RankSelectBitSet4 implements RankSelectBitSet {
    public final BitVector bits;
    public final RankSelect rankSelect;
    public final long oneCount;
    public final int size;

    // 构建器模式
    public static class Builder extends RankSelectBitSet.Builder {
        public Builder() {
            super(new LongArrayList());
        }

        public Builder(int size) {
            super(new LongArrayList(size + 63 >> 6));
        }

        @Override
        public RankSelectBitSet4 build(boolean rankSelect) {
            return new RankSelectBitSet4((LongArrayList) bits, size, count, rankSelect);
        }

        public BitVector bitVector() {
            return LongArrayBitVector.wrap(((LongArrayList) bits).toLongArray());
        }
    }

    public RankSelectBitSet4(LongArrayList bits, int size, int count, boolean rankSelect) {
        this.bits = LongArrayBitVector.wrap(bits.toLongArray());
        this.size = size;
        this.oneCount = count;

        if (rankSelect) {
            Rank9 rank = new Rank9(this.bits);
            this.rankSelect = new RankSelect(rank, new HintedBsearchSelect(rank), new SimpleSelectZero(this.bits));
            assert count == this.rankSelect.rank(size);
        } else {
            this.rankSelect = null;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public long oneCount() {
        return oneCount;
    }

    @Override
    public boolean get(int pos) {
        if (isInvalid(pos, 0, size - 1)) {
            return false;
        }
        return bits.getBoolean(pos);
    }

    @Override
    public int nextSetBit(int from) {
        // check(from, 0, size - 1);
        return (int) bits.nextOne(from);
    }

    // [0, pos]
    @Override
    public int rank1(int pos) {
        if (isInvalid(pos, 0, size - 1)) {
            return -1;
        }
        return (int) rankSelect.rank(pos + 1);
    }

    // 从1开始
    @Override
    public int select1(int k) {
        if (k == 0 || isInvalid(k, 1, oneCount)) {
            return -1;
        }
        return (int) rankSelect.select(k - 1);
    }

    // 返回位图在 [0, pos] 中 0 的个数
    @Override
    public int rank0(int pos) {
        if (isInvalid(pos, 0, size - 1)) {
            return -1;
        }
        return (int) rankSelect.rankZero(pos + 1);
    }

    // 返回位图第 k 个 0 所在的位置，等价于求：rank0(?) = k
    @Override
    public int select0(int k) {
        if (k == 0 || isInvalid(k, 1, size - oneCount)) {
            return -1;
        }
        return (int) rankSelect.selectZero(k - 1);
    }
}