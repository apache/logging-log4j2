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

import org.junit.Test;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

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

        assertEquals("drained", 0, destination.drainPoints.size());
        assertEquals("destination.buf.pos", text.length(), destination.buffer.position());

        for (int i = 0; i < text.length(); i++) {
            assertEquals("char at " + i, (byte) text.charAt(i), destination.buffer.get(i));
        }
    }

    @Test
    public void testEncodeText_TextFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 16, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(14, 15);
        helper.encode(text, destination);

        assertEquals("drained", 1, destination.drainPoints.size());
        assertEquals("drained[0].from", 0, destination.drainPoints.get(0).position);
        assertEquals("drained[0].to", destination.buffer.capacity(), destination.drainPoints.get(0).limit);
        assertEquals("drained[0].length", destination.buffer.capacity(), destination.drainPoints.get(0).length());
        assertEquals("destination.buf.pos", text.length() - destination.buffer.capacity(),
                destination.buffer.position());

        for (int i = 0; i < destination.buffer.capacity(); i++) {
            assertEquals("char at " + i, (byte) text.charAt(i), destination.drained.get(i));
        }
        for (int i = destination.buffer.capacity(); i < text.length(); i++) {
            final int bufIx = i - destination.buffer.capacity();
            assertEquals("char at " + i, (byte) text.charAt(i), destination.buffer.get(bufIx));
        }
    }

    @Test
    public void testEncodeText_TextFitCharBuff_BytesDontFitByteBuff_MultiplePasses() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 16, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(4, 20);
        helper.encode(text, destination);

        assertEquals("drained", 3, destination.drainPoints.size());
        assertEquals("drained[0].from", 0, destination.drainPoints.get(0).position);
        assertEquals("drained[0].to", destination.buffer.capacity(), destination.drainPoints.get(0).limit);
        assertEquals("drained[0].length", destination.buffer.capacity(), destination.drainPoints.get(0).length());
        assertEquals("drained[1].from", 0, destination.drainPoints.get(1).position);
        assertEquals("drained[1].to", destination.buffer.capacity(), destination.drainPoints.get(1).limit);
        assertEquals("drained[1].length", destination.buffer.capacity(), destination.drainPoints.get(1).length());
        assertEquals("drained[2].from", 0, destination.drainPoints.get(2).position);
        assertEquals("drained[2].to", destination.buffer.capacity(), destination.drainPoints.get(2).limit);
        assertEquals("drained[2].length", destination.buffer.capacity(), destination.drainPoints.get(2).length());
        assertEquals("destination.buf.pos", text.length() - 3 * destination.buffer.capacity(),
                destination.buffer.position());

        for (int i = 0; i < 3 * destination.buffer.capacity(); i++) {
            assertEquals("char at " + i, (byte) text.charAt(i), destination.drained.get(i));
        }
        for (int i = 3 * destination.buffer.capacity(); i < text.length(); i++) {
            final int bufIx = i - 3 * destination.buffer.capacity();
            assertEquals("char at " + i, (byte) text.charAt(i), destination.buffer.get(bufIx));
        }
    }

    @Test
    public void testEncodeText_TextDoesntFitCharBuff_BytesFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(17, 17);
        helper.encode(text, destination);

        assertEquals("drained", 0, destination.drainPoints.size());
        assertEquals("destination.buf.pos", text.length(), destination.buffer.position());

        for (int i = 0; i < text.length(); i++) {
            assertEquals("char at " + i, (byte) text.charAt(i), destination.buffer.get(i));
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_BytesFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(50, 50);
        helper.encode(text, destination);

        assertEquals("drained", 0, destination.drainPoints.size());
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertEquals("byte at " + i, utf8[i], destination.drained.get(i));
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

        assertEquals("drained", 0, destination.drainPoints.size());
        destination.drain(destination.getByteBuffer());

        final byte[] bytes = text.toString().getBytes(SHIFT_JIS);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals("byte at " + i, bytes[i], destination.drained.get(i));
        }
    }

    @Test
    public void testEncodeText_TextDoesntFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 17);
        helper.encode(text, destination);

        assertEquals("drained", 4, destination.drainPoints.size());
        assertEquals("destination.buf.pos", 3, destination.buffer.position());

        for (int i = 0; i < text.length() - 3; i++) {
            assertEquals("char at " + i, (byte) text.charAt(i), destination.drained.get(i));
        }
        for (int i = 0; i < 3; i++) {
            assertEquals("char at " + (12 + i), (byte) text.charAt(12 + i), destination.buffer.get(i));
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 50);
        helper.encode(text, destination);

        assertEquals("drained", 7, destination.drainPoints.size());
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertEquals("byte at " + i, utf8[i], destination.drained.get(i));
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

        assertEquals("drained", 7, destination.drainPoints.size());
        destination.drain(destination.getByteBuffer());

        final byte[] bytes = text.toString().getBytes(SHIFT_JIS);
        for (int i = 0; i < bytes.length; i++) {
            assertEquals("byte at " + i, bytes[i], destination.drained.get(i));
        }
    }

    @Test
    public void testCopyCopiesAllDataIfSuffientRemainingSpace() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[16]);
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals("everything fits", text.length(), length);
        for (int i = 0; i < length; i++) {
            assertEquals("char at " + i, text.charAt(i), buff.get(i));
        }
        assertEquals("position moved by length", text.length(), buff.position());
    }

    @Test
    public void testCopyUpToRemainingSpace() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[3]);
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals("partial copy", buff.capacity(), length);
        for (int i = 0; i < length; i++) {
            assertEquals("char at " + i, text.charAt(i), buff.get(i));
        }
        assertEquals("no space remaining", 0, buff.remaining());
        assertEquals("position at end", buff.capacity(), buff.position());
    }

    @Test
    public void testCopyDoesNotWriteBeyondStringText() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[5]);
        assertEquals("initial buffer position", 0, buff.position());
        final StringBuilder text = createText(2);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals("full copy", text.length(), length);
        for (int i = 0; i < length; i++) {
            assertEquals("char at " + i, text.charAt(i), buff.get(i));
        }
        assertEquals("resulting buffer position", text.length(), buff.position());
        for (int i = length; i < buff.capacity(); i++) {
            assertEquals("unset char at " + i, 0, buff.get(i));
        }
    }

    @Test
    public void testCopyStartsAtBufferPosition() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[10]);
        final int START_POSITION = 5;
        buff.position(START_POSITION); // set start position
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertEquals("partial copy", buff.capacity() - START_POSITION, length);
        for (int i = 0; i < length; i++) {
            assertEquals("char at " + i, text.charAt(i), buff.get(START_POSITION + i));
        }
        assertEquals("buffer position at end", buff.capacity(), buff.position());
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