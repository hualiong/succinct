package org.example.succinct.api;

import java.util.List;

public interface RankSelectBitSet {

    int size();

    int oneCount();

    boolean get(int pos);

    int nextSetBit(int from);

    int rank1(int pos);

    int select1(int k);

    int rank0(int pos);

    int select0(int k);

    default boolean isInvalid(long n, long min, long max) {
        assert min <= n && n <= max : "Index (" + n + ") is not in valid range [" + min + ", " + max + "]";
        return n < min || n > max;
    }

    abstract class Builder {
        protected final List<Long> bits;
        protected int size = 0;
        protected int count = 0;

        protected Builder(List<Long> bits) {
            this.bits = bits;
        }

        public void set(int position, boolean value) {
            ensureCapacity(position);
            int block = position >> 6;
            int offset = position & 0x3F;
            long mask = 1L << offset;
            long oldBlock = bits.get(block);
            if (value) {
                bits.set(block, oldBlock | mask); // 设置位为1
            } else {
                bits.set(block, oldBlock & ~mask); // 设置位为0
            }
            // 仅当位的值实际发生变化时更新计数器
            if ((oldBlock & mask) == 0 == value) {
                count += value ? 1 : 0;
            }
        }

        private void ensureCapacity(int position) {
            int requiredBlocks = (position >> 6) + 1;
            while (bits.size() < requiredBlocks) {
                bits.add(0L);
            }
            size = Math.max(size, position + 1);
        }

        public abstract RankSelectBitSet build(boolean rankSelect);
    }
}
