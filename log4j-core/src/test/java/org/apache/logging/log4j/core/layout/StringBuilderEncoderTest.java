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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

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

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(0);
        assertThat(destination.buffer.position()).describedAs("destination.buf.pos").isEqualTo(text.length());

        for (int i = 0; i < text.length(); i++) {
            assertThat(destination.buffer.get(i)).describedAs("char at " + i).isEqualTo((byte) text.charAt(i));
        }
    }

    @Test
    public void testEncodeText_TextFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 16, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(14, 15);
        helper.encode(text, destination);

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(1);
        assertThat(destination.drainPoints.get(0).position).describedAs("drained[0].from").isEqualTo(0);
        assertThat(destination.drainPoints.get(0).limit).describedAs("drained[0].to").isEqualTo(destination.buffer.capacity());
        assertThat(destination.drainPoints.get(0).length()).describedAs("drained[0].length").isEqualTo(destination.buffer.capacity());
        assertThat(destination.buffer.position()).describedAs("destination.buf.pos").isEqualTo(text.length() - destination.buffer.capacity());

        for (int i = 0; i < destination.buffer.capacity(); i++) {
            assertThat(destination.drained.get(i)).describedAs("char at " + i).isEqualTo((byte) text.charAt(i));
        }
        for (int i = destination.buffer.capacity(); i < text.length(); i++) {
            final int bufIx = i - destination.buffer.capacity();
            assertThat(destination.buffer.get(bufIx)).describedAs("char at " + i).isEqualTo((byte) text.charAt(i));
        }
    }

    @Test
    public void testEncodeText_TextFitCharBuff_BytesDontFitByteBuff_MultiplePasses() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 16, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(4, 20);
        helper.encode(text, destination);

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(3);
        assertThat(destination.drainPoints.get(0).position).describedAs("drained[0].from").isEqualTo(0);
        assertThat(destination.drainPoints.get(0).limit).describedAs("drained[0].to").isEqualTo(destination.buffer.capacity());
        assertThat(destination.drainPoints.get(0).length()).describedAs("drained[0].length").isEqualTo(destination.buffer.capacity());
        assertThat(destination.drainPoints.get(1).position).describedAs("drained[1].from").isEqualTo(0);
        assertThat(destination.drainPoints.get(1).limit).describedAs("drained[1].to").isEqualTo(destination.buffer.capacity());
        assertThat(destination.drainPoints.get(1).length()).describedAs("drained[1].length").isEqualTo(destination.buffer.capacity());
        assertThat(destination.drainPoints.get(2).position).describedAs("drained[2].from").isEqualTo(0);
        assertThat(destination.drainPoints.get(2).limit).describedAs("drained[2].to").isEqualTo(destination.buffer.capacity());
        assertThat(destination.drainPoints.get(2).length()).describedAs("drained[2].length").isEqualTo(destination.buffer.capacity());
        assertThat(destination.buffer.position()).describedAs("destination.buf.pos").isEqualTo(text.length() - 3 * destination.buffer.capacity());

        for (int i = 0; i < 3 * destination.buffer.capacity(); i++) {
            assertThat(destination.drained.get(i)).describedAs("char at " + i).isEqualTo((byte) text.charAt(i));
        }
        for (int i = 3 * destination.buffer.capacity(); i < text.length(); i++) {
            final int bufIx = i - 3 * destination.buffer.capacity();
            assertThat(destination.buffer.get(bufIx)).describedAs("char at " + i).isEqualTo((byte) text.charAt(i));
        }
    }

    @Test
    public void testEncodeText_TextDoesntFitCharBuff_BytesFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(17, 17);
        helper.encode(text, destination);

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(0);
        assertThat(destination.buffer.position()).describedAs("destination.buf.pos").isEqualTo(text.length());

        for (int i = 0; i < text.length(); i++) {
            assertThat(destination.buffer.get(i)).describedAs("char at " + i).isEqualTo((byte) text.charAt(i));
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_BytesFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(50, 50);
        helper.encode(text, destination);

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(0);
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertThat(destination.drained.get(i)).describedAs("byte at " + i).isEqualTo(utf8[i]);
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

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(0);
        destination.drain(destination.getByteBuffer());

        final byte[] bytes = text.toString().getBytes(SHIFT_JIS);
        for (int i = 0; i < bytes.length; i++) {
            assertThat(destination.drained.get(i)).describedAs("byte at " + i).isEqualTo(bytes[i]);
        }
    }

    @Test
    public void testEncodeText_TextDoesntFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = createText(15);
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 17);
        helper.encode(text, destination);

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(4);
        assertThat(destination.buffer.position()).describedAs("destination.buf.pos").isEqualTo(3);

        for (int i = 0; i < text.length() - 3; i++) {
            assertThat(destination.drained.get(i)).describedAs("char at " + i).isEqualTo((byte) text.charAt(i));
        }
        for (int i = 0; i < 3; i++) {
            assertThat(destination.buffer.get(i)).describedAs("char at " + (12 + i)).isEqualTo((byte) text.charAt(12 + i));
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_BytesDontFitByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 8 * 1024);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 50);
        helper.encode(text, destination);

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(7);
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertThat(destination.drained.get(i)).describedAs("byte at " + i).isEqualTo(utf8[i]);
        }
    }

    @Test
    public void testEncodeText_JapaneseTextUtf8DoesntFitCharBuff_DoesntFitTempByteBuff_BytesDontFitDestinationByteBuff() throws Exception {
        final StringBuilderEncoder helper = new StringBuilderEncoder(StandardCharsets.UTF_8, 4, 5);
        final StringBuilder text = new StringBuilder( // 日本語テスト文章日本語テスト文章
                "\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0\u65e5\u672c\u8a9e\u30c6\u30b9\u30c8\u6587\u7ae0");
        final SpyByteBufferDestination destination = new SpyByteBufferDestination(3, 50);
        helper.encode(text, destination);

        assertThat(destination.drainPoints.size()).describedAs("drained").isEqualTo(15);
        destination.drain(destination.getByteBuffer());

        final byte[] utf8 = text.toString().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < utf8.length; i++) {
            assertThat(destination.drained.get(i)).describedAs("byte at " + i).isEqualTo(utf8[i]);
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
            assertThat(destination.drained.get(i)).describedAs("byte at " + i).isEqualTo(bytes[i]);
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
            assertThat(destination.drained.get(i)).describedAs("byte at " + i).isEqualTo(bytes[i]);
        }
    }

    @Test
    public void testCopyCopiesAllDataIfSuffientRemainingSpace() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[16]);
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertThat(length).describedAs("everything fits").isEqualTo(text.length());
        for (int i = 0; i < length; i++) {
            assertThat(buff.get(i)).describedAs("char at " + i).isEqualTo(text.charAt(i));
        }
        assertThat(buff.position()).describedAs("position moved by length").isEqualTo(text.length());
    }

    @Test
    public void testCopyUpToRemainingSpace() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[3]);
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertThat(length).describedAs("partial copy").isEqualTo(buff.capacity());
        for (int i = 0; i < length; i++) {
            assertThat(buff.get(i)).describedAs("char at " + i).isEqualTo(text.charAt(i));
        }
        assertThat(buff.remaining()).describedAs("no space remaining").isEqualTo(0);
        assertThat(buff.position()).describedAs("position at end").isEqualTo(buff.capacity());
    }

    @Test
    public void testCopyDoesNotWriteBeyondStringText() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[5]);
        assertThat(buff.position()).describedAs("initial buffer position").isEqualTo(0);
        final StringBuilder text = createText(2);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertThat(length).describedAs("full copy").isEqualTo(text.length());
        for (int i = 0; i < length; i++) {
            assertThat(buff.get(i)).describedAs("char at " + i).isEqualTo(text.charAt(i));
        }
        assertThat(buff.position()).describedAs("resulting buffer position").isEqualTo(text.length());
        for (int i = length; i < buff.capacity(); i++) {
            assertThat(buff.get(i)).describedAs("unset char at " + i).isEqualTo(0);
        }
    }

    @Test
    public void testCopyStartsAtBufferPosition() throws Exception {
        final CharBuffer buff = CharBuffer.wrap(new char[10]);
        final int START_POSITION = 5;
        buff.position(START_POSITION); // set start position
        final StringBuilder text = createText(15);
        final int length = TextEncoderHelper.copy(text, 0, buff);
        assertThat(length).describedAs("partial copy").isEqualTo(buff.capacity() - START_POSITION);
        for (int i = 0; i < length; i++) {
            assertThat(buff.get(START_POSITION + i)).describedAs("char at " + i).isEqualTo(text.charAt(i));
        }
        assertThat(buff.position()).describedAs("buffer position at end").isEqualTo(buff.capacity());
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
