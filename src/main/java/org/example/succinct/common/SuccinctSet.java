package org.example.succinct.common;

import org.apache.lucene.util.Accountable;

public interface SuccinctSet extends Accountable {
    boolean contains(String key);

    String get(int nodeId);
}
