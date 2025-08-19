package org.example.succinct.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
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
        this.byteBuffer = ByteBuffer.allocate(1024);
        this.charBuffer = CharBuffer.allocate(1024);
    }

    public Charset charset() {
        return encoder.charset();
    }

    /**
     * 提取字符到外部提供的缓冲区
     * @return 实际复制的字符数
     */
    public static int getChars(String str, char[] dest) {
        int length = Math.min(str.length(), dest.length);
        if (length == 0) return 0;
        
        str.getChars(0, length, dest, 0);
        return length;
    }

    /**
     * 将字符串编码为字节数组（创建新数组）
     */
    public byte[] encodeToBytes(String str) {
        encodeToBuffer(str);
        return getBytesFromBuffer();
    }
    
    /**
     * 将字符编码为字节数组（创建新数组）
     */
    public byte[] encodeToBytes(char ch) {
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
        charBuffer.clear();
        charBuffer.put(ch);
        charBuffer.flip();

        // 复用 ByteBuffer
        byteBuffer.clear();

        // 执行编码
        encoder.reset();
        CoderResult result = encoder.encode(charBuffer, byteBuffer, true);
        if (!result.isUnderflow()) {
            throw new IllegalArgumentException("编码失败: " + result);
        }
        result = encoder.flush(byteBuffer);
        if (!result.isUnderflow()) {
            throw new IllegalArgumentException("刷新失败: " + result);
        }

        byteBuffer.flip(); // 准备读取
        return byteBuffer;
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
        charBuffer.clear();
        charBuffer.put(str);
        charBuffer.flip();

        // 计算所需最大字节空间
        int maxBytes = (int)(str.length() * encoder.maxBytesPerChar());
        
        // 确保字节缓冲区足够大
        if (byteBuffer.capacity() < maxBytes) {
            byteBuffer = ByteBuffer.allocate(maxBytes);
        }
        byteBuffer.clear();

        // 执行编码
        encoder.reset();
        CoderResult result = encoder.encode(charBuffer, byteBuffer, true);
        if (!result.isUnderflow()) {
            handleError(result);
        }
        result = encoder.flush(byteBuffer);
        if (!result.isUnderflow()) {
            handleError(result);
        }
        
        byteBuffer.flip(); // 准备读取
        return byteBuffer;
    }

    /**
     * 从当前ByteBuffer获取字节数组（复制数据）
     */
    private byte[] getBytesFromBuffer() {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    private void handleError(CoderResult result) {
        try {
            result.throwException();
        } catch (CharacterCodingException e) {
            throw new RuntimeException("Encoding failed", e);
        }
    }

    public static void main(String[] args) {
        StringEncoder encoder = new StringEncoder(Charset.forName("GB18030"));
        System.out.println(Arrays.toString(encoder.encodeToBytes("中")));
    }
}