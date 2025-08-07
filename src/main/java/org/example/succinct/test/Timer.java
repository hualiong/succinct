package org.example.succinct.test;

import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unused")
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
    
    public long count() {
        return count;
    }

    public <T, R> R once(T param, Function<T, R> f) {
        long start = System.currentTimeMillis();
        R result = f.apply(param);
        times += System.currentTimeMillis() - start;
        count++;
        return result;
    }

    public <T> void once(T param, Consumer<T> f) {
        long start = System.currentTimeMillis();
        f.accept(param);
        times += System.currentTimeMillis() - start;
        count++;
    }

    public <T> void multi(T[] params, Consumer<T> f) {
        long start = System.currentTimeMillis();
        for (T t : params) {
            f.accept(t);
        }
        times += System.currentTimeMillis() - start;
        count += params.length;
    }

    public static <T, R> R oncePrinter(T param, Function<T, R> f) {
        long start = System.currentTimeMillis();
        R result = f.apply(param);
        System.out.printf("%dms\n", System.currentTimeMillis() - start);
        return result;
    }

    public static <T> void oncePrinter(T param, Consumer<T> f) {
        long start = System.currentTimeMillis();
        f.accept(param);
        System.out.printf("%dms\n", System.currentTimeMillis() - start);
    }
}
