package org.example.succinct.api;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface SuccinctTrie {
    int size();

    int nodeCount();

    boolean contains(String key);

    int index(String key);

    String get(int nodeId);

    Iterator<String> iterator(boolean orderly);

    Iterator<String> prefixKeysOf(String str);

    Iterator<String> prefixSearch(String prefix);

    abstract class TermIterator implements Iterator<String> {
        protected String next = "";

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public String next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            String term = next;
            advance();
            return term;
        }

        protected abstract void advance();
    }
}
