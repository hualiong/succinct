package org.example.succinct.api;

public interface SuccinctSet {
    int size();

    int nodeCount();

    default boolean contains(String key) {
        throw new UnsupportedOperationException();
    }

    default int index(String key) {
        throw new UnsupportedOperationException();
    }

    default String get(int nodeId) {
        throw new UnsupportedOperationException();
    }
}
