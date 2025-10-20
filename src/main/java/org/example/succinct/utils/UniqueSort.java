package org.example.succinct.utils;

import java.util.Arrays;

public class UniqueSort {

    /**
     * 原地排序并去重字符串数组
     * 
     * @param arr 字符串数组
     * @return 去重后的新长度
     */
    public static int sort(String[] arr) {
        if (arr == null || arr.length == 0) {
            return 0;
        }
        return markAndSort(arr, 0, arr.length - 1);
    }

    /**
     * 改良版快速排序，在排序过程中去重
     */
    private static int markAndSort(String[] arr, int left, int right) {
        if (left >= right) {
            return 1; // 单个元素直接返回
        }

        // 随机选择基准元素，避免有序序列的性能退化
        int random = left + (int)(Math.random() * (right - left + 1));
        swap(arr, left, random);

        // 三路快排分区：将数组分为 < pivot, == pivot, > pivot 三部分
        String pivot = arr[left];
        int lt = left; // 小于pivot的右边界
        int gt = right; // 大于pivot的左边界
        int i = left + 1; // 当前处理位置

        while (i <= gt) {
            int cmp = arr[i].compareTo(pivot);

            if (cmp < 0) {
                // 小于pivot，交换到左侧
                swap(arr, lt++, i++);
            } else if (cmp > 0) {
                // 大于pivot，交换到右侧
                swap(arr, i, gt--);
            } else {
                // 等于pivot，标记为重复（设置为null）
                arr[i] = null;
                i++;
            }
        }

        // 递归处理左右两部分
        int leftCount = lt > left ? markAndSort(arr, left, lt - 1) : 0;
        int rightCount = gt < right ? markAndSort(arr, gt + 1, right) : 0;

        // 将唯一元素紧凑排列到数组前部
        return mergeAndDeduplicate(arr, left, lt, gt, right, leftCount, rightCount, pivot);
    }

    /**
     * 紧凑排列唯一元素并合并结果
     */
    private static int mergeAndDeduplicate(String[] arr, int left, int lt, int gt,
            int right, int leftCount, int rightCount, String pivot) {
        // 放置pivot元素
        int pivotPos = left + leftCount;
        arr[pivotPos] = pivot;

        // 将右分区的唯一元素移动到pivot后面
        if (gt != pivotPos && rightCount > 0) {
            System.arraycopy(arr, gt + 1, arr, pivotPos + 1, rightCount);
        }

        return leftCount + 1 + rightCount; // 左分区唯一数 + pivot + 右分区唯一数
    }

    /**
     * 交换数组元素
     */
    private static void swap(String[] arr, int i, int j) {
        String temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // 测试代码
    public static void main(String[] args) {
        // 生成测试数据
        int size = 1000000;
        String[] testData = StringGenerateUtil.randomArray(size, 2, 8, 0.0f);

        // 测试改良算法
        String[] copy1 = testData.clone();
        long start1 = System.currentTimeMillis();
        int length1 = sort(copy1);
        long end1 = System.currentTimeMillis();

        // 测试传统方法（排序+去重）
        String[] copy2 = testData;
        long start2 = System.currentTimeMillis();
        Arrays.sort(copy2);
        long end2 = System.currentTimeMillis();
        int length2 = removeDuplicatesTraditional(copy2);

        System.out.println("性能测试 (" + size + " 个元素):");
        System.out.println("改良算法: " + (end1 - start1) + "ms, 去重后长度: " + length1);
        System.out.println("传统方法: " + (end2 - start2) + "ms, 去重后长度: " + length2);
        System.out.println("匹配结果: " + Arrays.equals(copy1, 0, length1, copy2, 0, length2));
    }

    /**
     * 传统去重方法用于对比
     */
    private static int removeDuplicatesTraditional(String[] sortedArray) {
        if (sortedArray.length == 0)
            return 0;

        int uniqueCount = 1;
        for (int i = 1; i < sortedArray.length; i++) {
            if (!sortedArray[i].equals(sortedArray[uniqueCount - 1])) {
                sortedArray[uniqueCount++] = sortedArray[i];
            }
        }
        return uniqueCount;
    }
}