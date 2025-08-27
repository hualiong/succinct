package org.example.succinct.core;

import org.example.succinct.api.SuccinctTrie;

import java.nio.CharBuffer;
import java.util.*;

public class SimpleSuccinctTrie implements SuccinctTrie {
    private final char[] labels;         // 存储 Trie 树的字符标签
    private final BitVector labelBitmap; // 存储 LOUDS 编码的位向量
    private final BitVector isLeaf;      // 存储所有叶子节点标记的位向量

    public static SuccinctTrie of(String... keys) {
        return new SuccinctTrie(keys);
    }

    private SuccinctTrie(String[] keys) {
        for (int i = 1; i < keys.length; i++) {
            assert keys[i].compareTo(keys[i - 1]) >= 0 : "The inputs are not ordered!";
        }
        List<Character> labelsList = new ArrayList<>();
        BitVector.Builder labelBitmapBuilder = new BitVector.Builder();
        BitVector.Builder isLeafBuilder = new BitVector.Builder();

        Queue<Range> queue = new ArrayDeque<>();
        queue.add(new Range(0, keys.length, 0));
        int bitPos = 0, nodeId = 0;
        while (!queue.isEmpty()) {
            Range range = queue.poll();
            int L = range.L, R = range.R, index = range.index;
            isLeafBuilder.set(nodeId, keys[L].length() == index);
            // 处理子节点
            int start = L;
            while (start < R) {
                // 跳过长度不足的键
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
                // 添加子节点标签
                labelsList.add(currentChar);
                // 设置子节点标记(0)
                // labelBitmapBuilder.set(bitPos, false);
                bitPos++;
                // 将子节点范围加入队列
                queue.add(new Range(start, end, index + 1));
                start = end;
            }
            // 设置节点结束标记(1)
            labelBitmapBuilder.set(bitPos++, true);
            nodeId++;
        }
        // 转换并初始化位图
        this.labels = new char[labelsList.size()];
        for (int i = 0; i < labelsList.size(); i++) {
            labels[i] = labelsList.get(i);
        }
        this.labelBitmap = labelBitmapBuilder.build(true);
        this.isLeaf = isLeafBuilder.build(false);
    }

    /**
     * 存储的 key 的个数
     */
    public int size() {
        return isLeaf.oneCount;
    }

    /**
     * 该 Trie 树的节点个数
     */
    public int nodeCount() {
        return isLeaf.size;
    }

    /**
     * 判断 key 是否存在
     *
     * @param key 要查询的键值
     * @return 是否存在
     */
    public boolean contains(String key) {
        return index(key) >= 0;
    }

    /**
     * 精确查询给定 key 在内部唯一对应的节点 ID
     *
     * @param key 要查询的 key
     * @return 如果 key 存在，则返回对应的节点 ID；否则，返回 -1
     */
    public int index(String key) {
        int nodeId = extract(key);
        return nodeId >= 0 && isLeaf.get(nodeId) ? nodeId : -1;
    }

    /**
     * 反向查询给定节点 ID 在内部唯一对应的 key
     *
     * @param nodeId 要查询的节点 ID
     * @return 如果节点 ID 在合法范围内，则返回对应的 key；否则，返回 null
     */
    public String get(int nodeId) {
        if (isLeaf.get(nodeId)) {
            StringBuilder str = new StringBuilder();
            int bitmapIndex;
            while ((bitmapIndex = labelBitmap.select0(nodeId)) >= 0) {
                nodeId = labelBitmap.rank1(bitmapIndex);
                str.append(labels[bitmapIndex - nodeId]);
            }
            return str.reverse().toString();
        }
        return null;
    }

    /**
     * <p>以字典序或层序的方式遍历 Trie 中所有的 key</p>
     * <b>注意</b>：层序遍历的性能要优于字典序遍历，如果不追求有序，请将 {@code orderly} 设为 false 以获得最佳性能
     *
     * @param orderly 如果为 true，则按（DFS）字典序遍历；如果为 false，则按层序遍历。
     * @return 一个用于遍历所有 key 的迭代器
     */
    public Iterator<String> iterator(boolean orderly) {
        if (orderly) {
            return traverse(0, "");
        } else {
            return new Iterator<>() {
                private int index = isLeaf.nextSetBit(0);

                @Override
                public boolean hasNext() {
                    return index >= 0;
                }

                @Override
                public String next() {
                    String str = get(index);
                    index = isLeaf.nextSetBit(index + 1);
                    return str;
                }
            };
        }
    }

