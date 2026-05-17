package org.example.succinct.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.example.succinct.api.SuccinctTrie;

/**
 * 压缩基数树（路径压缩，单孩子合并，孩子排序数组 + 二分查找）。
 * 终端标志编码进 childCount（负数）。
 */
public class CompactRadixTree implements SuccinctTrie {

    private char[] labels;
    private int[] labelStart;     // 长度 nodeCount+1，最后一位为 labels.length（哨兵）
    private int[] firstChild;
    private short[] childCount;   // 负数终端，绝对值为孩子数
    private int[] children;
    private int nodeCount;
    private int termCount;

    private static class RadixNode {
        String edge;
        boolean terminal;
        TreeMap<Character, RadixNode> children = new TreeMap<>();

        RadixNode(String edge) { this.edge = edge; }
    }

    public CompactRadixTree(String[] words) {
        // 1. 构建可变基数树
        RadixNode root = new RadixNode("");
        for (String w : words) insert(root, w);
        mergeSingleChild(root, true);

        // 2. 先序遍历收集节点及父索引
        List<RadixNode> nodeList = new ArrayList<>();
        List<Integer> parentIdxList = new ArrayList<>();
        traverse(root, nodeList, parentIdxList, -1);

        nodeCount = nodeList.size();
        labelStart = new int[nodeCount + 1];
        firstChild = new int[nodeCount];
        childCount = new short[nodeCount];
        Arrays.fill(firstChild, -1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nodeCount; i++) {
            labelStart[i] = sb.length();
            sb.append(nodeList.get(i).edge);
        }
        labels = sb.toString().toCharArray();
        labelStart[nodeCount] = labels.length;   // 哨兵

        // 3. 统计孩子数，构建 children 数组
        int[] childCnt = new int[nodeCount];
        for (int i = 1; i < nodeCount; i++) childCnt[parentIdxList.get(i)]++;

        int total = 0;
        for (int i = 0; i < nodeCount; i++) {
            if (childCnt[i] > 0) {
                firstChild[i] = total;
                total += childCnt[i];
            }
        }
        children = new int[total];
        int[] pos = new int[nodeCount];      // 写入指针
        for (int i = 0; i < nodeCount; i++) pos[i] = firstChild[i];
        for (int i = 1; i < nodeCount; i++) {
            int p = parentIdxList.get(i);
            children[pos[p]++] = i;
        }

        termCount = 0;
        for (int i = 0; i < nodeCount; i++) {
            int cnt = childCnt[i];
            if (nodeList.get(i).terminal) {
                termCount++;
                childCount[i] = (short) (cnt == 0 ? -1 : -cnt);
            } else {
                childCount[i] = (short) cnt;
            }
        }
    }

    private void insert(RadixNode node, String word) {
        if (word.isEmpty()) { node.terminal = true; return; }
        char first = word.charAt(0);
        RadixNode child = node.children.get(first);
        if (child == null) {
            RadixNode leaf = new RadixNode(word);
            leaf.terminal = true;
            node.children.put(first, leaf);
            return;
        }
        String edge = child.edge;
        int common = 0, min = Math.min(edge.length(), word.length());
        while (common < min && edge.charAt(common) == word.charAt(common)) common++;
        if (common == edge.length()) {
            insert(child, word.substring(common));
        } else {
            String commonPart = word.substring(0, common);
            String childRemain = edge.substring(common);
            String wordRemain = word.substring(common);
            RadixNode split = new RadixNode(commonPart);
            split.children.put(childRemain.charAt(0), child);
            child.edge = childRemain;
            node.children.put(commonPart.charAt(0), split);
            if (wordRemain.isEmpty()) split.terminal = true;
            else {
                RadixNode leaf = new RadixNode(wordRemain);
                leaf.terminal = true;
                split.children.put(wordRemain.charAt(0), leaf);
            }
        }
    }

    private void mergeSingleChild(RadixNode node, boolean isRoot) {
        for (RadixNode child : node.children.values()) mergeSingleChild(child, false);
        if (!isRoot && !node.terminal && node.children.size() == 1) {
            RadixNode child = node.children.values().iterator().next();
            node.edge += child.edge;
            node.terminal = child.terminal;
            node.children = child.children;
        }
    }

    private void traverse(RadixNode node, List<RadixNode> nodeList, List<Integer> parentIdxList, int parentIdx) {
        int idx = nodeList.size();
        nodeList.add(node);
        parentIdxList.add(parentIdx);
        for (RadixNode child : node.children.values()) traverse(child, nodeList, parentIdxList, idx);
    }

    private int getLabelLen(int idx) {
        return labelStart[idx + 1] - labelStart[idx];
    }

