package org.example.succinct.core;

import org.example.succinct.api.RankSelectBitSet;
import org.example.succinct.common.Range;
import org.example.succinct.common.RankSelectBitSet1;
import org.example.succinct.api.SuccinctSet;

import java.util.*;

/**
 * 基于 char 数组实现的第一代 Succinct Set
 */
public class CharSuccinctSet implements SuccinctSet {
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
        RankSelectBitSet.Builder labelBitmapBuilder = new RankSelectBitSet1.Builder();
        RankSelectBitSet.Builder isLeafBuilder = new RankSelectBitSet1.Builder();

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
                if (bitmapIndex >= labelBitmap.size() || labelBitmap.get(bitmapIndex)) {
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

    @Override
    public String toString() {
        return "CharSuccinctSet[" + labels.length + " labels, " + labelBitmap.size() + " bits]";
    }

}
