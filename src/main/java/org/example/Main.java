package org.example;

import org.apache.lucene.util.RamUsageEstimator;
import org.example.succinct.ByteSuccinctSet;
import org.example.succinct.ByteSuccinctSet2;
import org.example.succinct.ByteSuccinctSet3;
import org.example.succinct.CharSuccinctSet;
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
        queryTimeTest();
    }
    
    public static void encodeTimeTest() {
        String charset = "GB18030";
        char[] c = new char[1024];
        StringEncoder encoder = new StringEncoder(Charset.forName(charset));
        String[] str = new String[10000000];
        Arrays.fill(str, "我是中国人");
        Timer t1 = new Timer();
        Timer t11 = new Timer();
        Timer t2 = new Timer();
        Timer t3 = new Timer();
        Timer t4 = new Timer();
        t1.multi(String::toCharArray, str);
        t11.multi(s -> StringEncoder.getChars(s, c), str);
        t2.multi(String::getBytes, str);
        t3.multi(encoder::encodeToBuffer, str);
        t4.multi(s -> s.getBytes(Charset.forName(charset)), str);
        System.out.println("CHAR: " + t1.sum() + "ms");
        System.out.println("CHAR(getChars): " + t11.sum() + "ms");
        System.out.println("UTF-8: " + t2.sum() + "ms");
        System.out.println(charset + "(encoder): " + t3.sum() + "ms");
        System.out.println(charset + ": " + t4.sum() + "ms");
    }

    public static void queryTimeTest() {
        int count = 2000000;
        String[] randoms = StringGenerateUtil.randomArray(count, 8, 0.0f);
        Set<String> set = Set.of(randoms);
        SuccinctSet bss3 = ByteSuccinctSet3.of(randoms);
        SuccinctSet bss2 = ByteSuccinctSet2.of(randoms);
        SuccinctSet css3 = CharSuccinctSet3.of(randoms);
        SuccinctSet css2 = CharSuccinctSet2.of(randoms);
        SimpleFSA fsa = new SimpleFSA(randoms);
        Timer t = new Timer();
        System.out.printf("Data: %s\n", extractSizeOf(randoms));
        t.multi(set::contains, randoms);
        System.out.printf("SetN: %dms | %s\n", t.sum(), extractSizeOf(set));
        t.reset();
        t.multi(bss3::contains, randoms);
        System.out.printf("ByteSuccinctSet3: %dms | %s\n", t.sum(), extractSizeOf(bss3));
        t.reset();
        t.multi(bss2::contains, randoms);
        System.out.printf("ByteSuccinctSet2: %dms | %s\n", t.sum(), extractSizeOf(bss2));
        t.reset();
        t.multi(css3::contains, randoms);
        System.out.printf("CharSuccinctSet3: %dms | %s\n", t.sum(), extractSizeOf(css3));
        t.reset();
        t.multi(css2::contains, randoms);
        System.out.printf("CharSuccinctSet2: %dms | %s\n", t.sum(), extractSizeOf(css2));
        t.reset();
        t.multi(fsa::contains, randoms);
        System.out.printf("FSA: %dms | %s\n", t.sum(), extractSizeOf(fsa));
    }

    public static String extractSizeOf(Object o) {
        return RamUsageEstimator.humanReadableUnits(GraphLayout.parseInstance(o).totalSize());
    }
}