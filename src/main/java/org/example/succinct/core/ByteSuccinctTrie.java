package org.example.succinct.core;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import org.example.succinct.api.RankSelectBitSet;
import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.common.Range;
import org.example.succinct.common.RankSelectBitSet4;
import org.example.succinct.utils.StringEncoder;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * 基于 byte 数组实现的第四代 Succinct Set，0标识子节点，1标识结束
 * Note: 从前缀树上看，当前标签位于弧上，归属上个节点，断言
 */
public class ByteSuccinctTrie implements SuccinctTrie {
    private final byte[] labels;
    private final RankSelectBitSet labelBitmap;
    private final RankSelectBitSet isLeaf;
    private final StringEncoder encoder;
    private byte[] buffer = new byte[512];

    public static ByteSuccinctTrie of(String... keys) {
        return ByteSuccinctTrie.of(keys, "GB18030", false);
    }

    public static ByteSuccinctTrie sortedOf(String... keys) {
        return ByteSuccinctTrie.of(keys, "GB18030", true);
    }

    public static ByteSuccinctTrie of(String[] keys, String charset, boolean sorted) {
        StringEncoder encoder = new StringEncoder(Charset.forName(charset));
        // 按字节数组字典序排序
        if (!sorted) {
            Arrays.parallelSort(keys);
        }
        byte[][] keyBytes = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            keyBytes[i] = encoder.encodeToBytes(keys[i]);
        }
        ByteArrayList labels = new ByteArrayList();
        // TODO RankSelectBitSet4 的实际表现要比 RankSelectBitSet3 慢，需要排查原因
        RankSelectBitSet.Builder labelBitmapBuilder = new RankSelectBitSet4.Builder();
        RankSelectBitSet.Builder isLeafBuilder = new RankSelectBitSet4.Builder();

        Queue<Range> queue = new ArrayDeque<>();
        queue.add(new Range(0, keys.length, 0));
        int bitPos = 0, nodeId = 0;

