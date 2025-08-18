package org.example.succinct.test;

import org.example.succinct.common.SimpleFSA;
import org.example.succinct.api.SuccinctSet;
import org.example.succinct.core.*;
import org.example.succinct.utils.Recorder;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;
import org.trie4j.louds.InlinedTailLOUDSTrie;
import org.trie4j.patricia.PatriciaTrie;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;

import java.util.Arrays;

public class SuccinctSetTimeTest {
    public static void main(String[] args) {
        containsTimeTest(1000000);
    }

    public static void containsTimeTest(int count) {
        String[] randoms = StringGenerateUtil.randomArray(count, 32, 0.0f);
        Arrays.parallelSort(randoms);
        // Set<String> set = Set.of(randoms);
        PatriciaTrie pTrie = new PatriciaTrie();
        for (String random : randoms) {
            pTrie.insert(random);
        }
        InlinedTailLOUDSTrie trie = new InlinedTailLOUDSTrie(pTrie);
        SuccinctSet bss3 = ByteSuccinctSet3.of(randoms);
        SuccinctSet bss4 = ByteSuccinctSet4.of(randoms);
        SuccinctSet css3 = CharSuccinctSet3.sortedOf(randoms);
        SuccinctSet css4 = CharSuccinctSet4.sortedOf(randoms);
        SimpleFSA fsa = new SimpleFSA(randoms);
        Recorder t = new Recorder();
        System.out.printf("Data: %s\n", sizeOf(randoms));
        // t.multi(randoms, set::contains);
        // System.out.printf("SetN: %dms | %s\n", t.sum(), sizeOf(set));
        // t.reset();
        t.multi(randoms, trie::contains);
        System.out.printf("%s: %dms | %s\n", trie.getClass().getSimpleName(), t.sum(), sizeOf(trie));
        t.reset();
        t.multi(randoms, bss3::contains);
        System.out.printf("%s: %dms | %s\n", bss3.getClass().getSimpleName(), t.sum(), sizeOf(bss3));
        t.reset();
        t.multi(randoms, bss4::contains);
        System.out.printf("%s: %dms | %s\n", bss4.getClass().getSimpleName(), t.sum(), sizeOf(bss4));
        t.reset();
        t.multi(randoms, css3::contains);
        System.out.printf("%s: %dms | %s\n", css3.getClass().getSimpleName(), t.sum(), sizeOf(css3));
        t.reset();
        t.multi(randoms, css4::contains);
        System.out.printf("%s: %dms | %s\n", css4.getClass().getSimpleName(), t.sum(), sizeOf(css4));
        t.reset();
        t.multi(randoms, fsa::contains);
        System.out.printf("%s: %dms | %s\n", fsa.getClass().getSimpleName(), t.sum(), sizeOf(fsa));
    }
    
    public static void getTimeTest(int count) {
        String[] randoms = StringGenerateUtil.randomArray(count, 32, 0.0f);
        ByteSuccinctSet4 bss4 = ByteSuccinctSet4.of(randoms);
        ByteSuccinctSet3 bss3 = ByteSuccinctSet3.of(randoms);
        CharSuccinctSet4 css4 = CharSuccinctSet4.of(randoms);
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
        for (int i = 0; i < size; i++) {
            css4.get(i);
        }
        long t4 = Timer.now();
        System.out.printf("ByteSuccinctSet4: %dms\n", Timer.ms(t0, t1));
        System.out.printf("ByteSuccinctSet3: %dms\n", Timer.ms(t2, t3));
        System.out.printf("CharSuccinctSet4: %dms\n", Timer.ms(t3, t4));
    }
}
