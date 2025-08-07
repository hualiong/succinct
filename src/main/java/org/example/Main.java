package org.example;

import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.ByteSuccinctSet2;
import org.example.succinct.ByteSuccinctSet3;
import org.example.succinct.CharSuccinctSet2;
import org.example.succinct.CharSuccinctSet3;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.common.SuccinctSet;
import org.example.succinct.test.Timer;
import org.example.succinct.utils.StringEncoder;
import org.example.succinct.utils.StringGenerateUtil;
import org.openjdk.jol.info.GraphLayout;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        // SuccinctSet bss3 = ByteSuccinctSet3.of("ૢ䊵࡬ﻰॽ", "׶¸Нꖷм旋", "ੱºڣଟ਄莋", "顼୲¤঴ݖ¦鲧", "ӏ¸ݩµॿ઱", "হ·ꁫ㴯র¹਷µ", "⨵䐃ੜ±࣐µ䷣ύ", "఺»삄ఌ¹ଟ¥瓯", "ୁ 晔②݌¹ܾ»", "ऻ¢㫌쵐ઋ¾꾶幞", "ਪ·❱ি·패⺖ژ", "ଖ뢚膂଑ת¿", "ㄱ৐·晩샥댈㡏㡡", "ѱ¹颈॔ ѣᘏ", "ઉ҈¼Ҿ¦㋏띸");
        // System.out.println(bss3.contains("ૢ䊵࡬ﻰॽ"));
        // Charset charset = Charset.forName("GB18030");
        // StringEncoder encoder = new StringEncoder(charset);
        // System.out.println(Arrays.toString("".getBytes(charset)));
        // System.out.println(Arrays.toString(encoder.encodeToBytes('')));
        containsTimeTest(40);
    }
    
    public static void encodeTimeTest() {
        String charset = "GB18030";
        char[] c = new char[1024];
        StringEncoder encoder = new StringEncoder(Charset.forName(charset));
        String[] str = new String[10000000];
        Arrays.fill(str, "我是中国人");
        Timer t = new Timer();
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

    public static void containsTimeTest(int flag) {
        // String[] randoms = StringGenerateUtil.readArray("C:\\Users\\huazhaoming\\Desktop\\data\\100w_en.txt");
        String[] randoms = StringGenerateUtil.randomArray(1000000, 5, 0.8f);
        System.out.printf("Data: %s\n", extractSizeOf(randoms));
        Arrays.parallelSort(randoms);
        Timer t = new Timer();
        if ((flag & 1) > 0) {
            Set<String> set = Set.of(randoms);
            t.multi(randoms, set::contains);
            System.out.printf("SetN: %dms | %s\n", t.sum(), extractSizeOf(set));
            t.reset();
        }
        if ((flag & 2) > 0) {
            SimpleFSA fsa = new SimpleFSA(randoms);
            t.multi(randoms, fsa::contains);
            System.out.printf("FSA: %dms | %s\n", t.sum(), extractSizeOf(fsa));
        }
        if ((flag & 4) > 0) {
            SuccinctSet bss2 = ByteSuccinctSet2.of(randoms);
            t.multi(randoms, bss2::contains);
            System.out.printf("ByteSuccinctSet2: %dms | %s\n", t.sum(), extractSizeOf(bss2));
            t.reset();
        }
        if ((flag & 8) > 0) {
            SuccinctSet bss3 = ByteSuccinctSet3.of(randoms);
            t.multi(randoms, bss3::contains);
            System.out.printf("ByteSuccinctSet3: %dms | %s\n", t.sum(), extractSizeOf(bss3));
            t.reset();
        }
        if ((flag & 16) > 0) {
            SuccinctSet css2 = CharSuccinctSet2.sortedOf(randoms);
            t.multi(randoms, css2::contains);
            System.out.printf("CharSuccinctSet2: %dms | %s\n", t.sum(), extractSizeOf(css2));
            t.reset();
        }
        if ((flag & 32) > 0) {
            SuccinctSet css3 = CharSuccinctSet3.sortedOf(randoms);
            t.multi(randoms, css3::contains);
            System.out.printf("CharSuccinctSet3: %dms | %s\n", t.sum(), extractSizeOf(css3));
            t.reset();
        }
    }

    public static String sizeOf(Object o) {
        long bytes = o instanceof Accountable ? RamUsageEstimator.sizeOfObject(o)
                : GraphLayout.parseInstance(o).totalSize();
        return RamUsageEstimator.humanReadableUnits(bytes);
    }

    public static String computeSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(RamUsageEstimator.sizeOfObject(o));
    }

    public static String extractSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(GraphLayout.parseInstance(o).totalSize());
    }
}