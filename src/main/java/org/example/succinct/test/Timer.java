package org.example.succinct.test;

import java.util.function.Consumer;
import java.util.function.Function;

public class Timer {
    private long times = 0L;
    private long count = 0L;

    public void reset() {
        times = 0L;
        count = 0L;
    }

    public long sum() {
        return times;
    }

    public double avg() {
        return 1.0 * times / count;
    }
    
    public double count() {
        return count;
    }

    public <T, R> R once(Function<T, R> f, T param) {
        long start = System.currentTimeMillis();
        R result = f.apply(param);
        times += System.currentTimeMillis() - start;
        count++;
        return result;
    }

    public <T> void once(Consumer<T> f, T param) {
        long start = System.currentTimeMillis();
        f.accept(param);
        times += System.currentTimeMillis() - start;
        count++;
    }

    public <T> void multi(Consumer<T> f, T[] params) {
        long start = System.currentTimeMillis();
        for (T t : params) {
            f.accept(t);
        }
        times += System.currentTimeMillis() - start;
        count += params.length;
    }
    
    public void multi(Consumer<Integer> f, Integer[] params) {
        long start = System.currentTimeMillis();
        for (Integer t : params) {
            f.accept(t);
        }
        times += System.currentTimeMillis() - start;
        count += params.length;
    }
    
    public void multi(Consumer<Long> f, Long[] params) {
        long start = System.currentTimeMillis();
        for (Long t : params) {
            f.accept(t);
        }
        times += System.currentTimeMillis() - start;
        count += params.length;
    }

    public static <T, R> R oncePrinter(Function<T, R> f, T param) {
        long start = System.currentTimeMillis();
        R result = f.apply(param);
        System.out.printf("%dms\n", System.currentTimeMillis() - start);
        return result;
    }

    public static <T> void oncePrinter(Consumer<T> f, T param) {
        long start = System.currentTimeMillis();
        f.accept(param);
        System.out.printf("%dms\n", System.currentTimeMillis() - start);
    }
}
