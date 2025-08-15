package org.example.succinct.api;

import java.util.List;

public interface RankSelectBitSet {

    int size();

    long oneCount();

    boolean get(int pos);

    int nextSetBit(int from);

    int rank1(int pos);

    int select1(int k);

    int rank0(int pos);

    int select0(int k);

     abstract class Builder {
        protected final List<Long> bits;
        protected int size = 0;

        protected Builder(List<Long> bits) {
            this.bits = bits;
        }

        public void set(int position, boolean value) {
            ensureCapacity(position);
            int block = position >> 6;
            int offset = position & 0x3F;
            long mask = 1L << offset;
            if (value) {
                bits.set(block, bits.get(block) | mask);
            } else {
                bits.set(block, bits.get(block) & ~mask);
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
