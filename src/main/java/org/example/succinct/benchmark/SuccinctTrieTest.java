package org.example.succinct.benchmark;

import org.example.succinct.api.SuccinctTrie;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.core.ByteSuccinctTrie;
import org.example.succinct.core.CharSuccinctTrie;
import org.example.succinct.core.NestedSuccinctTrie;
import org.example.succinct.utils.Recorder;
import org.example.succinct.utils.StringGenerateUtil;
import org.example.succinct.utils.Timer;
import org.trie4j.louds.InlinedTailLOUDSTrie;
import org.trie4j.patricia.PatriciaTrie;

import java.util.Arrays;

import static org.example.succinct.utils.RamUsageUtil.printSizeOf;
import static org.example.succinct.utils.RamUsageUtil.sizeOf;

public class SuccinctTrieTest {
    public static void main(String[] args) {
        getTest(100000);
    }

    public static void containsTest(int count) {
        String[] randoms = StringGenerateUtil.randomArray(count, 32, 0.0f);
        Arrays.parallelSort(randoms);
        PatriciaTrie pTrie = new PatriciaTrie();
        for (String random : randoms) {
            pTrie.insert(random);
        }
        InlinedTailLOUDSTrie trie = new InlinedTailLOUDSTrie(pTrie);
        SuccinctTrie bss4 = ByteSuccinctTrie.of(randoms);
        SuccinctTrie css4 = CharSuccinctTrie.sortedOf(randoms);
        SuccinctTrie nst = NestedSuccinctTrie.sortedOf(randoms);
        SimpleFSA fsa = new SimpleFSA(randoms);
        Recorder t = new Recorder();
        System.out.printf("Data: %s\n", sizeOf(randoms));
        t.multi(randoms, trie::contains);
        System.out.printf("%s: %dms | %s\n", trie.getClass().getSimpleName(), t.sum(), sizeOf(trie));
        t.reset();
        t.multi(randoms, bss4::contains);
        System.out.printf("%s: %dms | %s\n", bss4.getClass().getSimpleName(), t.sum(), sizeOf(bss4));
        t.reset();
        t.multi(randoms, css4::contains);
        System.out.printf("%s: %dms | %s\n", css4.getClass().getSimpleName(), t.sum(), sizeOf(css4));
        t.reset();
        t.multi(randoms, nst::contains);
        System.out.printf("%s: %dms | %s\n", nst.getClass().getSimpleName(), t.sum(), sizeOf(nst));
        t.reset();
        t.multi(randoms, fsa::contains);
        System.out.printf("%s: %dms | %s\n", fsa.getClass().getSimpleName(), t.sum(), sizeOf(fsa));
    }
    
    public static void getTest(int count) {
        String[] randoms = StringGenerateUtil.randomArray(count, 32, 0.0f);
        CharSuccinctTrie cst = CharSuccinctTrie.of(randoms);
        ByteSuccinctTrie bst = ByteSuccinctTrie.of(randoms);
        NestedSuccinctTrie nst = NestedSuccinctTrie.sortedOf(randoms);
        int size = (int) bst.nodeCount();
        for (int i = 0; i < size; i++) {
            bst.get(i);
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
        System.out.printf("ByteSuccinctTrie: %dms\n", Timer.ms(t0, t1));
        System.out.printf("NestedSuccinctTrie: %dms\n", Timer.ms(t1, t3));
        System.out.printf("CharSuccinctTrie: %dms\n", Timer.ms(t3, t4));
    }

    public static void memoryTest() {
        String[] largeRandoms = StringGenerateUtil.randomArray(1000000, 8, 0.0f);
        Arrays.parallelSort(largeRandoms);
        PatriciaTrie pTrie = new PatriciaTrie();
        for (String random : largeRandoms) {
            pTrie.insert(random);
        }
        InlinedTailLOUDSTrie trie = new InlinedTailLOUDSTrie(pTrie);
        ByteSuccinctTrie bss = ByteSuccinctTrie.of(largeRandoms);
        CharSuccinctTrie css = CharSuccinctTrie.sortedOf(largeRandoms);
        SimpleFSA fsa = new SimpleFSA(largeRandoms);
        printSizeOf(largeRandoms);
        printSizeOf(trie);
        printSizeOf(bss);
        printSizeOf(css);
        printSizeOf(fsa);
    }
}
