package org.example.succinct.benchmark;

import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.core.*;
import org.example.succinct.utils.Recorder;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.succinct.utils.RamUsageUtil.sizeOf;

public class SuccinctTrieTest {
    public static void main(String[] args) {
        containsTest();
    }

    public static void containsTest() {
        String[] randoms = StringGenerateUtil.randomArray(1000000,  2, 14, 0.0f);
        Arrays.parallelSort(randoms);
        // PatriciaTrie pTrie = new PatriciaTrie();
        // for (String random : randoms) {
        //     pTrie.insert(random);
        // }
        // InlinedTailLOUDSTrie trie = new InlinedTailLOUDSTrie(pTrie);
        SuccinctTrie bss = ByteSuccinctTrie.of(randoms);
        SuccinctTrie cst2 = CharSuccinctTrie2.sortedOf(randoms);
        SuccinctTrie css = CharSuccinctTrie.sortedOf(randoms);
        // SuccinctTrie nst = NestedSuccinctTrie.sortedOf(randoms, 4);
        SimpleFSA fsa = new SimpleFSA(randoms);
        Recorder t = new Recorder();
        System.out.printf("Data: %s\n", sizeOf(randoms));
        t.multi(randoms, bss::contains);
        System.out.printf("%s: %dms | %s\n", bss.getClass().getSimpleName(), t.sum(), sizeOf(bss));
        t.reset();
        t.multi(randoms, cst2::contains);
        System.out.printf("%s: %dms | %s\n", cst2.getClass().getSimpleName(), t.sum(), sizeOf(cst2));
        t.reset();
        t.multi(randoms, css::contains);
        System.out.printf("%s: %dms | %s\n", css.getClass().getSimpleName(), t.sum(), sizeOf(css));
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
    
    public static void getTest(int count) {
        String[] randoms = StringGenerateUtil.readArray("D:\\Hualiang\\Study\\JavaWeb\\succinct\\src\\main\\resources\\words.txt");
        CharSuccinctTrie cst = CharSuccinctTrie.of(randoms);
        CharSuccinctTrie2 cst2 = CharSuccinctTrie2.sortedOf(randoms);
        ByteSuccinctTrie bst = ByteSuccinctTrie.of(randoms);
        NestedSuccinctTrie nst = NestedSuccinctTrie.sortedOf(randoms, 2);
        int size = (int) bst.nodeCount();
        long t = Timer.now(); 
        for (int i = 0; i < size; i++) {
            cst2.get(i);
        }
        long t0 = Timer.now(); 
        for (int i = 0; i < size; i++) {
            bst.get(i);
        }
        long t1 = Timer.now();
        for (int i = 0; i < size; i++) {
            nst.get(i);
        }
        long t3 = Timer.now();
        for (int i = 0; i < size; i++) {
            cst.get(i);
        }
        long t4 = Timer.now();
        System.out.printf("CharSuccinctTrie2: %dms\n", Timer.ms(t, t0));
        System.out.printf("ByteSuccinctTrie: %dms\n", Timer.ms(t0, t1));
        System.out.printf("NestedSuccinctTrie: %dms\n", Timer.ms(t1, t3));
        System.out.printf("CharSuccinctTrie: %dms\n", Timer.ms(t3, t4));
    }

    public static void iteratorTest(int flag) throws IOException {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 32, 0.0f);
        System.out.printf("Data: %s\n", sizeOf(randoms));
        Arrays.parallelSort(randoms);
        if ((flag & 1) > 0) {
            Set<String> set = Arrays.stream(randoms).parallel().collect(Collectors.toSet());
            long now = Timer.now();
            set.forEach(s -> {
            });
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
    }

    public static void memoryTest() {
        String[] randoms = StringGenerateUtil.randomArray(1000000, 2, 14, 0.0f);
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
        System.out.printf("Data: %s\n", sizeOf(randoms));
        System.out.printf("%s: %dms | %s\n", fsa.getClass().getSimpleName(), Timer.ms(t0, t1), sizeOf(fsa));
        System.out.printf("%s: %dms | %s\n", bst.getClass().getSimpleName(), Timer.ms(t1, t2), sizeOf(bst));
        System.out.printf("%s: %dms | %s\n", cst2.getClass().getSimpleName(), Timer.ms(t2, t3), sizeOf(cst2));
        System.out.printf("%s: %dms | %s\n", cst.getClass().getSimpleName(), Timer.ms(t3, t4), sizeOf(cst));
    }
}
