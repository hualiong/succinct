package org.example.succinct.core;

import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.common.Range;
import org.example.succinct.common.RankSelectBitSet3;
import org.example.succinct.api.SuccinctSet;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;

import java.nio.charset.Charset;
import java.util.*;

/**
 * 基于 byte 数组实现的第二代 Succinct Set
 */
public class ByteSuccinctSet2 implements SuccinctSet, Accountable {
    protected final Charset charset;
    protected final byte[] labels;
    protected final RankSelectBitSet3 labelBitmap;
    protected final RankSelectBitSet3 isLeaf;

    public static ByteSuccinctSet2 of(String... keys) {
        return new ByteSuccinctSet2(keys, "GB18030");
    }

    public ByteSuccinctSet2(String[] keys, String charset) {
        // 转换为字节数组并排序
        this.charset = Charset.forName(charset);
        byte[][] keyBytes = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            keyBytes[i] = keys[i].getBytes(this.charset);
        }

        // 按字节数组字典序排序
        Arrays.parallelSort(keyBytes, (a, b) -> {
            int minLen = Math.min(a.length, b.length);
            for (int i = 0; i < minLen; i++) {
                int cmp = Byte.compare(a[i], b[i]);
                if (cmp != 0)
                    return cmp;
            }
            return a.length - b.length;
        });

        ByteArrayList labels = new ByteArrayList();
        RankSelectBitSet3.Builder labelBitmapBuilder = new RankSelectBitSet3.Builder();
        RankSelectBitSet3.Builder isLeafBuilder = new RankSelectBitSet3.Builder();

        Queue<Range> queue = new ArrayDeque<>();
        queue.add(new Range(0, keys.length, 0)); // 初始字节索引=0
        int bitPos = 0;
        int nodeId = 0;

        while (!queue.isEmpty()) {
            Range range = queue.poll();
            int L = range.L();
            int R = range.R();
            int index = range.index();

            // 检查当前节点是否是叶子节点
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
                labelBitmapBuilder.set(bitPos, false); // 子节点标记
                bitPos++;
                // 将子节点范围加入队列(字节索引+1)
                queue.add(new Range(start, end, index + 1));
                start = end;
            }

            // 设置节点结束标记
            labelBitmapBuilder.set(bitPos, true); // 结束标记
            bitPos++;
            nodeId++;
        }

        // 转换并初始化标签数组(byte)
        this.labels = labels.toByteArray();
        this.labelBitmap = labelBitmapBuilder.build(true);
        this.isLeaf = isLeafBuilder.build(false);
    }

    public int extract(String key) {
        int nodeId = getNodeIdByKey(key);
        return nodeId >= 0 && isLeaf.get(nodeId) ? nodeId : -1;
    }

    @Override
    public boolean contains(String key) {
        int nodeId = getNodeIdByKey(key);
        return nodeId >= 0 && isLeaf.get(nodeId);
    }

    @Override
    public String get(int nodeId) {
        if (isLeaf.get(nodeId)) {
            int id = nodeId;
            Deque<Byte> str = new LinkedList<>();
            int bitmapIndex;
            while ((bitmapIndex = labelBitmap.select0(id)) >= 0) {
                id = labelBitmap.rank1(bitmapIndex);
                str.push(labels[bitmapIndex - id]);
            }
            byte[] bytes = new byte[str.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = str.pop();
            }
            return new String(bytes, charset);
        }
        return null;
    }

    private int getNodeIdByKey(String key) {
        byte[] bytes = key.getBytes(charset);
        int nodeId = 0, bitmapIndex = 0;
        for (byte b : bytes) {
            // while (true) {
            // if (bitmapIndex >= labelBitmap.size || labelBitmap.get(bitmapIndex)) {
            // return -1;
            // }
            // int labelIndex = bitmapIndex - nodeId;
            // if (labelIndex < labels.length && labels[labelIndex] == b) {
            // break;
            // }
            // bitmapIndex++;
            // }
            // nodeId = bitmapIndex + 1 - nodeId;
            // bitmapIndex = labelBitmap.select1(nodeId) + 1;
            int low = bitmapIndex, mid = -1, high = labelBitmap.select1(nodeId + 1) - 1;
            if (high >= labelBitmap.size || labelBitmap.get(high)) {
                return -1;
            }
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
            if (low > high) {
                return -1;
            }
            nodeId = mid + 1 - nodeId;
            bitmapIndex = labelBitmap.select1(nodeId) + 1;
        }
        return nodeId;
    }

    // public TermIterator advanceExact(String key) {

    // }

    public TermIterator iterator() {
        return new TermIterator();
    }

    public class TermIterator implements Iterator<String> {
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
    }

    @Override
    public long ramBytesUsed() {
        return RamUsageEstimator.sizeOfObject(charset)
                + RamUsageEstimator.sizeOf(labels)
                + labelBitmap.ramBytesUsed()
                + isLeaf.ramBytesUsed();
    }

    @Override
    public Collection<Accountable> getChildResources() {
        return List.of(labelBitmap, isLeaf);
    }

    @Override
    public String toString() {
        return "ByteSuccinctSet2(" + charset + ")[" + labels.length + " labels, " + labelBitmap.size + " bits]";
    }

}