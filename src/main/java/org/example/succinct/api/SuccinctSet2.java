package org.example.succinct.api;

import java.util.Iterator;

public abstract class SuccinctSet2 extends SuccinctSet {
    protected final byte[] labels;
    public final long size;

    public SuccinctSet2(byte[] labels, long size) {
        this.labels = labels;
        this.size = size;
    }

    public abstract Iterator<String> prefixesOf(String key);

    public Iterator<String> startsWith(String key) {
        throw new UnsupportedOperationException();
    }
}
