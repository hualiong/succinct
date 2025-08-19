package org.example.succinct.api;

import java.util.Iterator;

public abstract class SuccinctSet2 extends SuccinctSet {
    protected final byte[] labels;
    public final long size;

    public SuccinctSet2(byte[] labels, long size) {
        this.labels = labels;
        this.size = size;
    }

    public abstract Iterator<String> iterator(boolean orderly);

    public abstract Iterator<String> prefixKeysOf(String str);

    public Iterator<String> prefixSearch(String prefix) {
        throw new UnsupportedOperationException();
    }
}
