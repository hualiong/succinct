package org.example.succinct.core;

import it.unimi.dsi.fastutil.chars.CharArrayList;
import org.example.succinct.api.RankSelectBitSet;
import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.common.Range;
import org.example.succinct.common.RankSelectBitSet4;

import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;

public class CharSuccinctTrie2 implements SuccinctTrie {
    private final char[] labels;
    private final RankSelectBitSet labelBitmap;
    private final RankSelectBitSet isLeaf;
    private final RankSelectBitSet isCompress;
    private final CharBuffer buffer;

    public static CharSuccinctTrie2 of(String... keys) {
        Arrays.sort(keys);
        return CharSuccinctTrie2.sortedOf(keys);
    }

    public static CharSuccinctTrie2 sortedOf(String[] keys) {
        CharArrayList charLabels = new CharArrayList();
        RankSelectBitSet.Builder labelBitmapBuilder = new RankSelectBitSet4.Builder();
        RankSelectBitSet.Builder isLeafBuilder = new RankSelectBitSet4.Builder();
        RankSelectBitSet.Builder isCompressBuilder = new RankSelectBitSet4.Builder();

        Queue<Range> queue = new ArrayDeque<>(keys.length);
        queue.add(new Range(0, keys.length, 0));
        int maxLen = 1;
        for (int bitPos = 0, nodeId = 0; !queue.isEmpty(); nodeId++) {
            Range range = queue.poll();
            int L = range.L(), R = range.R(), index = range.index();
            // 检查当前节点是否是叶子节点并跳过重复字符串（最短的一定是第一个）
            if (keys[L].length() == index) {
                maxLen = Math.max(maxLen, index);
                isLeafBuilder.set(nodeId, true);
                while (++L < R && keys[L].length() == index);
            }
            if (L < R) {
                int i = L;
                char c = keys[i].charAt(index);
                while (++i < R && keys[i].charAt(index) == c);
                if (i == R) {
                    i = L;
                    charLabels.add(c);
                    int pos = ++bitPos;
                    while (true) {
                        int before = i;
                        if (keys[i].length() == ++index) {
                            while (++i < R && keys[i].length() == index);
                            // 处理末端叶子节点
                            if (i >= R) {
                                queue.add(new Range(before, R, index));
                                break;
                            }
                        }
                        int start = i;
                        c = keys[i].charAt(index);
                        while (++i < R && keys[i].charAt(index) == c);
                        // 探测到分叉，结束压缩
                        if (i < R) {
                            queue.add(new Range(before, R, index));
                            break;
                        }
                        queue.add(new Range(before, before, index));
                        charLabels.add(c);
                        bitPos++;
                        i = start;
                    }
                    isCompressBuilder.set(nodeId, bitPos - pos > 0);
                } else {
                    // 处理多个子节点
                    int start = L;
                    while (start < R) {
                        int end = start;
                        c = keys[start].charAt(index);
                        while (++end < R && keys[end].charAt(index) == c);
                        bitPos++; // 设置子节点标记(0)
                        charLabels.add(c); // 添加子节点标签
                        queue.add(new Range(start, end, index + 1)); // 将子节点范围加入队列
                        start = end;
                    }
                }
            }
            labelBitmapBuilder.set(bitPos++, true); // 设置节点结束标记(1)
        }
        // 转换并初始化位图
        return new CharSuccinctTrie2(
                charLabels.toCharArray(),
                labelBitmapBuilder.build(true),
                isLeafBuilder.build(false),
                isCompressBuilder.build(false), maxLen);
    }

    private CharSuccinctTrie2(char[] labels, RankSelectBitSet labelBitmap, RankSelectBitSet isLeaf, RankSelectBitSet isCompress, int maxLen) {
        this.labels = labels;
        this.labelBitmap = labelBitmap;
        this.isLeaf = isLeaf;
        this.isCompress = isCompress;
        this.buffer = CharBuffer.allocate(maxLen);
    }

    @Override
    public int size() {
        return isLeaf.oneCount();
    }

    @Override
    public int nodeCount() {
        return isLeaf.size();
    }

    @Override
    public boolean contains(String key) {
        return index(key) >= 0;
    }

    @Override
    public int index(String key) {
        int nodeId = extract(key);
        return nodeId >= 0 && isLeaf.get(nodeId) ? nodeId : -1;
    }

    @Override
    public String get(int nodeId) {
        if (isLeaf.get(nodeId)) {
            int bitmapIndex, cap = buffer.capacity(), length = 0;
            while ((bitmapIndex = labelBitmap.select0(nodeId)) >= 0) {
                nodeId = bitmapIndex + 1 - nodeId;
                if (isCompress.get(nodeId)) {
                    do {
                        buffer.put(cap - ++length, labels[bitmapIndex - nodeId]);
                    } while (!labelBitmap.get(--bitmapIndex));
                } else {
                    buffer.put(cap - ++length, labels[bitmapIndex - nodeId]);
                }
            }
            String s = new String(buffer.array(), cap - length, length);
            buffer.clear();
            return s;
        }
        return null;
    }

