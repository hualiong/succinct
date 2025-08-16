package org.example.succinct.test;

import org.example.succinct.common.SimpleFSA;
import org.example.succinct.api.SuccinctSet;
import org.example.succinct.core.*;
import org.example.succinct.utils.Recorder;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;

import java.util.Arrays;
import java.util.Set;

public class SuccinctSetTimeTest {
    public static void main(String[] args) {
        getTimeTest(1000000);
    }

    public static void containsTimeTest() {
        int count = 2000000;
        String[] randoms = StringGenerateUtil.randomArray(count, 5, 0.7f);
        String[] copyOf = Arrays.copyOf(randoms, count >> 1);
        Set<String> set = Set.of(copyOf);
        SuccinctSet bss3 = ByteSuccinctSet3.of(copyOf);
        SuccinctSet bss2 = ByteSuccinctSet2.of(copyOf);
        SuccinctSet css3 = CharSuccinctSet3.of(copyOf);
        SuccinctSet css2 = CharSuccinctSet2.of(copyOf);
        SimpleFSA fsa = new SimpleFSA(copyOf);
        Recorder t = new Recorder();
        System.out.printf("Data: %s\n", sizeOf(randoms));
        t.multi(randoms, set::contains);
        System.out.printf("SetN: %dms | %s\n", t.sum(), sizeOf(set));
        t.reset();
        t.multi(randoms, bss3::contains);
        System.out.printf("ByteSuccinctSet3: %dms | %s\n", t.sum(), sizeOf(bss3));
        t.reset();
        t.multi(randoms, bss2::contains);
        System.out.printf("ByteSuccinctSet2: %dms | %s\n", t.sum(), sizeOf(bss2));
        t.reset();
        t.multi(randoms, css3::contains);
        System.out.printf("CharSuccinctSet3: %dms | %s\n", t.sum(), sizeOf(css3));
        t.reset();
        t.multi(randoms, css2::contains);
        System.out.printf("CharSuccinctSet2: %dms | %s\n", t.sum(), sizeOf(css2));
        t.reset();
        t.multi(randoms, fsa::contains);
        System.out.printf("FSA: %dms | %s\n", t.sum(), sizeOf(fsa));
    }
    
    public static void getTimeTest(int s) {
        String[] randoms = StringGenerateUtil.randomArray(s, 5, 1.0f);
        ByteSuccinctSet4 bss4 = ByteSuccinctSet4.of(randoms);
        ByteSuccinctSet3 bss3 = ByteSuccinctSet3.of(randoms);
        int size = (int) bss4.labelBitmap().oneCount();
        for (int i = 0; i < size; i++) {
            bss4.get(i);
        }
        long t0 = Timer.now(); 
        for (int i = 0; i < size; i++) {
            bss4.get(i);
        }
        long t1 = Timer.now(); 
        for (int i = 0; i < size; i++) {
            bss3.get(i);
        }
        long t2 = Timer.now(); 
        for (int i = 0; i < size; i++) {
            bss3.get(i);
        }
        long t3 = Timer.now();
        System.out.printf("ByteSuccinctSet4: %dms\n", Timer.ms(t0, t1));
        System.out.printf("ByteSuccinctSet3: %dms\n", Timer.ms(t2, t3));
    }

}
