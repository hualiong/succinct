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
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;

public class ByteSuccinctTrie2 implements SuccinctTrie {
    private final byte[] labels;
    private final RankSelectBitSet labelBitmap;
    private final RankSelectBitSet isLeaf;
    private final RankSelectBitSet isCompress;
    private final StringEncoder encoder;
    private final ByteBuffer buffer;

    public static ByteSuccinctTrie2 of(String... keys) {
        return ByteSuccinctTrie2.of(keys, Charset.forName("GB18030"));
    }

    public static ByteSuccinctTrie2 of(String[] keys, Charset charset) {
        StringEncoder encoder = new StringEncoder(charset);
        byte[][] keyBytes = Arrays.stream(keys).map(encoder::getBytesSafely).toArray(byte[][]::new);
        // 按字节数组字典序排序
        Arrays.sort(keyBytes, (a, b) -> {
            int minLen = Math.min(a.length, b.length);
            for (int i = 0; i < minLen; i++) {
                int cmp = Byte.compare(a[i], b[i]);
                if (cmp != 0) {
                    return cmp;
                }
            }
            return a.length - b.length;
        });
        ByteArrayList byteLabels = new ByteArrayList();
        // RankSelectBitSet4 的实际表现要比 RankSelectBitSet3 慢，需要排查原因
        RankSelectBitSet.Builder labelBitmapBuilder = new RankSelectBitSet4.Builder();
        RankSelectBitSet.Builder isLeafBuilder = new RankSelectBitSet4.Builder();
        RankSelectBitSet.Builder isCompressBuilder = new RankSelectBitSet4.Builder();

        Queue<Range> queue = new ArrayDeque<>();
        queue.add(new Range(0, keys.length, 0));
        for (int bitPos = 0, nodeId = 0; !queue.isEmpty(); nodeId++) {
            Range range = queue.poll();
            int L = range.L(), R = range.R(), index = range.index();
            // 检查当前节点是否是叶子节点并跳过重复字符串（最短的一定是第一个）
            if (keyBytes[L].length == index) {
                isLeafBuilder.set(nodeId, true);
                while (++L < R && keyBytes[L].length == index);
            }
            if (L < R) {
                int i = L;
                byte b = keyBytes[i][index];
                while (++i < R && keyBytes[i][index] == b);
                if (i == R) {
                    i = L;
                    byteLabels.add(b);
                    int pos = ++bitPos;
                    while (true) {
                        int before = i;
                        if (keyBytes[i].length == ++index) {
                            while (++i < R && keyBytes[i].length == index);
                            // 处理末端叶子节点
                            if (i >= R) {
                                queue.add(new Range(before, R, index));
                                break;
                            }
                        }
                        int start = i;
                        b = keyBytes[i][index];
                        while (++i < R && keyBytes[i][index] == b);
                        // 探测到分叉，结束压缩
                        if (i < R) {
                            queue.add(new Range(before, R, index));
                            break;
                        }
                        queue.add(new Range(before, before, index));
                        byteLabels.add(b);
                        bitPos++;
                        i = start;
                    }
                    isCompressBuilder.set(nodeId, bitPos - pos > 0);
                } else {
                    // 处理多个子节点
                    int start = L;
                    while (start < R) {
                        int end = start;
                        b = keyBytes[start][index];
                        while (++end < R && keyBytes[end][index] == b);
                        bitPos++; // 设置子节点标记(0)
                        byteLabels.add(b); // 添加子节点标签
                        queue.add(new Range(start, end, index + 1)); // 将子节点范围加入队列
                        start = end;
                    }
                }
            }
            labelBitmapBuilder.set(bitPos++, true); // 设置节点结束标记(1)
        }
        return new ByteSuccinctTrie2(
                byteLabels.toByteArray(),
                labelBitmapBuilder.build(true),
                isLeafBuilder.build(false),
                isCompressBuilder.build(false),
                encoder);
    }

