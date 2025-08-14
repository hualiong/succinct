package org.example;

import org.example.succinct.ByteSuccinctSet2;
import org.example.succinct.ByteSuccinctSet3;
import org.example.succinct.CharSuccinctSet2;
import org.example.succinct.CharSuccinctSet3;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.common.SuccinctSet;
import org.example.succinct.test.Recorder;
import org.example.succinct.utils.StringEncoder;
import org.example.succinct.utils.StringGenerateUtil;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;

public class Main {
    public static void main(String[] args) {
        containsTimeTest(40);
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

    public static void containsTimeTest(int flag) {
        // String[] randoms = StringGenerateUtil.readArray("C:\\Users\\huazhaoming\\Desktop\\data\\100w_en.txt");
        String[] randoms = StringGenerateUtil.randomArray(1000000, 5, 0.8f);
        System.out.printf("Data: %s\n", sizeOf(randoms));
        Arrays.parallelSort(randoms);
        Recorder t = new Recorder();
        if ((flag & 1) > 0) {
            Set<String> set = Set.of(randoms);
            t.multi(randoms, set::contains);
            System.out.printf("SetN: %dms | %s\n", t.sum(), sizeOf(set));
            t.reset();
        }
        if ((flag & 2) > 0) {
            SimpleFSA fsa = new SimpleFSA(randoms);
            t.multi(randoms, fsa::contains);
            System.out.printf("FSA: %dms | %s\n", t.sum(), sizeOf(fsa));
        }
        if ((flag & 4) > 0) {
            SuccinctSet bss2 = ByteSuccinctSet2.of(randoms);
            t.multi(randoms, bss2::contains);
            System.out.printf("ByteSuccinctSet2: %dms | %s\n", t.sum(), sizeOf(bss2));
            t.reset();
        }
        if ((flag & 8) > 0) {
            SuccinctSet bss3 = ByteSuccinctSet3.of(randoms);
            t.multi(randoms, bss3::contains);
            System.out.printf("ByteSuccinctSet3: %dms | %s\n", t.sum(), sizeOf(bss3));
            t.reset();
        }
        if ((flag & 16) > 0) {
            SuccinctSet css2 = CharSuccinctSet2.sortedOf(randoms);
            t.multi(randoms, css2::contains);
            System.out.printf("CharSuccinctSet2: %dms | %s\n", t.sum(), sizeOf(css2));
            t.reset();
        }
        if ((flag & 32) > 0) {
            SuccinctSet css3 = CharSuccinctSet3.sortedOf(randoms);
            t.multi(randoms, css3::contains);
            System.out.printf("CharSuccinctSet3: %dms | %s\n", t.sum(), sizeOf(css3));
            t.reset();
        }
    }
}