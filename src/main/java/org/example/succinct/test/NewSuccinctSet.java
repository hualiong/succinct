package org.example.succinct.test;

import java.util.ArrayList;
import java.util.List;

class NewSuccinctSet {
    private List<Long> leaves;
    private List<Long> labelBitmap;
    private List<Byte> labels;
    private int[] ranks;
    private int[] selects;

    // NewSet creates a new Set instance from a list of sorted strings.
    public static NewSuccinctSet of(String... keys) {
        NewSuccinctSet setInstance = new NewSuccinctSet();
        int labelIndex = 0;

        class QueueElement {
            int start;
            final int end;
            final int column;

            QueueElement(int start, int end, int column) {
                this.start = start;
                this.end = end;
                this.column = column;
            }
        }

        List<QueueElement> queue = new ArrayList<>();
        queue.add(new QueueElement(0, keys.length, 0));

        for (int i = 0; i < queue.size(); i++) {
            QueueElement element = queue.get(i);

            if (element.column == keys[element.start].length()) {
                // a leaf node
                element.start++;
                setBit(setInstance.leaves, i, 1);
            }

            for (int j = element.start; j < element.end; ) {
                int from = j;

                while (j < element.end && keys[j].charAt(element.column) == keys[from].charAt(element.column)) {
                    j++;
                }

                queue.add(new QueueElement(from, j, element.column + 1));
                setInstance.labels.add((byte) keys[from].charAt(element.column));
                setBit(setInstance.labelBitmap, labelIndex, 0);
                labelIndex++;
            }

            setBit(setInstance.labelBitmap, labelIndex, 1);
            labelIndex++;
        }

        setInstance.init();
        return setInstance;
    }

    // Has queries for a key and returns whether it is present in the Set.
    public boolean contains(String key) {
        int nodeId = 0, bitmapIndex = 0;

        for (int i = 0; i < key.length(); i++) {
            byte character = (byte) key.charAt(i);
            for (;;) {
                if (getBit(labelBitmap, bitmapIndex) != 0) {
                    // no more labels in this node
                    return false;
                }

                if (labels.get(bitmapIndex - nodeId) == character) {
                    break;
                }
                bitmapIndex++;
            }

            // go to next level
            nodeId = countZeros(labelBitmap, ranks, bitmapIndex + 1);
            bitmapIndex = selectIthOne(labelBitmap, ranks, selects, nodeId - 1) + 1;
        }

        return getBit(leaves, nodeId) != 0;
    }

    private static void setBit(List<Long> bitmap, int index, int value) {
        while (index >> 6 >= bitmap.size()) {
            bitmap.add(0L);
        }
        bitmap.set(index >> 6, bitmap.get(index >> 6) | ((long) value << (index & 63)));
    }

    private static long getBit(List<Long> bitmap, int index) {
        return bitmap.get(index >> 6) & (1L << (index & 63));
    }

    // init builds pre-calculated cache to speed up rank() and select()
    private void init() {
        ranks = new int[1];
        for (Long aLong : labelBitmap) {
            int count = Long.bitCount(aLong);
            ranks = append(ranks, ranks[ranks.length - 1] + count);
        }

        selects = new int[0];
        int count = 0;
        for (int i = 0; i < labelBitmap.size() << 6; i++) {
            int zero = (int) ((labelBitmap.get(i >> 6) >> (i & 63)) & 1);
            if (zero == 1 && count % 64 == 0) {
                selects = append(selects, i);
            }
            count += zero;
        }
    }

    // countZeros counts the number of "0" in a bitmap before the i-th bit (excluding
    // the i-th bit) on behalf of rank index.
    private static int countZeros(List<Long> bitmap, int[] ranks, int index) {
        return index - ranks[index >> 6] - Long.bitCount(bitmap.get(index >> 6) & (1L << (index & 63) - 1));
    }

    // selectIthOne returns the index of the i-th "1" in a bitmap, on behalf of rank
    // and select indexes.
    private static int selectIthOne(List<Long> bitmap, int[] ranks, int[] selects, int index) {
        int base = selects[index >> 6] & ~63;
        int findIthOne = index - ranks[base >> 6];

        for (int i = base >> 6; i < bitmap.size(); i++) {
            int bitIndex = 0;
            long w = bitmap.get(i);
            while (w > 0) {
                findIthOne -= (int) (w & 1);
                if (findIthOne < 0) {
                    return (i << 6) + bitIndex;
                }
                int trailingZeros = Long.numberOfTrailingZeros(w & ~1);
                w >>= trailingZeros;
                bitIndex += trailingZeros;
            }
        }
        throw new RuntimeException("no more ones");
    }

    private static int[] append(int[] array, int value) {
        int[] newArray = new int[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = value;
        return newArray;
    }

}
