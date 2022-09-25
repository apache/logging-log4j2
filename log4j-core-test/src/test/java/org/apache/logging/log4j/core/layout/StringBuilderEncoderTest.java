package org.apache.logging.log4j.core.layout;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@code TextEncoderHelper} class.
 */
public class StringBuilderEncoderTest {

    @Test
    public void testEncodeText_TextFitCharBuff_BytesFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 16, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(17, 17);
        helper.encode(text, destination);

        assertEquals(0, destination.drainPoints.size(), "drained");
        assertEquals(text.length(), destination.buffer.position(), "destination.buf.pos");

        for (int i = 0; i < text.length(); i++) {
            assertEquals((byte) text.charAt(i), destination.buffer.get(i), "char at " + i);
        }
    }

    @Test
    public void testEncodeText_TextFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 16, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(14, 15);
        helper.encode(text, destination);

        assertEquals(1, destination.drainPoints.size(), "drained");
        assertEquals(0, destination.drainPoints.get(0).position, "drained[0].from");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(0).limit, "drained[0].to");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(0).length(), "drained[0].length");
        assertEquals(text.length() - destination.buffer.capacity(),
                destination.buffer.position(), "destination.buf.pos");

        for (int i = 0; i < destination.buffer.capacity(); i++) {
            assertEquals((byte) text.charAt(i), destination.drained.get(i), "char at " + i);
        }
        for (int i = destination.buffer.capacity(); i < text.length(); i++) {
            final int bufIx = i - destination.buffer.capacity();
            assertEquals((byte) text.charAt(i), destination.buffer.get(bufIx), "char at " + i);
        }
    }

    @Test
    public void testEncodeText_TextFitCharBuff_BytesDontFitByteBuff_MultiplePasses() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 16, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(4, 20);
        helper.encode(text, destination);

        assertEquals(3, destination.drainPoints.size(), "drained");
        assertEquals(0, destination.drainPoints.get(0).position, "drained[0].from");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(0).limit, "drained[0].to");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(0).length(), "drained[0].length");
        assertEquals(0, destination.drainPoints.get(1).position, "drained[1].from");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(1).limit, "drained[1].to");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(1).length(), "drained[1].length");
        assertEquals(0, destination.drainPoints.get(2).position, "drained[2].from");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(2).limit, "drained[2].to");
        assertEquals(destination.buffer.capacity(), destination.drainPoints.get(2).length(), "drained[2].length");
        assertEquals(text.length() - 3 * destination.buffer.capacity(),
                destination.buffer.position(), "destination.buf.pos");

        for (int i = 0; i < 3 * destination.buffer.capacity(); i++) {
            assertEquals((byte) text.charAt(i), destination.drained.get(i), "char at " + i);
        }
        for (int i = 3 * destination.buffer.capacity(); i < text.length(); i++) {
            final int bufIx = i - 3 * destination.buffer.capacity();
            assertEquals((byte) text.charAt(i), destination.buffer.get(bufIx), "char at " + i);
        }
    }

    @Test
    public void testEncodeText_TextDoesntFitCharBuff_BytesFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(17, 17);
        helper.encode(text, destination);

        assertEquals(0, destination.drainPoints.size(), "drained");
        assertEquals(text.length(), destination.buffer.position(), "destination.buf.pos");

        for (int i = 0; i < text.length(); i++) {
            assertEquals((byte) text.charAt(i), destination.buffer.get(i), "char at " + i);
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_BytesFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(50, 50);
        helper.encode(text, destination);

        assertEquals(0, destination.drainPoints.size(), "drained");
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertEquals(utf8[i], destination.drained.get(i), "byte at " + i);
        }
    }

    @Test
    public void testEncodeText_JapaneseTextShiftJisDoesntFitCharBuff_BytesFitByteBuff() throws Exception {
        final Charset SHIFT_JIS = Charset.forName("Shift_JIS");
        final StringBuilderEncoder helper = new StringBuilderEncoder(SHIFT_JIS, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(50, 50);
        helper.encode(text, destination);

        assertEquals(0, destination.drainPoints.size(), "drained");
        destination.drain(destination.getByteBuffer());

        final byte[] bytes = text.toString().getBytes(SHIFT_JIS);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], destination.drained.get(i), "byte at " + i);
        }
    }

    @Test
    public void testEncodeText_TextDoesntFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 17);
        helper.encode(text, destination);

        assertEquals(4, destination.drainPoints.size(), "drained");
        assertEquals(3, destination.buffer.position(), "destination.buf.pos");

        for (int i = 0; i < text.length() - 3; i++) {
            assertEquals((byte) text.charAt(i), destination.drained.get(i), "char at " + i);
        }
        for (int i = 0; i < 3; i++) {
            assertEquals((byte) text.charAt(12 + i), destination.buffer.get(i), "char at " + (12 + i));
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 50);
        helper.encode(text, destination);

        assertEquals(7, destination.drainPoints.size(), "drained");
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertEquals(utf8[i], destination.drained.get(i), "byte at " + i);
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_DoesntFitTempByteBuff_BytesDontFitDestinationByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 5);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 50);
        helper.encode(text, destination);

        assertEquals(15, destination.drainPoints.size(), "drained");
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertEquals(utf8[i], destination.drained.get(i), "byte at " + i);
        }
    }

    @Test
    public void testEncodeText_JapaneseTextShiftJisDoesntFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final Charset SHIFT_JIS = Charset.forName("Shift_JIS");
        final StringBuilderEncoder helper = new StringBuilderEncoder(SHIFT_JIS, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 50);
        helper.encode(text, destination);

        destination.drain(destination.getByteBuffer());

        final byte[] bytes = text.toString().getBytes(SHIFT_JIS);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], destination.drained.get(i), "byte at " + i);
        }
    }

    @Test
    public void testEncodeText_JapaneseTextShiftJisDoesntFitCharBuff_DoesntFitTempByteBuff_BytesDontFitDestinationByteBuff() throws Exception {
        final Charset SHIFT_JIS = Charset.forName("Shift_JIS");
        final StringBuilderEncoder helper = new StringBuilderEncoder(SHIFT_JIS, 4, 5);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 50);
        helper.encode(text, destination);

        destination.drain(destination.getByteBuffer());

        final byte[] bytes = text.toString().getBytes(SHIFT_JIS);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(bytes[i], destination.drained.get(i), "byte at " + i);
        }
    }

    @Test
    public void testCopyCopiesAllDataIfSuffientRemainingSpace() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[16]);
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals(text.length(), length, "everything fits");
        for (int i = 0; i < length; i++) {
            assertEquals(text.charAt(i), buff.get(i), "char at " + i);
        }
        assertEquals(text.length(), buff.position(), "position moved by length");
    }

    @Test
    public void testCopyUpToRemainingSpace() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[3]);
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals(buff.capacity(), length, "partial copy");
        for (int i = 0; i < length; i++) {
            assertEquals(text.charAt(i), buff.get(i), "char at " + i);
        }
        assertEquals(0, buff.remaining(), "no space remaining");
        assertEquals(buff.capacity(), buff.position(), "position at end");
    }

    @Test
    public void testCopyDoesNotWriteBeyondStringText() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[5]);
        assertEquals(0, buff.position(), "initial buffer position");
        final StringBuilder text = createText(2);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals(text.length(), length, "full copy");
        for (int i = 0; i < length; i++) {
            assertEquals(text.charAt(i), buff.get(i), "char at " + i);
        }
        assertEquals(text.length(), buff.position(), "resulting buffer position");
        for (int i = length; i < buff.capacity(); i++) {
            assertEquals(0, buff.get(i), "unset char at " + i);
        }
    }

    @Test
    public void testCopyStartsAtBufferPosition() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[10]);
        final int START_POSITION = 5;
        buff.position(START_POSITION); // set start position
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals(buff.capacity() - START_POSITION, length, "partial copy");
        for (int i = 0; i < length; i++) {
            assertEquals(text.charAt(i), buff.get(START_POSITION + i), "char at " + i);
        }
        assertEquals(buff.capacity(), buff.position(), "buffer position at end");
    }

    @Test
    public void testEncode_ALotWithoutErrors() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(Charset.defaultCharset());
        final StringBuilder text = new StringBuilder("2016-04-13 21:07:47,487 DEBUG [org.apache.logging.log4j.perf.jmh.FileAppenderBenchmark.log4j2ParameterizedString-jmh-worker-1] FileAppenderBenchmark  - This is a debug [2383178] message\r\n");
        final int DESTINATION_SIZE = 1024 * 1024;
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(256 * 1024, DESTINATION_SIZE);

        final int max = DESTINATION_SIZE / text.length();
        for (int i = 0; i < max; i++) {
            helper.encode(text, destination);
        }
        // no error
    }

    private StringBuilder createText(final int length) {
        final StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append((char) (' ' + i)); // space=0x20
        }
        return result;
    }
}