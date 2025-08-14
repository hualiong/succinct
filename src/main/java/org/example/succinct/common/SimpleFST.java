package org.example.succinct.common;

import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.BytesRefFSTEnum;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FSTCompiler;
import org.apache.lucene.util.fst.Outputs;
import org.apache.lucene.util.fst.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class SimpleFST<T> implements Accountable {
    private final FST<T> fst;

    public SimpleFST(FST<T> fst) {
        this.fst = fst;
    }

    public SimpleFST(Map<BytesRef, T> input, Outputs<T> outputs) {
        FSTCompiler<T> compiler = new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, outputs).build();
        try {
            IntsRefBuilder scratchInts = new IntsRefBuilder();
            for (Map.Entry<BytesRef, T> entry : input.entrySet()) {
                Util.toIntsRef(entry.getKey(), scratchInts);
                compiler.add(scratchInts.get(), entry.getValue());
            }
            fst = FST.fromFSTReader(compiler.compile(), compiler.getFSTReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contains(String term) {
        return get(term) != null;
    }

    public T get(String term) {
        return get(new BytesRef(term));
    }

    public T get(BytesRef term) {
        try {
            return Util.get(fst, term);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BytesRefFSTEnum<T> iterator() {
        return new BytesRefFSTEnum<>(fst);
    }

    public void save(Path path) {
        try {
            fst.save(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long ramBytesUsed() {
        return fst.ramBytesUsed();
    }
}
