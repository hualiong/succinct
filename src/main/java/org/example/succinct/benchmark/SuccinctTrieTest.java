package org.example.succinct.benchmark;

import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.common.CompactRadixTree;
import org.example.succinct.core.*;
import org.example.succinct.utils.Recorder;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;
import java.io.IOException;
import java.util.Arrays;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;

public class SuccinctTrieTest {
    public static void main(String[] args) throws IOException {
        // iteratorTest(63);
        // containsTest();
        memoryTest();
    }

    public static void containsTest() {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 2, 14, 1.0f);
        Arrays.parallelSort(randoms);
        // PatriciaTrie pTrie = new PatriciaTrie();
        // for (String random : randoms) {
        //     pTrie.insert(random);
        // }
        // InlinedTailLOUDSTrie trie = new InlinedTailLOUDSTrie(pTrie);
        SuccinctTrie srt = new CompactRadixTree(randoms);
        SuccinctTrie bst2 = ByteSuccinctTrie2.of(randoms);
        SuccinctTrie bst = ByteSuccinctTrie.of(randoms);
        SuccinctTrie cst2 = CharSuccinctTrie2.sortedOf(randoms);
        SuccinctTrie cst = CharSuccinctTrie.sortedOf(randoms);
        // SuccinctTrie nst = NestedSuccinctTrie.sortedOf(randoms, 4);
        SimpleFSA fsa = new SimpleFSA(randoms);
        Recorder t = new Recorder();
        System.out.printf("Data: %s\n", sizeOf(randoms));
        t.multi(randoms, srt::contains);
        System.out.printf("%s: %dms | %s\n", srt.getClass().getSimpleName(), t.sum(), sizeOf(srt));
        t.reset();
        t.multi(randoms, bst2::contains);
        System.out.printf("%s: %dms | %s\n", bst2.getClass().getSimpleName(), t.sum(), sizeOf(bst2));
        t.reset();
        t.multi(randoms, bst::contains);
        System.out.printf("%s: %dms | %s\n", bst.getClass().getSimpleName(), t.sum(), sizeOf(bst));
        t.reset();
        t.multi(randoms, cst2::contains);
        System.out.printf("%s: %dms | %s\n", cst2.getClass().getSimpleName(), t.sum(), sizeOf(cst2));
        t.reset();
        t.multi(randoms, cst::contains);
        System.out.printf("%s: %dms | %s\n", cst.getClass().getSimpleName(), t.sum(), sizeOf(cst));
        // t.reset();
        // t.multi(randoms, nst::contains);
        // System.out.printf("%s: %dms | %s\n", nst.getClass().getSimpleName(), t.sum(), sizeOf(nst));
        // t.reset();
        // t.multi(randoms, trie::contains);
        // System.out.printf("%s: %dms | %s\n", trie.getClass().getSimpleName(), t.sum(), sizeOf(trie));
        t.reset();
        t.multi(randoms, fsa::contains);
        System.out.printf("%s: %dms | %s\n", fsa.getClass().getSimpleName(), t.sum(), sizeOf(fsa));
    }
    
    public static void getTest() {
        String[] randoms = StringGenerateUtil.randomArray(100000, 10, 1.0f);
        // String[] randoms = StringGenerateUtil.readArray();
        SuccinctTrie cst = CharSuccinctTrie.of(randoms);
        SuccinctTrie cst2 = CharSuccinctTrie2.sortedOf(randoms);
        SuccinctTrie bst = ByteSuccinctTrie.of(randoms);
        SuccinctTrie bst2 = ByteSuccinctTrie2.of(randoms);
        // NestedSuccinctTrie nst = NestedSuccinctTrie.sortedOf(randoms, 2);
        int size = (int) bst.nodeCount();
        long t = Timer.now(); 
        for (int i = 0; i < size; i++) {
            cst2.get(i);
        }
        long t0 = Timer.now(); 
        for (int i = 0; i < size; i++) {
            cst.get(i);
        }
        long t1 = Timer.now();
        for (int i = 0; i < size; i++) {
            bst2.get(i);
        }
        long t3 = Timer.now();
        for (int i = 0; i < size; i++) {
            bst.get(i);
        }
        long t4 = Timer.now();
        System.out.printf(cst2.getClass().getSimpleName() + ": %dms\n", Timer.ms(t, t0));
        System.out.printf(cst.getClass().getSimpleName() + ": %dms\n", Timer.ms(t0, t1));
        System.out.printf(bst2.getClass().getSimpleName() + ": %dms\n", Timer.ms(t1, t3));
        System.out.printf(bst.getClass().getSimpleName() + ": %dms\n", Timer.ms(t3, t4));
    }

    public static void iteratorTest(int flag) throws IOException {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 0, 8, 0.5f);
        System.out.printf("Data: %s\n", sizeOf(randoms));
        Arrays.parallelSort(randoms);
        if ((flag & 1) > 0) {
            SuccinctTrie srt = new CompactRadixTree(randoms);
            long now = Timer.now();
            srt.iterator(true).forEachRemaining(s -> {
            });
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", srt.getClass().getSimpleName(), ms, sizeOf(srt));
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
            SuccinctTrie cst2 = CharSuccinctTrie2.sortedOf(randoms);
            long now = Timer.now();
            cst2.iterator(true).forEachRemaining(s -> {
            });
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", cst2.getClass().getSimpleName(), ms, sizeOf(cst2));
        }
        if ((flag & 8) > 0) {
            SuccinctTrie cst = CharSuccinctTrie.of(randoms);
            long now = Timer.now();
            cst.iterator(true).forEachRemaining(s -> {
            });
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", cst.getClass().getSimpleName(), ms, sizeOf(cst));
        }
        if ((flag & 16) > 0) {
            SuccinctTrie bst = ByteSuccinctTrie.of(randoms);
            long now = Timer.now();
            bst.iterator(true).forEachRemaining(s -> {
            });
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", bst.getClass().getSimpleName(), ms, sizeOf(bst));
        }
        if ((flag & 32) > 0) {
            SuccinctTrie bst = ByteSuccinctTrie2.of(randoms);
            long now = Timer.now();
            bst.iterator(true).forEachRemaining(s -> {
            });
            long ms = Timer.ms(now);
            System.out.printf("%s: %dms | %s\n", bst.getClass().getSimpleName(), ms, sizeOf(bst));
        }
    }

    public static void memoryTest() {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 0, 8, 0.5f);
        Arrays.parallelSort(randoms);
        long t0 = Timer.now();
        SimpleFSA fsa = new SimpleFSA(randoms);
        long t1 = Timer.now();
        SuccinctTrie bst = ByteSuccinctTrie.of(randoms);
        long t2 = Timer.now();
        SuccinctTrie cst2 = CharSuccinctTrie2.sortedOf(randoms);
        long t3 = Timer.now();
        SuccinctTrie cst = CharSuccinctTrie.sortedOf(randoms);
        long t4 = Timer.now();
        SuccinctTrie srt = new CompactRadixTree(randoms);
        long t5 = Timer.now();
        System.out.printf("Data: %s\n", sizeOf(randoms));
        System.out.printf("%s: %dms | %s\n", fsa.getClass().getSimpleName(), Timer.ms(t0, t1), sizeOf(fsa));
        System.out.printf("%s: %dms | %s\n", bst.getClass().getSimpleName(), Timer.ms(t1, t2), sizeOf(bst));
        System.out.printf("%s: %dms | %s\n", cst2.getClass().getSimpleName(), Timer.ms(t2, t3), sizeOf(cst2));
        System.out.printf("%s: %dms | %s\n", cst.getClass().getSimpleName(), Timer.ms(t3, t4), sizeOf(cst));
        System.out.printf("%s: %dms | %s\n", srt.getClass().getSimpleName(), Timer.ms(t4, t5), sizeOf(srt));
    }
}
