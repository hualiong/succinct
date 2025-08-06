package org.example.succinct;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FSTCompiler;
import org.apache.lucene.util.fst.NoOutputs;
import org.apache.lucene.util.fst.Outputs;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Test;

import static org.example.succinct.SuccinctSetTests.extractSizeOf;

public class FSTTests {
    @Test
    public void memoryTest() throws IOException {
        String[] array = StringGenerateUtil.readArray("C:\\Users\\huazhaoming\\Desktop\\100w_en_kv.txt");
        Map<BytesRef, Long> map = new HashMap<>();
        for (String line : array) {
            String[] parts = line.split(" ");
            if (parts.length == 2) {
                map.put(new BytesRef(parts[0]), Long.parseLong(parts[1]));
            }
        }
        System.out.println(extractSizeOf(createFst(map)));
    }

    @Test
    public void testFST() throws IOException {
        // 1. 准备有序输入数据（必须按字典序排序！）
        Map<BytesRef, Long> input = Map.of(
                new BytesRef("cat"), 10L,
                new BytesRef("dog"), 20L,
                new BytesRef("dogs"), 30L,
                new BytesRef("dot"), 40L);

        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        FST<Long> fst = createFst(input, outputs);

        // 4. 使用 FST 查询
        System.out.println("FST 查询结果:");
        queryFST(fst, "cat"); // 输出: cat -> 10
        queryFST(fst, "dog"); // 输出: dog -> 20
        queryFST(fst, "dogs"); // 输出: dogs -> 30
        queryFST(fst, "apple"); // 输出: apple -> 未找到
        queryFST(fst, "dot"); // 输出: dot -> 40

        // 5. 保存到文件（可选）
        Path path = Paths.get(System.getProperty("user.home"), "Desktop", "data", "fst.bin");
        fst.save(path);
        System.out.println("FST已保存到: " + path + " (" + path.toFile().length() + " 字节)");

        // 6. 验证文件加载
        FST<Long> loadedFst = FST.read(path, outputs);
        System.out.println("加载的FST查询结果:");
        queryFST(loadedFst, "dogs"); // 输出: dogs -> 30
    }

    private FST<Long> createFst(Map<BytesRef, Long> input, Outputs<Long> outputs) throws IOException {
        // 2. 创建输出类型和编译器
        FSTCompiler<Long> compiler = new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, outputs).build();

        // 3. 构建 FST
        IntsRefBuilder scratchInts = new IntsRefBuilder();
        for (Map.Entry<BytesRef, Long> entry : input.entrySet()) {
            // 将字节序列转换为IntsRef
            Util.toIntsRef(entry.getKey(), scratchInts);
            // 使用新的add方法
            compiler.add(scratchInts.get(), entry.getValue());
        }
        return FST.fromFSTReader(compiler.compile(), compiler.getFSTReader());
    }

    public static <T> T queryFST(FST<T> fst, String term) throws IOException {
        return Util.get(fst, new BytesRef(term));
    }

    public static FST<Long> createFst(Map<BytesRef, Long> input) {
        PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton();
        FSTCompiler<Long> compiler = new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, outputs).build();
        try {
            IntsRefBuilder scratchInts = new IntsRefBuilder();
            for (Map.Entry<BytesRef, Long> entry : input.entrySet()) {
                Util.toIntsRef(entry.getKey(), scratchInts);
                compiler.add(scratchInts.get(), entry.getValue());
            }
            return FST.fromFSTReader(compiler.compile(), compiler.getFSTReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FST<Object> createFsa(String[] input) {
        NoOutputs outputs = NoOutputs.getSingleton();
        FSTCompiler<Object> compiler = new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, outputs).build();
        try {
            IntsRefBuilder scratchInts = new IntsRefBuilder();
            for (String str : input) {
                Util.toIntsRef(new BytesRef(str), scratchInts);
                compiler.add(scratchInts.get(), outputs.getNoOutput());
            }
            return FST.fromFSTReader(compiler.compile(), compiler.getFSTReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
