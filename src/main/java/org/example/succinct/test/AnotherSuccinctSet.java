package org.example.succinct.test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AnotherSuccinctSet {
    private final long[] leaves;
    private final long[] labelBitmap;
    private final byte[] labels;
    private final int[] ranks;   // ranks[k] 表示前k个字（0到k-1）的1的个数
    private final int[] selects; // 存储每64个1的位置

    public static AnotherSuccinctSet of(String... keys) {
        return new AnotherSuccinctSet(keys);
    }

    public AnotherSuccinctSet(String[] keys) {
        List<Long> leavesList = new ArrayList<>();
        List<Long> labelBitmapList = new ArrayList<>();
        ByteArrayOutputStream labelsStream = new ByteArrayOutputStream();
        Queue<QNode> queue = new LinkedList<>();
        queue.add(new QNode(0, keys.length, 0));
        int lIdx = 0;
        int nodeCount = 0;

        while (!queue.isEmpty()) {
            QNode elt = queue.poll();

            // 标记叶节点
            if (elt.start < elt.end && elt.col == keys[elt.start].length()) {
                setBit(leavesList, nodeCount, 1);
                elt.start++;
            }

            // 处理子节点
            int j = elt.start;
            while (j < elt.end) {
                char c = keys[j].charAt(elt.col);
                int frm = j;
                while (j < elt.end && keys[j].charAt(elt.col) == c) {
                    j++;
                }
                queue.add(new QNode(frm, j, elt.col + 1));
                labelsStream.write((byte) c);
                setBit(labelBitmapList, lIdx, 0);
                lIdx++;
            }

            // 标记节点结束
            setBit(labelBitmapList, lIdx, 1);
            lIdx++;
            nodeCount++;
        }

        // 转换为数组
        this.leaves = toLongArray(leavesList, (nodeCount + 63) / 64);
        this.labelBitmap = toLongArray(labelBitmapList, (lIdx + 63) / 64);
        this.labels = labelsStream.toByteArray();

        // 初始化辅助数组
        int[] helperArrays = initRanksAndSelects(labelBitmap, lIdx);
        int rankSize = (lIdx >> 6) + 1;
        this.ranks = new int[rankSize];
        System.arraycopy(helperArrays, 0, this.ranks, 0, rankSize);

        int selectSize = helperArrays[rankSize] > 0 ?
                (helperArrays[rankSize] + 63) / 64 : 0;
        this.selects = new int[selectSize];
        System.arraycopy(helperArrays, rankSize + 1, this.selects, 0, selectSize);
    }

    public boolean contains(String key) {
        int nodeId = 0;
        int bmIdx = 0;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            while (true) {
                if (getBit(labelBitmap, bmIdx) != 0) {
                    return false; // 节点中没有更多标签
                }
                if (labels[bmIdx - nodeId] == (byte) c) {
                    break; // 找到匹配的标签
                }
                bmIdx++;
            }

            // 计算下一个节点ID
            nodeId = countZeros(labelBitmap, ranks, bmIdx + 1);
            if (nodeId == 0) return false;

            // 定位下一个位图索引
            bmIdx = selectIthOne(labelBitmap, ranks, selects, nodeId - 1) + 1;
        }
        return getBit(leaves, nodeId) != 0;
    }

    // 辅助类和方法
    private static class QNode {
        int start;
        final int end;
        final int col;
        QNode(int s, int e, int c) {
            start = s;
            end = e;
            col = c;
        }
    }

    private static void setBit(List<Long> bitmap, int pos, int val) {
        int idx = pos >> 6;
        while (idx >= bitmap.size()) {
            bitmap.add(0L);
        }
        long word = bitmap.get(idx);
        long mask = 1L << (pos & 63);
        bitmap.set(idx, val != 0 ? word | mask : word & ~mask);
    }

    private static long getBit(long[] bitmap, int pos) {
        int idx = pos >> 6;
        if (idx >= bitmap.length) return 0;
        return bitmap[idx] & (1L << (pos & 63));
    }

    private static long[] toLongArray(List<Long> list, int minSize) {
        long[] arr = new long[Math.max(minSize, list.size())];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static int[] initRanksAndSelects(long[] bitmap, int bitLen) {
        int wordCount = (bitLen + 63) / 64;
        int[] ranks = new int[wordCount + 1];
        ranks[0] = 0;

        // 计算每个字之前的累积1的数量
        for (int i = 0; i < wordCount; i++) {
            long word = i < bitmap.length ? bitmap[i] : 0;
            if (i == wordCount - 1 && (bitLen & 63) != 0) {
                word &= (1L << (bitLen & 63)) - 1; // 清除高位
            }
            ranks[i + 1] = ranks[i] + Long.bitCount(word);
        }

        int totalOnes = ranks[wordCount];
        List<Integer> selectsList = new ArrayList<>();

        // 每64个1记录一次位置
        int count = 0;
        for (int i = 0; i < bitLen; i++) {
            if (getBit(bitmap, i) != 0) {
                if ((count & 63) == 0) {
                    selectsList.add(i);
                }
                count++;
            }
        }

        // 合并结果: ranks数组 + 总1数 + selects数组
        int[] result = new int[ranks.length + 1 + selectsList.size()];
        System.arraycopy(ranks, 0, result, 0, ranks.length);
        result[ranks.length] = totalOnes;
        for (int i = 0; i < selectsList.size(); i++) {
            result[ranks.length + 1 + i] = selectsList.get(i);
        }
        return result;
    }

    private static int countZeros(long[] bitmap, int[] ranks, int pos) {
        int wordIdx = pos >> 6;
        int count;

        // 获取前wordIdx个字的1的总数
        if (wordIdx < ranks.length) {
            count = ranks[wordIdx];
        } else {
            count = ranks[ranks.length - 1];
        }

        // 加上当前字中pos之前的1的数量
        if (wordIdx < bitmap.length) {
            long word = bitmap[wordIdx];
            word &= (1L << (pos & 63)) - 1; // 清除高位
            count += Long.bitCount(word);
        }

        return pos - count; // 总位数 - 1的数量 = 0的数量
    }

    private static int selectIthOne(long[] bitmap, int[] ranks, int[] selects, int i) {
        if (i < 0) return -1;
        if (i >= ranks[ranks.length - 1]) {
            return bitmap.length << 6; // 返回位图末尾
        }

        // 计算基础位置
        int block = i >> 6;
        int base = block < selects.length ? selects[block] : 0;
        base &= ~63; // 对齐到64的倍数

        // 计算相对位置
        int wordIdx = base >> 6;
        int findIthOne = i - ranks[wordIdx]; // 在base之后需要查找的1的数量

        while (wordIdx < bitmap.length) {
            long word = bitmap[wordIdx];
            int bits = Long.bitCount(word);

            if (findIthOne < bits) {
                // 在当前字中找到目标
                while (findIthOne > 0) {
                    word &= word - 1; // 移除最低位的1
                    findIthOne--;
                }
                return (wordIdx << 6) + Long.numberOfTrailingZeros(word);
            }

            // 移动到下一个字
            findIthOne -= bits;
            wordIdx++;
        }

        return bitmap.length << 6; // 未找到，返回末尾
    }
}