        while (!queue.isEmpty()) {
            Range range = queue.poll();
            int L = range.L(), R = range.R(), index = range.index();
            boolean isLeafNode = false;
            int ptr = L;
            while (ptr < R && keyBytes[ptr].length == index) {
                isLeafNode = true;
                ptr++;
            }
            isLeafBuilder.set(nodeId, isLeafNode);
            // 处理子节点
            int start = L;
            while (start < R) {
                // 跳过长度不足的键
                if (keyBytes[start].length <= index) {
                    start++;
                    continue;
                }
                byte currentByte = keyBytes[start][index];
                int end = start + 1;
                while (end < R) {
                    if (keyBytes[end].length <= index || keyBytes[end][index] != currentByte) {
                        break;
                    }
                    end++;
                }
                // 添加子节点标签(byte)
                labels.add(currentByte);
                labelBitmapBuilder.set(bitPos++, false); // 子节点标记
                // 将子节点范围加入队列(字节索引+1)
                queue.add(new Range(start, end, index + 1));
                start = end;
            }
            // 设置节点结束标记
            labelBitmapBuilder.set(bitPos++, true); // 结束标记
            nodeId++;
        }
        return new ByteSuccinctTrie(
                labels.toByteArray(),
                labelBitmapBuilder.build(true),
                isLeafBuilder.build(false),
                encoder);
    }

    public ByteSuccinctTrie(byte[] labels, RankSelectBitSet labelBitmap, RankSelectBitSet isLeaf,
                            StringEncoder encoder) {
        this.labels = labels;
        this.labelBitmap = labelBitmap;
        this.isLeaf = isLeaf;
        this.encoder = encoder;
    }

    public RankSelectBitSet labelBitmap() {
        return labelBitmap;
    }

    @Override
    public long size() {
        return isLeaf.oneCount();
    }

    @Override
    public int index(String key) {
        int nodeId = extract(key);
        return nodeId >= 0 && isLeaf.get(nodeId) ? nodeId : -1;
    }

    @Override
    public boolean contains(String key) {
        int nodeId = extract(key);
        return nodeId >= 0 && isLeaf.get(nodeId);
    }

    @Override
    public String get(int nodeId) {
        if (isLeaf.get(nodeId)) {
            int id = nodeId, length = 0, bitmapIndex;
            while ((bitmapIndex = labelBitmap.select0(id)) >= 0) {
                id = bitmapIndex + 1 - id;
                if (buffer.length < ++length) {
                    buffer = Arrays.copyOf(buffer, buffer.length >>> 1);
                }
                buffer[buffer.length - length] = labels[bitmapIndex - id];
            }
            return new String(buffer, buffer.length - length, length, encoder.charset());
        }
        return null;
    }

    @Override
    public Iterator<String> prefixKeysOf(String str) {
        return new Iterator<>() {
            private final byte[] bytes = encoder.encodeToBytes(str);
            private int pos = 0;
            private int layer = 0;
            private int nodeId = 0;
            private int bitmapIndex = 0;
            private String next;

            {
                advance(); // 初始化查找第一个前缀
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public String next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                String result = next;
                advance();
                return result;
            }

            private void advance() {
                while (pos < bytes.length) {
                    int index = labelSearch(nodeId, bitmapIndex, bytes[pos], layer < 3);
                    if (index < 0) {
                        break;
                    }
                    nodeId = index + 1 - nodeId;
                    bitmapIndex = labelBitmap.select1(nodeId) + 1;
                    layer++;
                    pos++;
                    if (isLeaf.get(nodeId)) {
                        next = new String(bytes, 0, pos, encoder.charset());
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
            return traverse(0);
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

    private int extract(String key) {
        ByteBuffer buffer = encoder.encodeToBuffer(key);
        int nodeId = 0, bitmapIndex = 0, layer = 0;
        buffer.rewind();
        while (buffer.hasRemaining()) {
            int index = labelSearch(nodeId, bitmapIndex, buffer.get(), layer < 3);
            if (index < 0) {
                return -1;
            }
            layer++;
            nodeId = index + 1 - nodeId;
            bitmapIndex = labelBitmap.select1(nodeId) + 1;
        }
        return nodeId;
    }

    private Iterator<String> traverse(int rootId) {
        return new Iterator<>() {
            private final ByteBuffer byteBuffer = ByteBuffer.allocate(512);
            // private int layer = 0;
            private int nodeId = 0;
            private int bitmapIndex = 0;
            private String next;

            {
                byteBuffer.flip();
                advance(); // 初始化查找第一个前缀
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public String next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                String result = next;
                advance();
                return result;
            }

            private void advance() {
                // 切换写模式
                byteBuffer.position(byteBuffer.limit());
                byteBuffer.limit(byteBuffer.capacity());
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
                        byteBuffer.position(byteBuffer.position() - 1);
                    }
                    byteBuffer.put(labels[bitmapIndex - nodeId]);
                    // 向下转移
                    nodeId = bitmapIndex + 1 - nodeId;
                    bitmapIndex = labelBitmap.select1(nodeId) + 1;
                    if (isLeaf.get(nodeId)) {
                        byteBuffer.flip();
                        next = encoder.charset().decode(byteBuffer).toString();
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
     * @param bSearch     是否使用二分查找
     * @return 目标标签在 {@code labelBitmap} 中的下标，否则返回 -1
     */
    private int labelSearch(int nodeId, int bitmapIndex, byte b, boolean bSearch) {
        if (bSearch) {
            int high = labelBitmap.select1(nodeId + 1) - 1;
            if (high >= labelBitmap.size() || labelBitmap.get(high)) {
                return -1;
            }
            // System.out.println("bSearch: " + (high - bitmapIndex + 1));
            int low = bitmapIndex, mid = -1;
            while (low <= high) {
                mid = low + high >>> 1;
                byte label = labels[mid - nodeId];
                if (label == b) {
                    break;
                } else if (label < b) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return low > high ? -1 : mid;
        } else {
            // int st = bitmapIndex;
            while (true) {
                if (bitmapIndex >= labelBitmap.size() || labelBitmap.get(bitmapIndex)) {
                    return -1;
                }
                int labelIndex = bitmapIndex - nodeId;
                if (labelIndex < labels.length && labels[labelIndex] == b) {
                    break;
                }
                bitmapIndex++;
            }
            // System.out.println("order: " + (bitmapIndex - st + 1));
            return bitmapIndex;
        }
    }

    @Override
    public String toString() {
        return String.format("ByteSuccinctSet4(%s)[%d labels, %d bits]", encoder.charset(), labels.length,
                labelBitmap.size());
    }
}