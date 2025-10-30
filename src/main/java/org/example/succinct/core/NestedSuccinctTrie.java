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
    private final CharBuffer buffer;
    private final int level;

    private record Range(int L, int R, int index, boolean nested) {
    }

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
        int maxLen = 1;
        for (int bitPos = 0, nodeId = 0; !queue.isEmpty(); nodeId++) {
            Range range = queue.poll();
            int L = range.L(), R = range.R(), index = range.index();
            if (range.nested() && level > 1) {
                isLinkBuilder.set(nodeId, true);
                charLabels.add(keys[L].charAt(index));
                int offset = 1;
                for (int next = index + 1; keys[L].length() > next; offset++, next++) {
                    int i = L;
                    char nextChar = keys[L].charAt(next);
                    while (++i < R && keys[i].charAt(next) == nextChar);
                    if (i != R) {
                        break;
                    }
                }
                bitPos++;
                compress.add(new StringBuilder(keys[L].substring(index - 1, index + offset)).reverse().toString());
                queue.add(new Range(L, R, index + offset, false));
            } else {
                // 检查当前节点是否是叶子节点并跳过重复字符串（最短的一定是第一个）
                if (keys[L].length() == index) {
                    maxLen = Math.max(maxLen, index);
                    isLeafBuilder.set(nodeId, true);
                    while (++L < R && keys[L].length() == index);
                }
                // 处理子节点
                int start = L;
                while (start < R) {
                    boolean nested = false;
                    int end = start + 1;
                    char currentChar = keys[start].charAt(index);
                    while (end < R && keys[end].charAt(index) == currentChar)
                        end++;
                    if (level > 1 && start == L && end == R && keys[start].length() > index + 1) {
                        int next = index + 1, i = start;
                        char nextChar = keys[start].charAt(next);
                        while (++i < end && keys[i].charAt(next) == nextChar);
                        nested = i == end; // 仅有一个子节点
                    }
                    bitPos++; // 设置子节点标记(0)
                    charLabels.add(currentChar); // 添加子节点标签
                    queue.add(new Range(start, end, index + 1, nested)); // 将子节点范围加入队列
                    start = end;
                }
            }
            labelBitmapBuilder.set(bitPos++, true); // 设置节点结束标记(1)
        }
        RankSelectBitSet isLink = null;
        NestedSuccinctTrie nestedTrie = null;
        char[] labels = charLabels.toCharArray();
        RankSelectBitSet labelBitmap = labelBitmapBuilder.build(true);
        if (level > 1 && !compress.isEmpty()) {
            isLink = isLinkBuilder.build(false);
            nestedTrie = NestedSuccinctTrie.of(compress.toArray(new String[0]), level - 1);
            int nodeId = -1;
            Iterator<String> iter = compress.iterator();
            while ((nodeId = isLink.nextSetBit(nodeId + 1)) >= 0) {
                int bitmapIndex = labelBitmap.select1(nodeId) + 1;
                String next = iter.next();
                int nestedTrieNodeId = nestedTrie.index(next);
                if (nestedTrieNodeId == -1) {
                    System.out.println(next);
                }
                labels[nodeId - 1] = (char) (nestedTrieNodeId >>> 16);
                labels[bitmapIndex - nodeId] = (char) (nestedTrieNodeId & 0xFFFF);
            }
        }
        // 转换并初始化位图
        return new NestedSuccinctTrie(
                labels,
                labelBitmap,
                isLeafBuilder.build(false),
                isLink, nestedTrie, maxLen);
    }

    private NestedSuccinctTrie(char[] labels, RankSelectBitSet labelBitmap, RankSelectBitSet isLeaf,
            RankSelectBitSet isLink, NestedSuccinctTrie trie, int maxLen) {
        this.labels = labels;
        this.labelBitmap = labelBitmap;
        this.isLeaf = isLeaf;
        this.isLink = isLink;
        this.nestedTrie = trie;
        this.buffer = CharBuffer.allocate(maxLen);
        this.level = trie == null ? 1 : trie.level + 1;
    }

    public int level() {
        return level;
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
                if (isLink != null && isLink.get(nodeId)) {
                    String str = nestedTrie.get(getLinkId(nodeId));
                    length--;
                    for (int i = 0; i < str.length(); i++) {
                        buffer.put(cap - ++length, str.charAt(i));
                    }
                } else {
                    buffer.put(cap - ++length, labels[nodeId - 1]);
                }
                nodeId = bitmapIndex + 1 - nodeId;
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
                while (pos < chars.length && (state = moveDown(state[0], state[1], chars, pos)).length > 1) {
                    pos = state[2];
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
            private final CharBuffer charBuffer = CharBuffer.allocate(buffer.capacity());
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
        if (key.length() > buffer.capacity()) {
            return -1;
        }
        buffer.append(key);
        buffer.flip();
        int[] state = new int[2];
        while (buffer.hasRemaining() && (state = moveDown(state[0], state[1], buffer.array(), buffer.limit(),
                buffer.position())).length > 1) {
            buffer.position(state[2]);
        }
        buffer.clear();
        return state[0];
    }

    // private int[] moveUp(int nodeId) {
    // int bitmapIndex = labelBitmap.select0(nodeId) + 1;
    // nodeId = bitmapIndex - nodeId;
    // return new int[] { nodeId, bitmapIndex };
    // }

    private int[] moveDown(int nodeId, int bitmapIndex, char[] chars, int i) {
        return moveDown(nodeId, bitmapIndex, chars, chars.length, i);
    }

    private int[] moveDown(int nodeId, int bitmapIndex, char[] chars, int length, int i) {
        int linkId = -1;
        // 单个子节点
        if (labelBitmap.get(bitmapIndex + 1)) {
            int labelIndex = bitmapIndex - nodeId;
            if (isLink != null && isLink.get(labelIndex + 1)) {
                linkId = getLinkId(labelIndex + 1);
                assert linkId >= 0 : "linkId is -1";
            } else if (labels[labelIndex] != chars[i]) {
                bitmapIndex = -1;
            }
        } else {
            bitmapIndex = labelSearch(nodeId, bitmapIndex, chars[i], i < 3);
        }
        if (bitmapIndex < 0) {
            return new int[] { -1 };
        }
        nodeId = bitmapIndex + 1 - nodeId;
        if (linkId >= 0) {
            String str = nestedTrie.get(linkId);
            for (int j = str.length() - 1; j >= 0; j--) {
                if (i >= length || str.charAt(j) != chars[i++]) {
                    return new int[] { -1 };
                }
            }
            bitmapIndex = labelBitmap.select1(nodeId) + 1;
            nodeId = bitmapIndex + 1 - nodeId;
        } else {
            i++;
        }
        bitmapIndex = labelBitmap.select1(nodeId) + 1;
        return new int[] { nodeId, bitmapIndex, i };
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
        if (bSearch) {
            int high = labelBitmap.select1(nodeId + 1) - 1;
            if (high >= labelBitmap.size() || labelBitmap.get(high)) {
                return -1;
            }
            int index = Arrays.binarySearch(labels, bitmapIndex - nodeId, high - nodeId + 1, c);
            return index < 0 ? -1 : index + nodeId;
        } else {
            while (bitmapIndex < labelBitmap.size() && !labelBitmap.get(bitmapIndex)) {
                if (labels[bitmapIndex - nodeId] == c) {
                    return bitmapIndex;
                }
                bitmapIndex++;
            }
            return -1;
        }
    }

    private int getLinkId(int nodeId) {
        int highBits = ((int) labels[nodeId - 1]) << 16;
        int lowBits = ((int) labels[labelBitmap.select1(nodeId) + 1 - nodeId]) & 0xffff;
        return highBits | lowBits;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("NestedSuccinctSet[" + nodeCount());
        NestedSuccinctTrie trie = nestedTrie;
        while (trie != null) {
            str.append(" -> " + trie.nodeCount());
            trie = trie.nestedTrie;
        }
        str.append("]");
        return str.toString();
    }

}
