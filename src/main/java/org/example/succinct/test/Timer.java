package org.example.succinct.test;

@SuppressWarnings("unused")
public class Timer {
    public static long now() {
        return System.currentTimeMillis();
    }

    public static double s(long start, long end) {
        return (end - start) / 1000.0;
    }

    public static long ms(long start) {
        return now() - start;
    }

    public static long ms(long start, long end) {
        return end - start;
    }
}
