package org.example.succinct.common;

import org.apache.lucene.util.Accountable;
import org.openjdk.jol.info.GraphLayout;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.sux4j.util.EliasFanoMonotoneLongBigList;

public class RankSelectBitSet2 implements Accountable {
    private final long[] bits;
    private final EliasFanoMonotoneLongBigList ranks1;
    private final EliasFanoMonotoneLongBigList selects1;
    public final int size;

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

        public RankSelectBitSet2 build(boolean rankSelect) {
            return new RankSelectBitSet2(bits, size, rankSelect);
        }
    }

    private RankSelectBitSet2(LongArrayList bits, int size, boolean rankSelect) {
        this.bits = bits.toLongArray();
        this.size = size;

        if (rankSelect) {
            IntArrayList ranks1 = new IntArrayList(bits.size() + 1);
            IntArrayList selects1 = new IntArrayList();
            int totalOnes = 0;
            for (int i = 0; i < bits.size(); i++) {
                ranks1.add(totalOnes);
                int blockOnes = Long.bitCount(bits.getLong(i));
                totalOnes += blockOnes;

                long block = bits.getLong(i);
                for (int j = 0; j < 64; j++) {
                    if ((block & (1L << j)) != 0) {
                        selects1.add((i << 6) + j);
                    }
                }
            }
            ranks1.add(totalOnes);
            this.ranks1 = new EliasFanoMonotoneLongBigList(ranks1);
            this.selects1 = new EliasFanoMonotoneLongBigList(selects1);
        } else {
            this.ranks1 = null;
            this.selects1 = null;
        }
    }

    public boolean get(int pos) {
        if (pos < 0 || pos >= size) {
            return false;
        }
        int block = pos >> 6;
        int offset = pos & 0x3F;
        return (bits[block] & (1L << offset)) != 0;
    }

    public int nextSetBit(long from) {
        if (from < 0 || from >= size) {
            return -1;
        }
        int u = (int) (from >> 6);
        long word;
        for(word = this.bits[u] & -1L << from; word == 0L; word = this.bits[u]) {
            if (++u == bits.length) {
                return -1;
            }
        }
        return (u << 6) + Long.numberOfTrailingZeros(word);
    }

    // [0, pos]
    public int rank1(int pos) {
        if (pos < 0 || pos >= size) {
            return 0;
        }
        int block = pos + 1 >> 6;
        int offset = pos + 1 & 0x3F;
        int count = (int) ranks1.getLong(block);

        if (offset > 0) {
            long mask = (1L << offset) - 1;
            count += Long.bitCount(bits[block] & mask);
        }
        return count;
    }

    // 从1开始
    public int select1(long k) {
        return (int) selects1.getLong(k - 1);
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
        if (k <= 0 || k > ranks1.getLong(bits.length)) {
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