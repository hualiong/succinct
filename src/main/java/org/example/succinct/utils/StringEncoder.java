package org.example.succinct.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class StringEncoder {
    private final CharsetEncoder encoder;
    private ByteBuffer byteBuffer;
    private CharBuffer charBuffer;

    public StringEncoder(Charset charset) {
        this.encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // 初始分配1024字节缓冲区
        this.byteBuffer = ByteBuffer.allocate(8);
        this.charBuffer = CharBuffer.allocate(16);
    }

    public Charset charset() {
        return encoder.charset();
    }
    
    public ByteBuffer byteBuffer() {
        return byteBuffer;
    }

    public int maxBytes(int length) {
        return (int) (length * encoder.maxBytesPerChar());
    }

    public static int getUTF8Bytes(String s, int off, int len, byte[] out, int outOff) {
        final int end = off + len;

        int upto = outOff;
        for (int i = off; i < end; i++) {
            final int code = (int) s.charAt(i);

            if (code < 0x80)
                out[upto++] = (byte) code;
            else if (code < 0x800) {
                out[upto++] = (byte) (0xC0 | (code >> 6));
                out[upto++] = (byte) (0x80 | (code & 0x3F));
            } else if (code < 0xD800 || code > 0xDFFF) {
                out[upto++] = (byte) (0xE0 | (code >> 12));
                out[upto++] = (byte) (0x80 | ((code >> 6) & 0x3F));
                out[upto++] = (byte) (0x80 | (code & 0x3F));
            } else {
                // surrogate pair
                // confirm valid high surrogate
                if (code < 0xDC00 && (i < end - 1)) {
                    int utf32 = (int) s.charAt(i + 1);
                    // confirm valid low surrogate and write pair
                    if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) {
                        utf32 = (code << 10) + utf32 - 56613888;
                        i++;
                        out[upto++] = (byte) (0xF0 | (utf32 >> 18));
                        out[upto++] = (byte) (0x80 | ((utf32 >> 12) & 0x3F));
                        out[upto++] = (byte) (0x80 | ((utf32 >> 6) & 0x3F));
                        out[upto++] = (byte) (0x80 | (utf32 & 0x3F));
                        continue;
                    }
                }
                // replace unpaired surrogate or out-of-order low surrogate
                // with substitution character
                out[upto++] = (byte) 0xEF;
                out[upto++] = (byte) 0xBF;
                out[upto++] = (byte) 0xBD;
            }
        }
        // assert matches(s, offset, length, out, upto);
        return upto;
    }

    public static int getChars(String str, char[] dest) {
        int length = Math.min(str.length(), dest.length);
        if (length == 0){
            return 0;
        }
        str.getChars(0, length, dest, 0);
        return length;
    }

    public byte[] getBytesSafely(String str) {
        checkCapacity(str);
        return getBytes(str);
    }
    
    public byte[] getBytes(String str) {
        encodeToBuffer(str);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    public ByteBuffer encodeToBufferSafely(String str) {
        checkCapacity(str);
        return encodeToBuffer(str);
    }
    
    public ByteBuffer encodeToBuffer(String str) {
        charBuffer.clear().put(str).flip();
        byteBuffer.clear();

        // 执行编码
        encoder.reset();
        CoderResult result = encoder.encode(charBuffer, byteBuffer, true);
        assert result.isUnderflow() : "Encoding failed";
        result = encoder.flush(byteBuffer);
        assert result.isUnderflow() : "Encoding failed";

        return byteBuffer.flip(); // 准备读取
    }

    public void checkCapacity(String str) {
        if (charBuffer.capacity() < str.length()) {
            charBuffer = CharBuffer.allocate(str.length());
        }
        int maxBytes = maxBytes(str.length());
        if (byteBuffer.capacity() < maxBytes) {
            byteBuffer = ByteBuffer.allocate(maxBytes);
        }
    }

    public boolean checkByteCapacity(String str) {
        int maxBytes = maxBytes(str.length());
        return byteBuffer.capacity() >= maxBytes;
    }
}