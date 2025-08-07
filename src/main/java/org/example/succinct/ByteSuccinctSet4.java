package org.example.succinct;

import org.example.succinct.common.Range;
import org.example.succinct.common.RankSelectBitSet3;
import org.example.succinct.common.SuccinctSet;
import org.example.succinct.utils.StringEncoder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.nio.charset.Charset;
import java.util.*;

public class ByteSuccinctSet4 implements SuccinctSet {
    protected final byte[][] labels;
    protected final RankSelectBitSet3 labelBitmap;
    protected final RankSelectBitSet3 isLeaf;
    protected final StringEncoder encoder;
    protected final char[] buffer = new char[128];
    
    public static ByteSuccinctSet4 of(String... keys) {
        return new ByteSuccinctSet4(keys, "GB18030");
    }

    public ByteSuccinctSet4(String[] keys, String charset) {
        // 转换为字节数组并排序
        encoder = new StringEncoder(Charset.forName(charset));
        Arrays.parallelSort(keys);
        // byte[][] keyBytes = new byte[keys.length][];
        // for (int i = 0; i < keys.length; i++) {
        //     keyBytes[i] = encoder.encodeToBytes(keys[i]);
        // }

        // // 按字节数组字典序排序
        // Arrays.parallelSort(keyBytes, (a, b) -> {
        //     int minLen = Math.min(a.length, b.length);
        //     for (int i = 0; i < minLen; i++) {
        //         int cmp = Byte.compare(a[i], b[i]);
        //         if (cmp != 0)
        //             return cmp;
        //     }
        //     return a.length - b.length;
        // });
        ObjectArrayList<byte[]> labels = new ObjectArrayList<>();
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
            while (ptr < R && keys[ptr].length() == index) {
                isLeafNode = true;
                ptr++;
            }
            isLeafBuilder.set(nodeId, isLeafNode);

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
                // 添加子节点标签(byte)
                labels.add(encoder.encodeToBytes(currentChar));
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
        this.labels = labels.toArray(new byte[0][]);
        this.labelBitmap = labelBitmapBuilder.build(true);
        this.isLeaf = isLeafBuilder.build(false);
        System.out.printf("bss: labels: %d, bitmap: %d, isLeaf: %d\n", this.labels.length, labelBitmap.size, isLeaf.size);
    }

    public int extract(String key) {
        int nodeId = getNodeIdByKey(key);
        return isLeaf.get(nodeId) ? nodeId : -1;
    }

    @Override
    public boolean contains(String key) {
        return isLeaf.get(getNodeIdByKey(key));
    }

    @Override
    public String get(int nodeId) {
        if (isLeaf.get(nodeId)) {
            int id = nodeId;
            Deque<Byte> str = new LinkedList<>();
            int bitmapIndex;
            while ((bitmapIndex = labelBitmap.select0(id)) >= 0) {
                id = labelBitmap.rank1(bitmapIndex);
                for (byte b : labels[bitmapIndex - id]) {
                    str.push(b);
                }
            }
            byte[] bytes = new byte[str.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = str.pop();
            }
            return new String(bytes, encoder.charset());
        }
        return null;
    }

    private int getNodeIdByKey(String key) {
        int nodeId = 0, bitmapIndex = 0;
        int length = StringEncoder.getChars(key, buffer);
        for (int i = 0; i < length; i++) {
            int low = bitmapIndex, mid = -1, high = labelBitmap.select1(nodeId + 1) - 1;
            if (high >= labelBitmap.size || labelBitmap.get(high)) {
                return -1;
            }
            while (low <= high) {
                mid = low + high >>> 1;
                byte[] label = labels[mid - nodeId];
                int cmp = Arrays.compare(label, encoder.encodeToBytes(buffer[i]));
                if (cmp == 0) {
                    break;
                } else if (cmp < 0) {
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

    public static List<byte[]> splitGb18030Bytes(byte[] bytes) {
        List<byte[]> bytesList = new ArrayList<>();
        int i = 0;
        while (i < bytes.length) {
            int charLength = (bytes[i] & 0xFF) >= 0x81 ? 2 : 1; // 简单判断
            byte[] charBytes = new byte[charLength];
            System.arraycopy(bytes, i, charBytes, 0, charLength);
            bytesList.add(charBytes);
            i += charLength;
        }
        return bytesList;
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
    public String toString() {
        return "ByteSuccinctSet4(" + encoder.charset() + ")[" + labels.length + " labels, " + labelBitmap.size + " bits]";
    }
}