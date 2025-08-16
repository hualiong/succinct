package org.example.succinct.api;

public abstract class SuccinctSet2 extends SuccinctSet {
    protected final byte[] labels;
    public final long size;

    public SuccinctSet2(byte[] labels, long size) {
        this.labels = labels;
        this.size = size;
    }

    public boolean contains(String key) {
        throw new UnsupportedOperationException();
    }

    public int index(String key) {
        throw new UnsupportedOperationException();
    }

    public String get(int nodeId) {
        throw new UnsupportedOperationException();
    }
}
