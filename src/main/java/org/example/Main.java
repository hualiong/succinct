package org.example;

import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.ByteSuccinctSet;
import org.example.succinct.ByteSuccinctSet2;
import org.example.succinct.CharSuccinctSet;
import org.example.succinct.CharSuccinctSet2;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.common.SuccinctSet;
import org.example.succinct.test.Timer;
import org.example.succinct.utils.StringCodecUtil;
import org.openjdk.jol.info.GraphLayout;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

public class Main {
    private static final CharsetEncoder UTF8_ENCODER = StandardCharsets.UTF_8.newEncoder();

    public static void main(String[] args) {
        encodeTimeTest();
    }

    public static void encodeTimeTest() {
        ByteBuffer buffer = ByteBuffer.allocate(128);
        CharBuffer charBuffer = CharBuffer.allocate(20);
        String[] str = new String[10000000];
        Arrays.fill(str, "Succinct");
        Timer t1 = new Timer();
        Timer t2 = new Timer();
        Timer t3 = new Timer();
        Timer t4 = new Timer();
        t1.multi(String::toCharArray, str);
        t2.multi(s -> {
            charBuffer.clear();
            charBuffer.append(s);
            UTF8_ENCODER.encode(charBuffer, buffer, true);
        }, str);
        t3.multi(s -> s.getBytes(Charset.forName("GBK")), str);
        t4.multi(s -> s.getBytes(Charset.forName("GB18030")), str);
        System.out.println("char: " + t1.sum() + "ms");
        System.out.println("utf-8: " + t2.sum() + "ms");
        System.out.println("gbk: " + t3.sum() + "ms");
        System.out.println("gb18030: " + t4.sum() + "ms");
    }

    public static void queryTimeTest() {
        int count = 2000000;
        String[] randoms = StringCodecUtil.randomArray(count, 8, 0.0f);
        String[] copyOf = Arrays.copyOf(randoms, count >>> 1);
        Set<String> unique = Set.of(copyOf);
        SuccinctSet set = ByteSuccinctSet.of(copyOf);
        SuccinctSet set2 = ByteSuccinctSet2.of(copyOf);
        SuccinctSet charSet = CharSuccinctSet.of(copyOf);
        SuccinctSet charSet2 = CharSuccinctSet2.of(copyOf);
        SimpleFSA fsa = new SimpleFSA(copyOf);
        Timer t1 = new Timer();
        Timer t2 = new Timer();
        Timer t22 = new Timer();
        Timer t3 = new Timer();
        Timer t32 = new Timer();
        Timer t4 = new Timer();
        t1.multi(unique::contains, randoms);
        t2.multi(set::contains, randoms);
        t22.multi(set2::contains, randoms);
        t3.multi(charSet::contains, randoms);
        t32.multi(charSet2::contains, randoms);
        t4.multi(fsa::contains, randoms);
        System.out.printf("SetN: %dms | %s\n", t1.sum(), extractSizeOf(unique));
        System.out.printf("ByteSuccinctSet: %dms | %s\n", t2.sum(), extractSizeOf(set));
        System.out.printf("ByteSuccinctSet2: %dms | %s\n", t22.sum(), extractSizeOf(set2));
        System.out.printf("CharSuccinctSet: %dms | %s\n", t3.sum(), extractSizeOf(charSet));
        System.out.printf("CharSuccinctSet2: %dms | %s\n", t32.sum(), extractSizeOf(charSet2));
        System.out.printf("FSA: %dms | %s\n", t4.sum(), extractSizeOf(fsa));
    }

    public static String extractSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(GraphLayout.parseInstance(o).totalSize());
    }
}