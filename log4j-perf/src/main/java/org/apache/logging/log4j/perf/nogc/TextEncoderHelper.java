/*
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
package org.apache.logging.log4j.perf.nogc;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

/**
 * TextEncoderHelper class proposed in LOG4J2-1274.
 */
public class TextEncoderHelper {
    private Charset charset;
    private CharBuffer cachedCharBuffer = CharBuffer.wrap(new char[2048]);
    private CharsetEncoder charsetEncoder;

    public TextEncoderHelper(Charset charset) {
        this.charset = charset;
        charsetEncoder = charset.newEncoder();
    }

    public void encodeWithoutAllocation(StringBuilder text, ByteBufferDestination destination) {
        charsetEncoder.reset();
        ByteBuffer byteBuf = destination.getByteBuffer();
        CharBuffer charBuf = getCachedCharBuffer();
        charBuf.clear();
        int start = 0;
        int todoChars = text.length();
        boolean endOfInput = true;
        do {
            int copied = copy(text, start, charBuf);
            start += copied;
            todoChars -= copied;
            endOfInput = todoChars <= 0;

            charBuf.flip();
            CoderResult result;
            do {
                result = charsetEncoder.encode(charBuf, byteBuf, endOfInput);
                if (result == CoderResult.OVERFLOW) { // byteBuf full
                    // destination consumes contents
                    // and returns byte buffer with more capacity
                    byteBuf = destination.drain(byteBuf);
                }
            } while (result == CoderResult.OVERFLOW);
        } while (!endOfInput);
    }

    /**
     * Copies characters from the StringBuilder into the CharBuffer,
     * starting at the specified offset and ending when either all
     * characters have been copied or when the CharBuffer is full.
     *
     * @return the number of characters that were copied
     */
    int copy(StringBuilder source, int offset, CharBuffer destination) {
        int length = Math.min(source.length() - offset, destination.capacity());
        for (int i = offset; i < offset + length; i++) {
            destination.put(source.charAt(i));
        }
        return length;
    }

    public CharBuffer getCachedCharBuffer() {
        return cachedCharBuffer;
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("AAA bbb ccc dddd eee fff ggg hhh iii");
        ByteBufferDestination dest = new ByteBufferDestination() {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[4096]);

            @Override
            public ByteBuffer getByteBuffer() {
                return buffer;
            }

            @Override
            public ByteBuffer drain(ByteBuffer buf) {
                buf.flip();
                buf.clear();
                return buf;
            }
        };
        TextEncoderHelper helper = new TextEncoderHelper(StandardCharsets.UTF_8);
        for (int i = 0; i < 100; i++) {
            helper.encodeWithoutAllocation(sb, dest);
            dest.drain(dest.getByteBuffer());
        }
    }
}
