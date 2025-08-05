package org.example.succinct.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
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
        if (length <= 0) return 0;
        
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
        // 使用示例
        Charset charset = Charset.forName("GB18030");
        StringEncoder encoder = new StringEncoder(charset);
        
        // 测试数据
        String[] testStrings = {
            "Hello, World!",
            "Java性能优化",
            "复用对象减少内存分配",
            "The quick brown fox jumps over the lazy dog"
        };
        
        // 性能测试
        int iterations = 100_000;
        long startTime, duration;
        
        // 测试传统getBytes()
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String str : testStrings) {
                byte[] bytes = str.getBytes(charset);
            }
        }
        duration = System.nanoTime() - startTime;
        System.out.printf("传统getBytes()耗时: %,d ns%n", duration);
        
        // 测试优化后的编码器
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String str : testStrings) {
                byte[] bytes = encoder.encodeToBytes(str);
            }
        }
        duration = System.nanoTime() - startTime;
        System.out.printf("优化编码器耗时:   %,d ns%n", duration);
        
        // 测试零拷贝方法
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String str : testStrings) {
                ByteBuffer buffer = encoder.encodeToBuffer(str);
                // 这里可以直接使用buffer，无需复制
            }
        }
        duration = System.nanoTime() - startTime;
        System.out.printf("零拷贝方法耗时:   %,d ns%n", duration);
    }
}