package org.example.succinct;

import org.example.succinct.api.SuccinctSet;
import org.example.succinct.core.*;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SuccinctSetTest {
    private static final int COUNT = 20000;
    private String[] randoms;
    private SuccinctSet set;

    @Before
    public void setUp() {
        // 初始化测试数据
        randoms = StringGenerateUtil.randomArray(COUNT, 10, 0.5f);
        Arrays.parallelSort(randoms);

        // 初始化测试实例
        set = CharSuccinctSet.sortedOf(randoms);
    }

    @Test
    public void indexTest() {
        Set<String> unique = Arrays.stream(randoms).collect(Collectors.toSet());
        for (String random : randoms) {
            int index = set.index(random);
            assertEquals(unique.contains(random), index > 0);
            assertEquals(random, set.get(index));
        }
    }

    @Test
    public void getTest() {
        CharSuccinctSet expected = CharSuccinctSet.sortedOf(randoms);
        long size = expected.labelBitmap().oneCount();
        for (int i = 0; i < size; i++) {
            assertEquals(expected.get(i), set.get(i));
        }
    }
}