    /**
     * 查询给定字符串在 Trie 内所有的前缀
     *
     * @param str 要查询的字符串
     * @return 一个用于遍历所有前缀的迭代器
     */
    public Iterator<String> prefixKeysOf(String str) {
        return new TermIterator() {
            private final char[] chars = str.toCharArray();
            private int pos = 0;
            private int nodeId = 0;
            private int bitmapIndex = 0;

            {
                advance(); // 初始化查找第一个前缀
            }

            @Override
            public void advance() {
                while (pos < chars.length) {
                    int index = labelSearch(nodeId, bitmapIndex, chars[pos]);
                    if (index < 0) {
                        break;
                    }
                    nodeId = index + 1 - nodeId;
                    bitmapIndex = labelBitmap.select1(nodeId) + 1;
                    pos++;
                    if (isLeaf.get(nodeId)) {
                        next = new String(chars, 0, pos);
                        return;
                    }
                }
                next = null;
            }
        };
    }

    /**
     * 查询所有以给定前缀开头的 key
     *
     * @param prefix 要搜索的前缀
     * @return 一个用于遍历所有匹配前缀的 key 的迭代器
     */
    public Iterator<String> prefixSearch(String prefix) {
        return traverse(extract(prefix), prefix);
    }

    private int extract(String key) {
        int nodeId = 0, bitmapIndex = 0;
        for (char c : key.toCharArray()) {
            if ((bitmapIndex = labelSearch(nodeId, bitmapIndex, c)) < 0) {
                return -1;
            }
            // 向子节点转移
            nodeId = bitmapIndex + 1 - nodeId;
            bitmapIndex = labelBitmap.select1(nodeId) + 1;
        }
        return nodeId;
    }

    private Iterator<String> traverse(int rootId, String prefix) {
        return new TermIterator() {
            private final CharBuffer charBuffer = CharBuffer.allocate(256);
            private int nodeId = rootId;
            private int bitmapIndex = rootId < 0 ? labelBitmap.size : labelBitmap.select1(rootId) + 1;

            {
                charBuffer.append(prefix);
                charBuffer.flip();
                if (!isLeaf.get(rootId)) {
                    advance();
                }
            }

            @Override
            public void advance() {
                // 切换写模式
                charBuffer.position(charBuffer.limit());
                charBuffer.limit(charBuffer.capacity());
                while (true) {
                    // 撞墙
                    while (bitmapIndex >= labelBitmap.size || labelBitmap.get(bitmapIndex)) {
                        // 到达根节点，遍历结束
                        if (nodeId == rootId) {
                            next = null;
                            return;
                        }
                        // 回溯并向右转移
                        bitmapIndex = labelBitmap.select0(nodeId) + 1;
                        nodeId = bitmapIndex - nodeId;
                        charBuffer.position(charBuffer.position() - 1);
                    }
                    charBuffer.put(labels[bitmapIndex - nodeId]);
                    // 向下转移
                    nodeId = bitmapIndex + 1 - nodeId;
                    bitmapIndex = labelBitmap.select1(nodeId) + 1;
                    if (isLeaf.get(nodeId)) {
                        charBuffer.flip();
                        next = charBuffer.toString();
                        return;
                    }
                }
            }
        };
    }

    /**
     * 搜索标签向下层转移
     *
     * @param nodeId      当前节点ID
     * @param bitmapIndex 当前节点在 {@code labelBitmap} 中的起始下标
     * @param b           要搜索的标签
     * @return 目标标签在 {@code labelBitmap} 中的下标，否则返回 -1
     */
    private int labelSearch(int nodeId, int bitmapIndex, char b) {
        while (true) {
            if (bitmapIndex >= labelBitmap.size || labelBitmap.get(bitmapIndex)) {
                return -1;
            }
            int labelIndex = bitmapIndex - nodeId;
            if (labelIndex < labels.length && labels[labelIndex] == b) {
                break;
            }
            bitmapIndex++;
        }
        return bitmapIndex;
    }

