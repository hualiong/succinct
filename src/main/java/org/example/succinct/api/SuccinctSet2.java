package org.example.succinct.api;

public abstract class SuccinctSet2 extends SuccinctSet {
    protected final byte[] labels;
    public final long size;

    public SuccinctSet2(byte[] labels, long size) {
        this.labels = labels;
        this.size = size;
    }

    public abstract Iterable<String> prefixesOf(String key);

    public abstract Iterable<String> startsWith(String key);
}