    @Override
    public Iterator<String> prefixKeysOf(String str) {
        return new TermIterator() {
            private final char[] chars = str.toCharArray();
            private int pos = 0;
            private int[] state = new int[2]; // nodeId + bitmapIndex

            {
                if (!isLeaf.get(0)) {
                    advance(); // 初始化查找第一个前缀
                }
            }

            protected void advance() {
                int index;
                while (pos < chars.length && (index = moveDown(state, chars, pos)) >= 0) {
                    pos = index;
                    if (isLeaf.get(state[0])) {
                        next = new String(chars, 0, pos);
                        return;
                    }
                }
                next = null;
            }
        };
    }

    @Override
    public Iterator<String> iterator(boolean orderly) {
        if (orderly) {
            return dfs(0, "");
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

    @Override
    public Iterator<String> prefixSearch(String prefix) {
        return dfs(extract(prefix), prefix);
    }

    private Iterator<String> dfs(int rootId, String prefix) {
        return new TermIterator() {
            private final CharBuffer charBuffer = CharBuffer.allocate(256);
            private int nodeId = rootId;
            private int bitmapIndex = rootId < 0 ? labelBitmap.size() : labelBitmap.select1(rootId) + 1;

            {
                charBuffer.append(prefix);
                charBuffer.flip();
                if (!isLeaf.get(rootId)) {
                    advance();
                }
            }

            @Override
            protected void advance() {
                // 切换写模式
                charBuffer.position(charBuffer.limit());
                charBuffer.limit(charBuffer.capacity());
                while (true) {
                    // 撞墙
                    while (bitmapIndex >= labelBitmap.size() || labelBitmap.get(bitmapIndex)) {
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
                    // 向下转移
                    nodeId = bitmapIndex + 1 - nodeId;
                    bitmapIndex = labelBitmap.select1(nodeId) + 1;
                    
                    charBuffer.put(labels[nodeId - 1]);
                    if (isLeaf.get(nodeId)) {
                        charBuffer.flip();
                        next = charBuffer.toString();
                        return;
                    }
                }
            }
        };
    }

    private int extract(String key) {
        buffer.append(key);
        buffer.flip();
        int pos = 0;
        int[] state = new int[2];
        while (buffer.hasRemaining() && (pos = moveDown(state, buffer)) >= 0) {
            buffer.position(pos);
        }
        buffer.clear();
        return pos >= 0 ? state[0] : -1;
    }

    private int moveDown(int[] state, char[] chars, int i) {
        return moveDown(state, chars, chars.length, i);
    }

    private int moveDown(int[] state, CharBuffer buffer) {
        return moveDown(state, buffer.array(), buffer.limit(), buffer.position());
    }

    private int moveDown(int[] state, char[] chars, int length, int i) {
        int index = state[1];
        if (isCompress.get(state[0])) {
            while (!labelBitmap.get(index) && i < length) {
                if (labels[index++ - state[0]] != chars[i++]) {
                    return -1;
                }
            }
            index--;
        } else {
            index = labelSearch(state[0], state[1], chars[i], i++ < 3);
        }
        if (index < 0) {
            return -1;
        }
        state[0] = index + 1 - state[0];
        state[1] = labelBitmap.select1(state[0]) + 1;
        return i; 
    }

    /**
     * 搜索标签向下层转移
     *
     * @param nodeId      当前节点ID
     * @param bitmapIndex 当前节点在 {@code labelBitmap} 中的起始下标
     * @param c           要搜索的标签
     * @param bSearch     是否使用二分查找
     * @return 目标标签在 {@code labelBitmap} 中的下标，否则返回 -1
     */
    private int labelSearch(int nodeId, int bitmapIndex, char c, boolean bSearch) {
        if (bSearch && !labelBitmap.get(bitmapIndex + 1)) {
            int high = labelBitmap.select1(nodeId + 1) - 1;
            if (labelBitmap.get(high)) {
                return -1;
            }
            int index = Arrays.binarySearch(labels, bitmapIndex - nodeId, high - nodeId + 1, c);
            return index < 0 ? -1 : index + nodeId;
        } else {
            while (!labelBitmap.get(bitmapIndex)) {
                int labelIndex = bitmapIndex - nodeId;
                if (labels[labelIndex] == c) {
                    return bitmapIndex;
                }
                bitmapIndex++;
            }
            return -1;
        }
    }

    @Override
    public String toString() {
        return "CharSuccinctTrie[" + labels.length + " labels, " + labelBitmap.size() + " bits]";
    }

}
