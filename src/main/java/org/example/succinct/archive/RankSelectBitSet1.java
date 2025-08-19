package org.example.succinct.archive;

import java.util.ArrayList;
import java.util.List;

import org.example.succinct.api.RankSelectBitSet;

@SuppressWarnings("unused")
public class RankSelectBitSet1 implements RankSelectBitSet {
    private static final int GAP = 64;
    private final long[] bits;
    private final int[] ranks1;
    private final int[] selects1;
    public final int size;

    // 构建器模式
    public static class Builder extends RankSelectBitSet.Builder {
        public Builder() {
            super(new ArrayList<>());
        }

        public Builder(int size) {
            super(new ArrayList<>(size + 63 >> 6));
        }

        @Override
        public RankSelectBitSet1 build(boolean rankSelect) {
            long[] array = new long[bits.size()];
            for (int i = 0; i < bits.size(); i++) {
                array[i] = bits.get(i);
            }
            return new RankSelectBitSet1(array, size, rankSelect);
        }
    }

    private RankSelectBitSet1(long[] bits, int size, boolean rankSelect) {
        this.bits = bits;
        this.size = size;

        if (rankSelect) {
            this.ranks1 = new int[bits.length + 1];
            List<Integer> selectList = new ArrayList<>();
            // 预计算rank和select
            int totalOnes = 0;
            int oneCount = 0;
            for (int i = 0; i < bits.length; i++) {
                ranks1[i] = totalOnes;
                int blockOnes = Long.bitCount(bits[i]);
                totalOnes += blockOnes;

                long block = bits[i];
                for (int j = 0; j < 64; j++) {
                    if ((block & (1L << j)) != 0) {
                        oneCount++;
                        if (oneCount % GAP == 0) {
                            selectList.add((i << 6) + j);
                        }
                    }
                }
            }
            ranks1[bits.length] = totalOnes;

            this.selects1 = new int[selectList.size()];
            for (int i = 0; i < selectList.size(); i++) {
                selects1[i] = selectList.get(i);
            }
        } else {
            this.ranks1 = new int[0];
            this.selects1 = new int[0];
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public long oneCount() {
        return ranks1[bits.length];
    }

    @Override
    public boolean get(int pos) {
        if (pos < 0 || pos >= size) {
            return false;
        }
        int block = pos >> 6;
        int offset = pos & 0x3F;
        return (bits[block] & (1L << offset)) != 0;
    }

    @Override
    public int nextSetBit(int from) {
        if (from < 0 || from >= size) {
            return -1;
        }
        int u = from >> 6;
        long word;
        for(word = this.bits[u] & -1L << from; word == 0L; word = this.bits[u]) {
            if (++u == bits.length) {
                return -1;
            }
        }
        return (u << 6) + Long.numberOfTrailingZeros(word);
    }

    // [0, pos]
    @Override
    public int rank1(int pos) {
        if (pos < 0 || pos >= size) {
            return 0;
        }
        int block = pos + 1 >> 6;
        int offset = pos + 1 & 0x3F;
        int count = ranks1[block];

        if (offset > 0) {
            long mask = (1L << offset) - 1;
            count += Long.bitCount(bits[block] & mask);
        }
        return count;
    }

    // 从1开始
    @Override
    public int select1(int k) {
        if (k <= 0 || k > ranks1[bits.length]) {
            return -1;
        }
        // 使用预计算的select加速
        if (k % GAP == 0) {
            int idx = k / GAP - 1;
            if (idx < selects1.length)
                return selects1[idx];
        }
        // 二分查找块
        int low = 0, high = ranks1.length - 1;
        while (low < high) {
            int mid = low + high >>> 1;
            if (ranks1[mid] < k) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        int block = low - 1;
        // 在块内查找
        int remaining = k - ranks1[block];
        long word = bits[block];
        for (int i = 0; i < 64; i++) {
            if ((word & (1L << i)) != 0) {
                if (--remaining == 0) {
                    return block * 64 + i;
                }
            }
        }
        return -1;
    }

    // 返回位图在 [0, pos] 中 0 的个数
    @Override
    public int rank0(int pos) {
        if (pos >= size) {
            pos = size - 1;
        }
        return pos + 1 - rank1(pos);
    }

    // 返回位图第 k 个 0 所在的位置，等价于求：rank0(?) = k
    @Override
    public int select0(int k) {
        if (k <= 0 || k > ranks1[bits.length]) {
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
}