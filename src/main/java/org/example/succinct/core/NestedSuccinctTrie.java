package org.example.succinct.core;

import it.unimi.dsi.fastutil.chars.CharArrayList;
import org.example.succinct.api.RankSelectBitSet;
import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.common.RankSelectBitSet4;

import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class NestedSuccinctTrie implements SuccinctTrie {
    private final char[] labels;
    private final RankSelectBitSet labelBitmap;
    private final RankSelectBitSet isLeaf;
    private final RankSelectBitSet isLink;
    private final NestedSuccinctTrie nestedTrie;
    private CharBuffer buffer = CharBuffer.allocate(128);

    private record Range(int L, int R, int index, boolean nested) {}

    public static NestedSuccinctTrie of(String... keys) {
        Arrays.sort(keys);
        return NestedSuccinctTrie.sortedOf(keys);
    }
    
    public static NestedSuccinctTrie of(String[] keys, int level) {
        Arrays.sort(keys);
        return NestedSuccinctTrie.sortedOf(keys, level);
    }
    
    public static NestedSuccinctTrie sortedOf(String... keys) {
        return NestedSuccinctTrie.sortedOf(keys, 4);
    }

    public static NestedSuccinctTrie sortedOf(String[] keys, int level) {
        CharArrayList charLabels = new CharArrayList();
        RankSelectBitSet.Builder labelBitmapBuilder = new RankSelectBitSet4.Builder();
        RankSelectBitSet.Builder isLeafBuilder = new RankSelectBitSet4.Builder();
        RankSelectBitSet.Builder isLinkBuilder = level > 1 ? new RankSelectBitSet4.Builder() : null;

        Queue<Range> queue = new ArrayDeque<>(keys.length);
        List<String> compress = new ArrayList<>();
        queue.add(new Range(0, keys.length, 0, false));
       for (int bitPos = 0, nodeId = 0; !queue.isEmpty(); nodeId++) {
            Range range = queue.poll();
            int L = range.L(), R = range.R(), index = range.index();
            if (range.nested() && level > 1) {
                isLinkBuilder.set(nodeId, true);
                bitPos++;
                charLabels.add(keys[L].charAt(index));
                int offset = 1;
                for (int next = index + 1; keys[L].length() > next; offset++, next++) {
                    int i = L + 1;
                    char nextChar = keys[L].charAt(next);
                    while (i < R && keys[i].charAt(next) == nextChar) i++;
                    if (i != R) break;
                }
                compress.add(new StringBuilder(keys[L].substring(index - 1, index + offset)).reverse().toString());
                queue.add(new Range(L, R, index + offset, false));
            } else {
                // 检查当前节点是否是叶子节点并跳过重复字符串（最短的一定是第一个）
                if (keys[L].length() == index) {
                    isLeafBuilder.set(nodeId, true);
                    while (++L < R && keys[L].length() == index);
                }
                // 处理子节点
                int start = L;
                while (start < R) {
                    boolean marked = false;
                    int end = start + 1;
                    char currentChar = keys[start].charAt(index);
                    while (end < R && keys[end].charAt(index) == currentChar) end++;
                    // 判断无值
                    if (keys[start].length() > index + 1 && level > 1) {
                        int next = index + 1, i = start + 1;
                        char nextChar = keys[start].charAt(next);
                        while (i < end && keys[i].charAt(next) == nextChar) i++;
                        marked = i == end; // 仅有一个子节点
                    }
                    bitPos++; // 设置子节点标记(0)
                    charLabels.add(currentChar); // 添加子节点标签
                    queue.add(new Range(start, end, index + 1, marked)); // 将子节点范围加入队列
                    start = end;
                }
            }
            labelBitmapBuilder.set(bitPos++, true); // 设置节点结束标记(1)
        }
        RankSelectBitSet isLink = null;
        NestedSuccinctTrie nestedTrie = null;
        char[] labels = charLabels.toCharArray();
        RankSelectBitSet labelBitmap = labelBitmapBuilder.build(true);
        if (level > 1) {
            isLink = isLinkBuilder.build(false);
            nestedTrie = NestedSuccinctTrie.of(compress.toArray(new String[0]), level - 1);
            int nodeId = -1;
            Iterator<String> iter = compress.iterator();
            while ((nodeId = isLink.nextSetBit(nodeId + 1)) >= 0) {
                int bitmapIndex = labelBitmap.select1(nodeId) + 1;
                int nestedTrieNodeId = nestedTrie.index(iter.next());
                labels[nodeId - 1] = (char) (nestedTrieNodeId >>> 16);
                labels[bitmapIndex - nodeId] = (char) (nestedTrieNodeId & 0xFFFF);
            }
        }
        // 转换并初始化位图
        return new NestedSuccinctTrie(
                labels,
                labelBitmap,
                isLeafBuilder.build(false),
                isLink, nestedTrie);
    }

    private NestedSuccinctTrie(char[] labels, RankSelectBitSet labelBitmap, RankSelectBitSet isLeaf, RankSelectBitSet isLink, NestedSuccinctTrie trie) {
        this.labels = labels;
        this.labelBitmap = labelBitmap;
        this.isLeaf = isLeaf;
        this.isLink = isLink;
        this.nestedTrie = trie;
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
                if (cap < ++length) {
                    buffer.flip();
                    buffer = CharBuffer.allocate(cap << 1).put(buffer);
                }
                buffer.put(cap - length, labels[nodeId - 1]);
                nodeId = bitmapIndex + 1 - nodeId;
            }
            String s = new String(buffer.array(), cap - length, length);
            buffer.clear();
            return s;
        }
        return null;
    }
    
    private char[] getByChars(int nodeId) {
        int bitmapIndex, cap = buffer.capacity(), length = 0;
        while ((bitmapIndex = labelBitmap.select0(nodeId)) >= 0) {
            if (cap < ++length) {
                buffer.flip();
                buffer = CharBuffer.allocate(cap << 1).put(buffer);
            }
            buffer.put(labels[nodeId - 1]);
            nodeId = bitmapIndex + 1 - nodeId;
        }
        char[] arr = Arrays.copyOf(buffer.array(), length);
        buffer.clear();
        return arr;
    }
    
    @Override
    public Iterator<String> prefixKeysOf(String str) {
        return new TermIterator() {
            private final char[] chars = str.toCharArray();
            private int pos = 0;
            private int layer = 0;
            private int nodeId = 0;
            private int bitmapIndex = 0;

            {
                advance(); // 初始化查找第一个前缀
            }

            protected void advance() {
                out: while (pos < chars.length) {
                    int[] index = labelSearch(nodeId, bitmapIndex, chars[pos], layer < 3);
                    nodeId = index[0] + 1 - nodeId;
                    if (index[0] < 0) {
                        break;
                    }
                    if (index.length > 1) {
                        char[] chars = nestedTrie.getByChars(index[1]);
                        for (int i = 1; i < chars.length; i++) {
                            if (chars[i] != chars[++pos]) {
                                break out;
                            }
                        }
                        bitmapIndex = labelBitmap.select1(nodeId) + 1;
                        nodeId = bitmapIndex + 1 - nodeId;
                    }
                    layer++;
                    bitmapIndex = labelBitmap.select1(nodeId) + 1;
                    layer++;
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

    private int extract(String key) {
        buffer.append(key);
        buffer.flip();
        int[] state = new int[2];
        while (buffer.hasRemaining() && state.length > 1) {
            state = moveDown(state[0], state[1], buffer.get(), buffer.position());
        }
        buffer.clear();
        return state[0];
    }

    private int[] moveUp(int nodeId) {
        int bitmapIndex = labelBitmap.select0(nodeId) + 1;
        nodeId = bitmapIndex - nodeId;
        return new int[] { nodeId, bitmapIndex };
    }
    
    private int[] moveDown(int nodeId, int bitmapIndex, char c, int layer) {
        int[] index = labelSearch(nodeId, bitmapIndex, c, layer <= 3);
        if (index[0] < 0) {
            return new int[] { -1 };
        }
        nodeId = index[0] + 1 - nodeId;
        if (index[1] >= 0) {
            char[] chars = nestedTrie.getByChars(index[1]);
            for (int i = 1; i < chars.length; i++) {
                if (!buffer.hasRemaining() || chars[i] != buffer.get()) {
                    buffer.clear();
                    return new int[] { -1 };
                }
            }
            bitmapIndex = labelBitmap.select1(nodeId) + 1;
            nodeId = bitmapIndex + 1 - nodeId;
        }
        bitmapIndex = labelBitmap.select1(nodeId) + 1;
        return new int[] { nodeId, bitmapIndex };
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
    private int[] labelSearch(int nodeId, int bitmapIndex, char c, boolean bSearch) {
        if (bSearch) {
            int high = labelBitmap.select1(nodeId + 1) - 1;
            if (high >= labelBitmap.size() || labelBitmap.get(high)) {
                return new int[] { -1, -1 };
            }
            int low = bitmapIndex, mid = -1, linkId = -1;
            while (low <= high) {
                linkId = -1;
                mid = low + (high - low >>> 1);
                int labelIndex = mid - nodeId;
                char label = labels[labelIndex];
                if (isLink != null && isLink.get(labelIndex + 1)) {
                    linkId = getLinkId(labelIndex + 1);
                    label = nestedTrie.labels[linkId - 1];
                }
                if (label == c) {
                    break;
                } else if (label < c) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return new int[] { low > high ? -1 : mid, linkId };
        } else {
            while (bitmapIndex < labelBitmap.size() && !labelBitmap.get(bitmapIndex)) {
                int labelIndex = bitmapIndex - nodeId, linkId = -1;
                char label = labels[labelIndex];
                if (isLink != null && isLink.get(labelIndex + 1)) {
                    linkId = getLinkId(labelIndex + 1);
                    label = nestedTrie.labels[linkId - 1];
                }
                if (label == c) {
                    return new int[] { bitmapIndex, linkId };
                }
                bitmapIndex++;
            }
            return new int[] { -1, -1 };
        }
    }

    private int getLinkId(int nodeId) {
        int highBits = ((int) labels[nodeId - 1]) << 16;
        int lowBits = ((int) labels[labelBitmap.select1(nodeId) + 1 - nodeId]) & 0xffff;
        return highBits | lowBits;
    }

    @Override
    public String toString() {
        return "NestedSuccinctSet[" + labels.length + " labels, " + labelBitmap.size() + " bits]";
    }

}
