package org.example.succinct.core;

import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.common.Range;
import org.example.succinct.common.RankSelectBitSet;
import org.example.succinct.common.SuccinctSet;

import java.util.*;

/**
 * 基于 char 数组实现的第一代 Succinct Set
 */
public class CharSuccinctSet implements SuccinctSet, Accountable {
    protected final char[] labels;
    protected final RankSelectBitSet labelBitmap;
    protected final RankSelectBitSet isLeaf;

    public static CharSuccinctSet of(String... keys) {
        return new CharSuccinctSet(keys, false);
    }

    public static CharSuccinctSet sortedOf(String... keys) {
        return new CharSuccinctSet(keys, true);
    }

    public CharSuccinctSet(String[] keys, boolean sorted) {
        if (!sorted) {
            Arrays.parallelSort(keys);
        }
        List<Character> labelsList = new ArrayList<>();
        RankSelectBitSet.Builder labelBitmapBuilder = new RankSelectBitSet.Builder();
        RankSelectBitSet.Builder isLeafBuilder = new RankSelectBitSet.Builder();

        Queue<Range> queue = new ArrayDeque<>();
        queue.add(new Range(0, keys.length, 0));
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
                // 添加子节点标签
                labelsList.add(currentChar);
                // 设置子节点标记(0)
                labelBitmapBuilder.set(bitPos, false);
                bitPos++;
                // 将子节点范围加入队列
                queue.add(new Range(start, end, index + 1));
                start = end;
            }
            // 设置节点结束标记(1)
            labelBitmapBuilder.set(bitPos, true);
            bitPos++;
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

    private int extract(String key) {
        int nodeId = 0, bitmapIndex = -1;
        for (char c : key.toCharArray()) {
            // 移动到下一个节点
            nodeId = bitmapIndex + 1 - nodeId;
            bitmapIndex = labelBitmap.select1(nodeId) + 1;
            while (true) {
                if (bitmapIndex >= labelBitmap.size || labelBitmap.get(bitmapIndex)) {
                    return -1;
                }
                // 计算标签索引
                int labelIndex = bitmapIndex - nodeId;
                if (labelIndex < labels.length && labels[labelIndex] == c) {
                    break;
                }
                bitmapIndex++;
            }
        }
        return bitmapIndex;
    }

    @Override
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

    @Override
    public boolean contains(String key) {
        int bitmapIndex = extract(key);
        if (bitmapIndex < 0) {
            return false;
        }
        return isLeaf.get(labelBitmap.rank0(bitmapIndex));
    }

    // public SuccinctIterator lowerBound(String key) {
    //     int bitmapIndex = extract(key);
    //     if (bitmapIndex < 0) {
    //         return null;
    //     }
    //     if (!isLeaf.get(labelBitmap.rank0(bitmapIndex))) {
    //         int nodeId = labelBitmap.rank1(bitmapIndex);
    //         while (0 <= bitmapIndex && bitmapIndex < labelBitmap.size) {
    //             if (labelBitmap.get(bitmapIndex)) {
    //                 // 回溯
    //                 while (0 <= bitmapIndex && labelBitmap.get(bitmapIndex)) {
    //                     bitmapIndex = labelBitmap.select0(nodeId);   // 在父节点的bit位置
    //                     nodeId = labelBitmap.rank1(bitmapIndex++);   // 父节点编码（并右移）
    //                 }
    //                 nodeId = labelBitmap.rank0(bitmapIndex);   // 右兄弟第一个子节点编码
    //                 bitmapIndex = labelBitmap.select1(nodeId); // 右兄弟节点bit位置
    //             } else {
    //                 // 向下转移
    //                 nodeId = bitmapIndex + 1 - nodeId; // 第一子节点
    //                 bitmapIndex = labelBitmap.select1(nodeId) + 1; // 及其起始bit位置
    //             }
    //         }
    //     }
    //     return new SuccinctIterator(bitmapIndex, labels, labelBitmap, isLeaf);
    // }

    // /*
    //  * 移动到下一层节点:
    //  * 节点3的起始bit位置 = select1(3) + 1 = 7
    //  * 节点3第一个子节点编号 = rank0(7 + 0) = 5
    //  * 节点3第二个子节点编号 = rank0(7 + 1) = 6
    //  * 节点3的父节点起始bit位置 = select0(3) = 2，父节点编号 = rank1(select0(3)) = rank1(2) = 0
    //  * 节点3的第1个子节点的起始bit位置 = select1(rank1(7 + 0)) + 1 = select1(5) + 1 = 11
    //  * 节点3的第2个子节点的起始bit位置 = select1(rank1(7 + 1)) + 1 = select1(6) + 1 = 12
    //  * 节点5的父节点起始bit位置 = select0(5) = 7，父节点编号 = rank1(select1(5)) = rank1(7) = 3
    //  * 节点6的父节点起始bit位置 = select0(6) = 8，父节点编号 = rank1(select1(6)) = rank1(8) = 3
    //  */
    // public static class SuccinctIterator implements Iterator<String> {
    //     private int nodeId;
    //     private int bitmapIndex;
    //     private final char[] labels;
    //     private final RankSelectBitSet labelBitmap;
    //     private final RankSelectBitSet isLeaf;
    //     private StringBuilder str = new StringBuilder();

    //     public SuccinctIterator(int bitmapIndex, char[] labels, RankSelectBitSet labelBitmap, RankSelectBitSet isLeaf) {
    //         this.nodeId = bitmapIndex + 1 - labelBitmap.rank1(bitmapIndex);
    //         this.bitmapIndex = bitmapIndex;
    //         this.labels = labels;
    //         this.labelBitmap = labelBitmap;
    //         this.isLeaf = isLeaf;
    //     }

    //     @Override
    //     public boolean hasNext() {
    //         return bitmapIndex < labelBitmap.size;
    //     }

    //     @Override
    //     public String next() {
    //         String string = str.append(labels[bitmapIndex - nodeId]).toString();
    //         while (true) {
    //             nodeId = labelBitmap.rank0(bitmapIndex);
    //             bitmapIndex = labelBitmap.select1(nodeId) + 1;

    //             if (bitmapIndex >= labelBitmap.size) {
    //                 break;
    //             }
    //             // 节点3的父节点起始bit位置 = select0(3) = 2，父节点编号 = rank1(select0(3)) = 0
    //             // pos = select0(rank0(pos))
    //             if (labelBitmap.get(bitmapIndex)) {
    //                 bitmapIndex++;
    //             }

    //             int labelIndex = bitmapIndex - nodeId;
    //             if (labelIndex < labels.length && labels[labelIndex] == c) {
    //                 break;
    //             }
    //         }
    //         return string;
    //     }
    // }

    @Override
    public long ramBytesUsed() {
        return RamUsageEstimator.sizeOf(labels)
                + RamUsageEstimator.sizeOf(labelBitmap)
                + RamUsageEstimator.sizeOf(isLeaf);
    }

    @Override
    public Collection<Accountable> getChildResources() {
        return List.of(labelBitmap, isLeaf);
    }

    @Override
    public String toString() {
        return "CharSuccinctSet[" + labels.length + " labels, " + labelBitmap.size + " bits]";
    }

}