    // 辅助类：表示键范围
    private record Range(int L, int R, int index) {
    }

    // 词项迭代器
    private abstract static class TermIterator implements Iterator<String> {
        String next = "";

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public String next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            String term = next;
            advance();
            return term;
        }

        abstract void advance();
    }

    // 自实现位向量（位图）
    public static class BitVector {
        /**
         * 数值越小，selects 预计算的间距越小，占用更高，select1 的性能越好
         * 经测试，设为 1 或 2 时，性能提升明显，但占用极高，其余数值影响不大
         */
        private static final int GAP = 64;

        private final long[] bits;
        private final int[] ranks;   // 预计算rank1
        private final int[] selects; // 部分预计算select1
        public final int oneCount;
        public final int size;

        // 构建器模式
        public static class Builder {
            private final List<Long> bits = new ArrayList<>();
            private int size = 0;
            private int count = 0;

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

            public BitVector build(boolean rankSelect) {
                long[] array = new long[bits.size()];
                for (int i = 0; i < bits.size(); i++) {
                    array[i] = bits.get(i);
                }
                return new BitVector(array, size, count, rankSelect);
            }
        }

        private BitVector(long[] bits, int size, int count, boolean rankSelect) {
            this.bits = bits;
            this.size = size;
            this.oneCount = count;

            // 预计算rank和select
            if (rankSelect) {
                int totalOnes = 0;
                int oneCount = 0;
                this.ranks = new int[bits.length + 1];
                List<Integer> selectList = new ArrayList<>();
                for (int i = 0; i < bits.length; i++) {
                    ranks[i] = totalOnes;
                    int blockOnes = Long.bitCount(bits[i]);
                    totalOnes += blockOnes;

                    long block = bits[i];
                    for (int j = 0; j < 64; j++) {
                        if ((block & (1L << j)) != 0) {
                            oneCount++;
                            if (oneCount % GAP == 0) {
                                selectList.add(i * 64 + j);
                            }
                        }
                    }
                }
                ranks[bits.length] = totalOnes;

                this.selects = new int[selectList.size()];
                for (int i = 0; i < selectList.size(); i++) {
                    selects[i] = selectList.get(i);
                }
            } else {
                this.ranks = null;
                this.selects = null;
            }
        }

        public int nextSetBit(int from) {
            if (from < 0 || from >= size) {
                return -1;
            }
            int u = from >> 6;
            long word;
            for (word = this.bits[u] & -1L << from; word == 0L; word = this.bits[u]) {
                if (++u == bits.length) {
                    return -1;
                }
            }
            return (u << 6) + Long.numberOfTrailingZeros(word);
        }

        public boolean get(int pos) {
            if (pos >= size) return false;
            int block = pos >> 6;
            int offset = pos & 0x3F;
            return (bits[block] & (1L << offset)) != 0;
        }

        public int rank1(int pos) {
            if (pos < 0 || pos >= size) {
                return 0;
            }
            int block = pos + 1 >> 6;
            int offset = pos + 1 & 0x3F;
            int count = ranks[block];

            if (offset > 0) {
                long mask = (1L << offset) - 1;
                count += Long.bitCount(bits[block] & mask);
            }
            return count;
        }

        // 性能较差
        public int select1(int k) {
            if (k <= 0 || k > ranks[bits.length]) {
                return -1;
            }
            // 使用预计算的select加速
            if (k % GAP == 0) {
                int idx = k / GAP - 1;
                if (idx < selects.length)
                    return selects[idx];
            }
            // 二分查找块
            int low = 0, high = ranks.length - 1;
            while (low < high) {
                int mid = low + high >>> 1;
                if (ranks[mid] < k) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            int block = low - 1;
            // 在块内查找
            int remaining = k - ranks[block];
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

        public int rank0(int pos) {
            if (pos >= size) {
                pos = size - 1;
            }
            return pos + 1 - rank1(pos);
        }

        // 性能极差
        public int select0(int k) {
            if (k <= 0 || k > ranks[bits.length]) {
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
}
