package org.example.succinct.common;

import it.unimi.dsi.bits.LongArrayBitVector;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.sux4j.bits.RankSelect;
import it.unimi.dsi.sux4j.bits.SparseSelect;
import org.apache.lucene.util.Accountable;
import org.openjdk.jol.info.GraphLayout;

public class RankSelectBitSet3 implements Accountable {
    public final int size;
    private final LongArrayBitVector bits;
    private final RankSelect rankSelect;

    // 构建器模式
    public static class Builder {
        private final LongArrayList bits = new LongArrayList();
        private int size = 0;

        public void set(int position, boolean value) {
            ensureCapacity(position);
            int block = position >> 6;
            int offset = position & 0x3F;
            long mask = 1L << offset;
            if (value) {
                bits.set(block, bits.getLong(block) | mask);
            } else {
                bits.set(block, bits.getLong(block) & ~mask);
            }
        }

        private void ensureCapacity(int position) {
            int requiredBlocks = (position >> 6) + 1;
            while (bits.size() < requiredBlocks) {
                bits.add(0L);
            }
            size = Math.max(size, position + 1);
        }

        public RankSelectBitSet3 build(boolean rankSelect) {
            return new RankSelectBitSet3(bits, size, rankSelect);
        }
    }

    private RankSelectBitSet3(LongArrayList bits, int size, boolean rankSelect) {
        this.bits = LongArrayBitVector.wrap(bits.toLongArray(), size);
        this.size = size;

        if (rankSelect) {
            SparseSelect select = new SparseSelect(this.bits);
            this.rankSelect = new RankSelect(select.getRank(), select);
        } else {
            this.rankSelect = null;
        }
    }

    public boolean get(int pos) {
        if (pos < 0 || pos >= size) {
            return false;
        }
        return bits.getBoolean(pos);
    }

    public int nextSetBit(long from) {
        if (from < 0 || from >= size) {
            return -1;
        }
        return (int) bits.nextOne(from);
    }

    // [0, pos]
    public int rank1(int pos) {
        if (pos < 0 || pos >= size) {
            return 0;
        }
        return (int) rankSelect.rank(pos + 1);
    }

    // 从1开始
    public int select1(int k) {
        return (int) rankSelect.select(k - 1);
    }

    // 返回位图在 [0, pos] 中 0 的个数
    public int rank0(int pos) {
        if (pos >= size) {
            pos = size - 1;
        }
        return pos + 1 - rank1(pos);
    }

    // 返回位图第 k 个 0 所在的位置，等价于求：rank0(?) = k
    public int select0(int k) {
        if (k <= 0 || k > rank1(size - 1)) {
            return -1;
        }
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

    @Override
    public long ramBytesUsed() {
        return GraphLayout.parseInstance(this).totalSize();
    }
}