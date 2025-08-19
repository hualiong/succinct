package org.example.succinct.api;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface SuccinctTrie extends SuccinctSet {
    Iterator<String> iterator(boolean orderly);

    Iterator<String> prefixKeysOf(String str);

    default Iterator<String> prefixSearch(String prefix) {
        throw new UnsupportedOperationException();
    }

    abstract class TermIterator implements Iterator<String> {
        protected String next;

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
