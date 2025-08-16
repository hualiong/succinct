package org.example.succinct;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.example.succinct.common.SimpleFSA;
import org.example.succinct.common.SimpleFST;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Test;

import static org.example.succinct.utils.RamUsageUtil.estimateSizeOf;
import static org.example.succinct.utils.RamUsageUtil.sizeOf;

public class FSTTests {
    @Test
    public void FSAMemoryTest() {
        String[] array = StringGenerateUtil.randomArray(1000000, 5, 0.7f);
        // System.out.println(sizeOf(array));
        SimpleFSA fsa = new SimpleFSA(array);
        System.out.println(sizeOf(fsa));
        System.out.println(estimateSizeOf(fsa));
    }

    @Test
    public void FSTMemoryTest() {
        String[] array = StringGenerateUtil.randomArray(1000000, 5, 0.7f);
        // System.out.println(sizeOf(array));
        Map<BytesRef, Long> map = new LinkedHashMap<>();
        for (String line : array) {
            String[] parts = line.split(" ");
            if (parts.length == 2) {
                map.put(new BytesRef(parts[0]), Long.parseLong(parts[1]));
            }
        }
        SimpleFST<Long> fst = new SimpleFST<>(map, PositiveIntOutputs.getSingleton());
        System.out.println(sizeOf(fst));
        System.out.println(estimateSizeOf(fst));
    }

    @Test
    public void testFST() throws IOException {
        // 1. 准备有序输入数据（必须按字典序排序！）
        Map<BytesRef, Long> input = Map.of(
                new BytesRef("cat"), 10L,
                new BytesRef("dog"), 20L,
                new BytesRef("dogs"), 30L,
                new BytesRef("dot"), 40L
        );

        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        SimpleFST<Long> fst = new SimpleFST<>(input, outputs);

        // 4. 使用 FST 查询
        System.out.println("FST 查询结果:");
        System.out.println(fst.get("cat")); // 输出: cat -> 10
        System.out.println(fst.get("dog")); // 输出: dog -> 20
        System.out.println(fst.get("dogs")); // 输出: dogs -> 30
        System.out.println(fst.get("apple")); // 输出: apple -> 未找到
        System.out.println(fst.get("dot")); // 输出: dot -> 40

        // 5. 保存到文件（可选）
        Path path = Paths.get(System.getProperty("user.home"), "Desktop", "data", "fst.bin");
        fst.save(path);
        System.out.println("FST已保存到: " + path + " (" + path.toFile().length() + " 字节)");

        // 6. 验证文件加载
        SimpleFST<Long> loadedFst = new SimpleFST<>(FST.read(path, outputs));
        System.out.println("加载的FST查询结果:");
        System.out.println(loadedFst.get("dogs")); // 输出: dogs -> 30
    }
}
