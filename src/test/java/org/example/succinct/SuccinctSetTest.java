package org.example.succinct;

import org.example.succinct.api.SuccinctSet;
import org.example.succinct.core.*;
import org.example.succinct.utils.StringGenerateUtil;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SuccinctSetTest {
    private static final int COUNT = 20000;
    private String[] randoms;
    private SuccinctSet set;

    @Before
    public void setUp() {
        Function<String[], SuccinctSet> init = CharSuccinctSet::of;
        randoms = StringGenerateUtil.randomArray(COUNT, 10, 0.5f);

        set = init.apply(randoms);
    }

    @Test
    public void indexAndGetTest() {
        Set<String> unique = Arrays.stream(randoms).collect(Collectors.toSet());
        int[] array = new int[set.nodeCount()];
        Arrays.setAll(array, i -> i);
        for (String random : randoms) {
            int index = set.index(random);
            assertEquals(unique.contains(random), index > 0);
            assertEquals(random, set.get(index));
            array[index] = -1;
        }
        Arrays.stream(array).filter(i -> i > 0).forEach(i -> assertNull(set.get(i)));
    }
}
