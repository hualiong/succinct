package org.example.succinct.benchmark;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.example.succinct.common.SimpleFST;
import org.example.succinct.utils.Recorder;
import org.example.succinct.utils.StringGenerateUtil;
import static org.example.succinct.utils.RamUsageUtil.estimateSizeOf;

@SuppressWarnings("unused")
public class FSTTest {
    public static void main(String[] args) {
        FSTQueryTest();
    }

    public static void FSTQueryTest() {
        String[] array = StringGenerateUtil.randomArray(1000000, 5, 0.7f);
        Recorder t = new Recorder();
        Map<BytesRef, Long> map = new LinkedHashMap<>();
        for (String line : array) {
            String[] parts = line.split(" ");
            if (parts.length == 2) {
                map.put(new BytesRef(parts[0]), Long.parseLong(parts[1]));
            }
        }
        SimpleFST<Long> fst = new SimpleFST<>(map, PositiveIntOutputs.getSingleton());
        t.once(map, m -> {
            m.keySet().forEach(fst::get);
        });
        System.out.println(t.sum() + "ms | " + estimateSizeOf(fst));
    }
}