    private int getChildCount(int idx) {
        short c = childCount[idx];
        return c < 0 ? -c : c;
    }

    private boolean isTerminal(int idx) {
        return childCount[idx] < 0;
    }

    @Override public int size() { return termCount; }
    @Override public int nodeCount() { return nodeCount; }

    @Override
    public boolean contains(String key) {
        int node = findNode(key, true);
        return node != -1 && isTerminal(node);
    }

    @Override
    public int index(String key) {
        int node = findNode(key, true);
        return (node != -1 && isTerminal(node)) ? node : -1;
    }

    @Override
    public String get(int nodeId) {
        if (nodeId < 0 || nodeId >= nodeCount || !isTerminal(nodeId)) return null;
        return collectString(0, nodeId, "");
    }

    private String collectString(int cur, int target, String prefix) {
        String label = new String(labels, labelStart[cur], getLabelLen(cur));
        String current = prefix + label;
        if (cur == target) return current;
        int fc = firstChild[cur];
        if (fc == -1) return null;
        int cc = getChildCount(cur);
        for (int i = 0; i < cc; i++) {
            String res = collectString(children[fc + i], target, current);
            if (res != null) return res;
        }
        return null;
    }

    @Override
    public Iterator<String> iterator(boolean orderly) {
        return new RadixTreeIterator(0, "");
    }

    @Override
    public Iterator<String> prefixKeysOf(String str) {
        if (str.isEmpty()) return Collections.emptyIterator();
        List<String> results = new ArrayList<>();
        int cur = 0, pos = 0;
        while (cur != -1 && pos < str.length()) {
            int start = labelStart[cur], len = getLabelLen(cur);
            if (pos + len > str.length()) {
                if (startsWith(labels, start, len, str, pos)) break;
                return results.iterator();
            }
            if (!regionMatches(labels, start, str, pos, len)) return results.iterator();
            pos += len;
            if (isTerminal(cur) && cur != 0) results.add(collectString(0, cur, ""));
            if (pos == str.length()) return results.iterator();
            cur = findChild(cur, str.charAt(pos));
        }
        return results.iterator();
    }

    @Override
    public Iterator<String> prefixSearch(String prefix) {
        if (prefix == null || prefix.isEmpty()) return iterator(true);
        int node = findNode(prefix, false);
        if (node == -1) return new RadixTreeIterator(-1, null);
        return new RadixTreeIterator(node, prefix);
    }

    private int findNode(String s, boolean exact) {
        int cur = 0, pos = 0;
        while (cur != -1 && pos < s.length()) {
            int start = labelStart[cur], len = getLabelLen(cur);
            if (pos + len > s.length()) {
                if (startsWith(labels, start, len, s, pos)) return exact ? -1 : cur;
                return -1;
            }
            if (!regionMatches(labels, start, s, pos, len)) return -1;
            pos += len;
            if (pos == s.length()) return cur;
            cur = findChild(cur, s.charAt(pos));
        }
        return (pos == s.length()) ? cur : -1;
    }

    private int findChild(int parent, char ch) {
        int base = firstChild[parent];
        if (base == -1) return -1;
        int low = 0, high = getChildCount(parent) - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int child = children[base + mid];
            char first = labels[labelStart[child]];
            if (first == ch) return child;
            if (first < ch) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    private boolean regionMatches(char[] arr, int arrOff, String str, int strOff, int len) {
        for (int i = 0; i < len; i++)
            if (arr[arrOff + i] != str.charAt(strOff + i)) return false;
        return true;
    }

    private boolean startsWith(char[] arr, int arrOff, int arrLen, String str, int strOff) {
        int limit = Math.min(arrLen, str.length() - strOff);
        return regionMatches(arr, arrOff, str, strOff, limit);
    }

    private class RadixTreeIterator extends TermIterator {
        private Deque<Integer> nodeStack = new ArrayDeque<>();
        private Deque<String> prefixStack = new ArrayDeque<>();

        RadixTreeIterator(int startNode, String startPrefix) {
            if (startNode == -1) { this.next = null; return; }
            nodeStack.push(startNode);
            prefixStack.push(startPrefix);
            advance();
        }

        @Override
        protected void advance() {
            while (!nodeStack.isEmpty()) {
                int node = nodeStack.pop();
                String prefix = prefixStack.pop();
                int fc = firstChild[node];
                int cc = getChildCount(node);
                if (fc != -1) {
                    for (int i = cc - 1; i >= 0; i--) {
                        int child = children[fc + i];
                        nodeStack.push(child);
                        prefixStack.push(prefix + new String(labels, labelStart[child], getLabelLen(child)));
                    }
                }
                if (isTerminal(node)) { this.next = prefix; return; }
            }
            this.next = null;
        }
    }
}