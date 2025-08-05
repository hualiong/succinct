package org.example.succinct.common;

import java.io.IOException;

import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FSTCompiler;
import org.apache.lucene.util.fst.NoOutputs;
import org.apache.lucene.util.fst.Util;

public class SimpleFSA implements Accountable {
    private final FST<Object> fst;

    public SimpleFSA(String[] input) {
        NoOutputs outputs = NoOutputs.getSingleton();
        FSTCompiler<Object> compiler = new FSTCompiler.Builder<>(FST.INPUT_TYPE.BYTE1, outputs).build();
        try {
            IntsRefBuilder scratchInts = new IntsRefBuilder();
            for (String str : input) {
                Util.toIntsRef(new BytesRef(str), scratchInts);
                compiler.add(scratchInts.get(), outputs.getNoOutput());
            }
            fst = FST.fromFSTReader(compiler.compile(), compiler.getFSTReader());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contains(String term) {
        try {
            return Util.get(fst, new BytesRef(term)) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long ramBytesUsed() {
        return fst.ramBytesUsed();
    }
}
