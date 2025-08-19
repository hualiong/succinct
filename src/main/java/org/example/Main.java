package org.example;

import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.archive.ByteSuccinctSet3;
import org.example.succinct.archive.CharSuccinctSet3;
import org.example.succinct.core.*;
import org.example.succinct.common.*;
import org.example.succinct.api.RankSelectBitSet;
import org.example.succinct.utils.Recorder;
import org.example.succinct.utils.StringEncoder;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class Main {
    public static void main(String[] args) throws IOException {
        iteratorTest(15);
    }

    public static void iteratorTest(int flag) throws IOException {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 32, 0.0f);
        System.out.printf("Data: %s\n", sizeOf(randoms));
        Arrays.parallelSort(randoms);
        if ((flag & 1) > 0) {
            Set<String> set = Arrays.stream(randoms).parallel().collect(Collectors.toSet());;
            long now = Timer.now();
            set.forEach(s -> {});
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", set.getClass().getSimpleName(), ms, sizeOf(set));
        }
        if ((flag & 2) > 0) {
            SimpleFSA fsa = new SimpleFSA(randoms);
            long now = Timer.now();
            BytesRefFSTEnum<Object> iter = fsa.iterator();
            while (iter.next() != null);
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", fsa.getClass().getSimpleName(), ms, sizeOf(fsa));
        }
        if ((flag & 4) > 0) {
            CharSuccinctTrie cst = CharSuccinctTrie.sortedOf(randoms);
            long now = Timer.now();
            cst.iterator(true).forEachRemaining(s -> {});
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", cst.getClass().getSimpleName(), ms, sizeOf(cst));
        }
        if ((flag & 8) > 0) {
            ByteSuccinctTrie bst = ByteSuccinctTrie.of(randoms);
            long now = Timer.now();
            bst.iterator(true).forEachRemaining(s -> {});
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", bst.getClass().getSimpleName(), ms, sizeOf(bst));
        }
    }

    public static void containsTest(int flag) {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 32, 0.0f);
        System.out.printf("Data: %s\n", sizeOf(randoms));
        Arrays.parallelSort(randoms);
        Recorder t = new Recorder();
        if ((flag & 1) > 0) {
            Set<String> set = Set.of(randoms);
            t.multi(randoms, set::contains);
            System.out.printf("%s: %dms | %s\n", set.getClass().getSimpleName(), t.sum(), sizeOf(set));
            t.reset();
        }
        if ((flag & 2) > 0) {
            SimpleFSA fsa = new SimpleFSA(randoms);
            t.multi(randoms, fsa::contains);
            System.out.printf("%s: %dms | %s\n", fsa.getClass().getSimpleName(), t.sum(), sizeOf(fsa));
            t.reset();
        }
        if ((flag & 4) > 0) {
            ByteSuccinctSet ss = ByteSuccinctSet.of(randoms);
            t.multi(randoms, ss::contains);
            System.out.printf("%s: %dms | %s\n", ss.getClass().getSimpleName(), t.sum(), sizeOf(ss));
            t.reset();
        }
        if ((flag & 8) > 0) {
            ByteSuccinctSet3 ss = ByteSuccinctSet3.of(randoms);
            t.multi(randoms, ss::contains);
            System.out.printf("%s: %dms | %s\n", ss.getClass().getSimpleName(), t.sum(), sizeOf(ss));
            t.reset();
        }
        if ((flag & 16) > 0) {
            CharSuccinctSet ss = CharSuccinctSet.sortedOf(randoms);
            t.multi(randoms, ss::contains);
            System.out.printf("%s: %dms | %s\n", ss.getClass().getSimpleName(), t.sum(), sizeOf(ss));
            t.reset();
        }
        if ((flag & 32) > 0) {
            CharSuccinctSet3 ss = CharSuccinctSet3.sortedOf(randoms);
            t.multi(randoms, ss::contains);
            System.out.printf("%s: %dms | %s\n", ss.getClass().getSimpleName(), t.sum(), sizeOf(ss));
            t.reset();
        }
    }

    public static void bitSetTest() {
        String[] keys = StringGenerateUtil.randomArray(1000000, 32, 0.0f);
        RankSelectBitSet.Builder builder = new RankSelectBitSet4.Builder();
        RankSelectBitSet bitSet = createLoudsBits(builder, keys);
        System.out.printf("memory: %s\n", sizeOf(bitSet));
        long t = Timer.now();
        for (int i = 0; i < bitSet.size(); i++) {
            bitSet.get(i);
        }
        long t0 = Timer.now();
        System.out.printf("get: %d ms\n", Timer.ms(t, t0));
        for (int i = 0; i < bitSet.size(); i++) {
            bitSet.rank1(i);
        }
        long t1 = Timer.now();
        System.out.printf("rank1: %d ms\n", Timer.ms(t0, t1));
        for (int i = 0; i < bitSet.oneCount(); i++) {
            bitSet.select1(i + 1);
        }
        long t2 = Timer.now();
        System.out.printf("select1: %d ms\n", Timer.ms(t1, t2));
        for (int i = 0; i < bitSet.size(); i++) {
            bitSet.rank0(i);
        }
        long t3 = Timer.now();
        System.out.printf("rank0: %d ms\n", Timer.ms(t2, t3));
        for (int i = 0; i < bitSet.size() - bitSet.oneCount(); i++) {
            bitSet.select0(i + 1);
        }
        long t4 = Timer.now();
        System.out.printf("select0: %d ms\n", Timer.ms(t3, t4));
    }

    public static void encodeTimeTest() {
        String charset = "GB18030";
        char[] c = new char[1024];
        StringEncoder encoder = new StringEncoder(Charset.forName(charset));
        String[] str = new String[10000000];
        Arrays.fill(str, "我是中国人");
        Recorder t = new Recorder();
        t.multi(str, String::toCharArray);
        System.out.println("CHAR: " + t.sum() + "ms");
        t.reset();
        t.multi(str, s -> StringEncoder.getChars(s, c));
        System.out.println("CHAR(getChars): " + t.sum() + "ms");
        t.reset();
        t.multi(str, String::getBytes);
        System.out.println("UTF-8: " + t.sum() + "ms");
        t.reset();
        t.multi(str, encoder::encodeToBytes);
        System.out.println(charset + "(encodeToBytes): " + t.sum() + "ms");
        t.reset();
        t.multi(str, encoder::encodeToBuffer);
        System.out.println(charset + "(encodeToBuffer): " + t.sum() + "ms");
        t.reset();
        t.multi(str, s -> s.getBytes(Charset.forName(charset)));
        System.out.println(charset + ": " + t.sum() + "ms");
    }

    private static RankSelectBitSet createLoudsBits(RankSelectBitSet.Builder builder, String[] keys) {
        Arrays.parallelSort(keys);
        Queue<Range> queue = new ArrayDeque<>();
        queue.add(new Range(0, keys.length, 0));
        int bitPos = 0;
        while (!queue.isEmpty()) {
            Range range = queue.poll();
            int L = range.L(), R = range.R(), index = range.index();
            int ptr = L;
            while (ptr < R && keys[ptr].length() == index) {
                ptr++;
            }
            int start = L;
            while (start < R) {
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
                builder.set(bitPos++, false);
                queue.add(new Range(start, end, index + 1));
                start = end;
            }
            builder.set(bitPos++, true);
        }
        return builder.build(true);
    }
}