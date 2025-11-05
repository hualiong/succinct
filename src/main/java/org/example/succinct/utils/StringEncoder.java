package org.example.succinct.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

public class StringEncoder {
    private final CharsetEncoder encoder;
    private ByteBuffer byteBuffer;
    private CharBuffer charBuffer;

    public StringEncoder(Charset charset) {
        this.encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        // 初始分配1024字节缓冲区
        this.byteBuffer = ByteBuffer.allocate(512);
        this.charBuffer = CharBuffer.allocate(256);
    }

    public Charset charset() {
        return encoder.charset();
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

    /**
     * 提取字符到外部提供的缓冲区
     * @return 实际复制的字符数
     */
    public static int getChars(String str, char[] dest) {
        int length = Math.min(str.length(), dest.length);
        if (length == 0){
            return 0;
        }
        str.getChars(0, length, dest, 0);
        return length;
    }

    /**
     * 将字符串编码为字节数组（创建新数组）
     */
    public byte[] getBytes(String str) {
        encodeToBuffer(str);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    /**
     * 将字符编码为字节数组（创建新数组）
     */
    public byte[] getBytes(char ch) {
        // 优化：ASCII 字符（0-127）直接返回单字节
        if (ch <= 127 && encoder.canEncode(ch)) {
            return new byte[] { (byte) ch };
        }
        ByteBuffer buffer = encodeToBuffer(ch);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * 将单个 char 编码到复用的 ByteBuffer（零拷贝）
     * 注意：返回的 ByteBuffer 内容会在下次编码时被覆盖
     */
    public ByteBuffer encodeToBuffer(char ch) {
        // 复用 CharBuffer
        charBuffer.clear().put(ch).flip();

        // 复用 ByteBuffer
        byteBuffer.clear();

        // 执行编码
        encoder.reset();
        CoderResult result = encoder.encode(charBuffer, byteBuffer, true);
        assert result.isUnderflow() : "编码失败: " + result;
        result = encoder.flush(byteBuffer);
        assert result.isUnderflow() : "刷新失败: " + result;

        return byteBuffer.flip(); // 准备读取 
    }

    /**
     * 将字符串编码到复用的ByteBuffer（零拷贝）
     * 注意：返回的ByteBuffer内容会在下次编码时被覆盖
     */
    public ByteBuffer encodeToBuffer(String str) {
        // 确保字符缓冲区足够大
        if (charBuffer.capacity() < str.length()) {
            charBuffer = CharBuffer.allocate(str.length());
        }
        charBuffer.clear().put(str).flip();

        // 计算所需最大字节空间
        int maxBytes = (int) (str.length() * encoder.maxBytesPerChar());

        // 确保字节缓冲区足够大
        if (byteBuffer.capacity() < maxBytes) {
            byteBuffer = ByteBuffer.allocate(maxBytes);
        }
        byteBuffer.clear();

        // 执行编码
        encoder.reset();
        CoderResult result = encoder.encode(charBuffer, byteBuffer, true);
        assert result.isUnderflow() : "Encoding failed";
        result = encoder.flush(byteBuffer);
        assert result.isUnderflow() : "Encoding failed";

        return byteBuffer.flip(); // 准备读取
    }
}