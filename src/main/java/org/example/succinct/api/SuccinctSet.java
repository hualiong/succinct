package org.example.succinct.api;

public interface SuccinctSet {
    boolean contains(String key);

    String get(int nodeId);
}
