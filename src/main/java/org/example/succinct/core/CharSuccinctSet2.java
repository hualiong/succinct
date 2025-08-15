package org.example.succinct.core;

import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.common.Range;
import org.example.succinct.common.RankSelectBitSet3;
import org.example.succinct.api.SuccinctSet;

import it.unimi.dsi.fastutil.chars.CharArrayList;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

/**
 * 基于 char 数组实现的第二代 Succinct Set
 */
public class CharSuccinctSet2 implements SuccinctSet, Accountable {
    protected final char[] labels;
    protected final RankSelectBitSet3 labelBitmap;
    protected final RankSelectBitSet3 isLeaf;

    public static CharSuccinctSet2 of(String... keys) {
        return new CharSuccinctSet2(keys, false);
    }

    public static CharSuccinctSet2 sortedOf(String... keys) {
        return new CharSuccinctSet2(keys, true);
    }

    public CharSuccinctSet2(String[] keys, boolean sorted) {
        if (!sorted) {
            Arrays.parallelSort(keys);
        }
        CharArrayList labels = new CharArrayList();
        RankSelectBitSet3.Builder labelBitmapBuilder = new RankSelectBitSet3.Builder();
        RankSelectBitSet3.Builder isLeafBuilder = new RankSelectBitSet3.Builder();

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
                labels.add(currentChar);
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
        this.labels = labels.toCharArray();
        this.labelBitmap = labelBitmapBuilder.build(true);
        this.isLeaf = isLeafBuilder.build(false);
    }

    private int getNodeIdByKey(String key) {
        int nodeId = 0, bitmapIndex = 0;
        for (char c : key.toCharArray()) {
            int low = bitmapIndex, mid = -1, high = labelBitmap.select1(nodeId + 1) - 1;
            if (high >= labelBitmap.size || labelBitmap.get(high)) {
                return -1;
            }
            while (low <= high) {
                mid = low + high >>> 1;
                char label = labels[mid - nodeId];
                if (label == c) {
                    break;
                } else if (label < c) {
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
        int nodeId = getNodeIdByKey(key);
        return nodeId >= 0 && isLeaf.get(nodeId);
    }

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
