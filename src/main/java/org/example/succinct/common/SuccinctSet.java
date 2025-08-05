package org.example.succinct.common;

public interface SuccinctSet {
    boolean contains(String key);

    String get(int nodeId);
}