    private ByteSuccinctTrie2(byte[] labels, RankSelectBitSet labelBitmap, RankSelectBitSet isLeaf, RankSelectBitSet isCompress, StringEncoder encoder) {
        this.labels = labels;
        this.labelBitmap = labelBitmap;
        this.isLeaf = isLeaf;
        this.isCompress = isCompress;
        this.encoder = encoder;
        this.buffer = encoder.byteBuffer();
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

    private int extract(String key) {
        if (!encoder.checkByteCapacity(key)) {
            return -1;
        }
        ByteBuffer buffer = encoder.encodeToBuffer(key);
        int nodeId = 0, bitmapIndex = 0;
        while (buffer.hasRemaining() && bitmapIndex >= 0) {
            if (isCompress.get(nodeId)) {
                while (!labelBitmap.get(bitmapIndex) && buffer.hasRemaining()) {
                    if (labels[bitmapIndex++ - nodeId] != buffer.get()) {
                        bitmapIndex = 0;
                        break;
                    }
                }
                bitmapIndex--;
            } else {
                bitmapIndex = labelSearch(nodeId, bitmapIndex, buffer.get(), buffer.position() <= 3);
            }
            if (bitmapIndex >= 0) {
                nodeId = bitmapIndex + 1 - nodeId;
                bitmapIndex = labelBitmap.select1(nodeId) + 1;
            }
        }
        return bitmapIndex >= 0 ? nodeId : -1;
    }

    @Override
    public String get(int nodeId) {
        String s = null;
        if (isLeaf.get(nodeId)) {
            int length = 0, cap = buffer.clear().capacity(), bitmapIndex;
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
            s = new String(buffer.array(), cap - length, length, encoder.charset());
        }
        return s;
    }

    @Override
    public Iterator<String> prefixKeysOf(String str) {
        return str.length() > buffer.capacity() ? Collections.emptyIterator() : new TermIterator() {
            private final byte[] bytes = encoder.getBytes(str);
            private int pos = 0;
            private int nodeId = 0;
            private int bitmapIndex = 0;

            {
                if (!isLeaf.get(0)) {
                    advance(); // 初始化查找第一个前缀
                }
            }

            @Override
            protected void advance() {
                while (pos < bytes.length && bitmapIndex >= 0) {
                    if (isCompress.get(nodeId)) {
                        while (!labelBitmap.get(bitmapIndex) && pos < bytes.length) {
                            int labelIndex = bitmapIndex++ - nodeId;
                            if (labels[labelIndex] != bytes[pos]) {
                                bitmapIndex = 0;
                                break;
                            } else {
                                pos++;
                                if (isLeaf.get(labelIndex + 1)) {
                                    if (labelBitmap.get(bitmapIndex)) {
                                        break;
                                    }
                                    next = new String(bytes, 0, pos, encoder.charset());
                                    return;
                                }
                            }
                        }
                        bitmapIndex--;
                    } else {
                        bitmapIndex = labelSearch(nodeId, bitmapIndex, bytes[pos++], pos <= 3);
                    }
                    if (bitmapIndex >= 0) {
                        nodeId = bitmapIndex + 1 - nodeId;
                        bitmapIndex = labelBitmap.select1(nodeId) + 1;
                        if (isLeaf.get(nodeId)) {
                            next = new String(bytes, 0, pos, encoder.charset());
                            return;
                        }
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
        Charset charset = encoder.charset();
        return rootId < 0 ? Collections.emptyIterator() : new TermIterator() {
            private final ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.capacity());
            private int nodeId = rootId;
            private int bitmapIndex = labelBitmap.select1(rootId) + 1;

            {
                byteBuffer.put(encoder.getBytes(prefix)).flip();
                int parentIndex = labelBitmap.select0(nodeId) + 1, parentId = parentIndex - nodeId;
                if (isCompress.get(parentId) && !labelBitmap.get(parentIndex)) {
                    nodeId = parentId;
                    bitmapIndex = parentIndex;
                }
                if (isLeaf.get(rootId)) {
                    next = charset.decode(byteBuffer.flip()).toString();
                } else {
                    advance();
                }
            }

            @Override
            protected void advance() {
                // 切换写模式
                byteBuffer.position(byteBuffer.limit()).limit(byteBuffer.capacity());
                while (true) {
                    // 撞墙
                    while (labelBitmap.get(bitmapIndex) || bitmapIndex < 0) {
                        // 到达根节点，遍历结束
                        if (nodeId <= rootId) {
                            next = null;
                            return;
                        }
                        // 回溯并向右转移
                        bitmapIndex = labelBitmap.select0(nodeId) + 1;
                        nodeId = bitmapIndex - nodeId;
                        if (isCompress.get(nodeId)) {
                            int pos = byteBuffer.position();
                            while (--bitmapIndex >= 0 && !labelBitmap.get(bitmapIndex)) pos--;
                            byteBuffer.position(pos);
                        } else {
                            byteBuffer.position(byteBuffer.position() - 1);
                        }
                    }
                    // 向下转移
                    if (isCompress.get(nodeId)) {
                        while (!labelBitmap.get(bitmapIndex + 1)) {
                            int labelIndex = bitmapIndex++ - nodeId;
                            byteBuffer.put(labels[labelIndex]);
                            if (isLeaf.get(labelIndex + 1)) {
                                next = charset.decode(byteBuffer.flip()).toString();
                                return;
                            }
                        }
                    }
                    nodeId = bitmapIndex + 1 - nodeId;
                    bitmapIndex = labelBitmap.select1(nodeId) + 1;
                    byteBuffer.put(labels[nodeId - 1]);
                    if (isLeaf.get(nodeId)) {
                        next = charset.decode(byteBuffer.flip()).toString();
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
        if (bSearch && !labelBitmap.get(bitmapIndex + 1)) {
            int high = labelBitmap.select1(nodeId + 1) - 1;
            if (labelBitmap.get(high)) {
                return -1;
            }
            int index = Arrays.binarySearch(labels, bitmapIndex - nodeId, high - nodeId + 1, b);
            return index < 0 ? -1 : index + nodeId;
        } else {
            while (!labelBitmap.get(bitmapIndex)) {
                int labelIndex = bitmapIndex - nodeId;
                if (labels[labelIndex] == b) {
                    return bitmapIndex;
                }
                bitmapIndex++;
            }
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("ByteSuccinctTrie(%s)[%d labels, %d bits]", encoder.charset(), labels.length,
                labelBitmap.size());
    }
}