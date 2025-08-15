package org.example.succinct.common;

import it.unimi.dsi.bits.LongArrayBitVector;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.sux4j.bits.RankSelect;
import it.unimi.dsi.sux4j.bits.SparseSelect;
import org.example.succinct.api.RankSelectBitSet;

public class RankSelectBitSet3 implements RankSelectBitSet {
    private final LongArrayBitVector bits;
    private final RankSelect rankSelect;
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
        public RankSelectBitSet3 build(boolean rankSelect) {
            return new RankSelectBitSet3((LongArrayList) bits, size, rankSelect);
        }
    }

    private RankSelectBitSet3(LongArrayList bits, int size, boolean rankSelect) {
        this.bits = LongArrayBitVector.wrap(bits.toLongArray());
        this.size = size;

        if (rankSelect) {
            SparseSelect select = new SparseSelect(this.bits);
            this.rankSelect = new RankSelect(select.getRank(), select);
            this.oneCount = this.rankSelect.rank(size);
        } else {
            this.oneCount = 0;
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
        check(pos, 0, size - 1);
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
        check(pos, 0, size - 1);
        return (int) rankSelect.rank(pos + 1);
    }

    // 从1开始
    @Override
    public int select1(int k) {
        check(k, 1, oneCount);
        return (int) rankSelect.select(k - 1);
    }

    // 返回位图在 [0, pos] 中 0 的个数
    @Override
    public int rank0(int pos) {
        check(pos, 0, size - 1);
        return pos + 1 - rank1(pos);
    }

    // 返回位图第 k 个 0 所在的位置，等价于求：rank0(?) = k
    @Override
    public int select0(int k) {
        check(k, 1, size - oneCount);
        int low = 0, high = size - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            // 计算[0, mid]区间内的0的个数
            if (rank0(mid) < k) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        // 满足rank0(low) >= k的最小位置即第k个0的位置
        return low;
    }

    private static void check(long n, long min, long max) {
        if (n < min || n > max) {
            throw new IndexOutOfBoundsException("Index (" + n + ") is not in valid range [" + min + ", " + max + "]");
        }
    }
}