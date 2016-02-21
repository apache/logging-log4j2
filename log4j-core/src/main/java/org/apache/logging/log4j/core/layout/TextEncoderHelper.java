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
package org.apache.logging.log4j.core.layout;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/**
 * Helper class to encode text to binary data without allocating temporary objects.
 *
 * @since 2.6
 */
public class TextEncoderHelper {
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private final Charset charset;
    private final CharBuffer cachedCharBuffer;
    private final CharsetEncoder charsetEncoder;

    public TextEncoderHelper(final Charset charset) {
        this(charset, DEFAULT_BUFFER_SIZE);
    }

    public TextEncoderHelper(final Charset charset, final int bufferSize) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.charsetEncoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.cachedCharBuffer = CharBuffer.wrap(new char[bufferSize]);
    }

    public void encodeText(final StringBuilder text, final ByteBufferDestination destination) {
        charsetEncoder.reset();
        ByteBuffer byteBuf = destination.getByteBuffer();
        final CharBuffer charBuf = getCachedCharBuffer();
        int start = 0;
        int todoChars = text.length();
        boolean endOfInput = true;
        do {
            charBuf.clear(); // reset character buffer position to zero, limit to capacity
            int copied = copy(text, start, charBuf);
            start += copied;
            todoChars -= copied;
            endOfInput = todoChars <= 0;

            charBuf.flip(); // prepare for reading: set limit to position, position to zero
            byteBuf = encode(charBuf, endOfInput, destination, byteBuf);
        } while (!endOfInput);
    }

    private ByteBuffer encode(final CharBuffer charBuf, final boolean endOfInput,
            final ByteBufferDestination destination, ByteBuffer byteBuf) {
        try {
            byteBuf = encodeAsMuchAsPossible(charBuf, endOfInput, destination, byteBuf);
            if (endOfInput) {
                byteBuf = flushRemainingBytes(destination, byteBuf);
            }
        } catch (final CharacterCodingException ex) {
            throw new IllegalStateException(ex);
        }
        return byteBuf;
    }

    private ByteBuffer encodeAsMuchAsPossible(final CharBuffer charBuf, final boolean endOfInput,
            final ByteBufferDestination destination, ByteBuffer byteBuf) throws CharacterCodingException {
        CoderResult result;
        do {
            result = charsetEncoder.encode(charBuf, byteBuf, endOfInput);
            if (result.isOverflow()) { // byteBuf full
                // destination consumes contents
                // and returns byte buffer with more capacity
                byteBuf = destination.drain(byteBuf);
            }
        } while (result.isOverflow()); // byteBuf has been drained: retry
        if (!result.isUnderflow()) { // we should have fully read the char buffer contents
            result.throwException();
        }
        return byteBuf;
    }

    private ByteBuffer flushRemainingBytes(final ByteBufferDestination destination, ByteBuffer byteBuf)
            throws CharacterCodingException {
        CoderResult result;
        do {
            // write any final bytes to the output buffer once the overall input sequence has been read
           result = charsetEncoder.flush(byteBuf);
            if (result.isOverflow()) { // byteBuf full
                // destination consumes contents
                // and returns byte buffer with more capacity
                byteBuf = destination.drain(byteBuf);
            }
        } while (result.isOverflow()); // byteBuf has been drained: retry
        if (!result.isUnderflow()) { // we should have fully flushed the remaining bytes
            result.throwException();
        }
        return byteBuf;
    }

    /**
     * Copies characters from the StringBuilder into the CharBuffer,
     * starting at the specified offset and ending when either all
     * characters have been copied or when the CharBuffer is full.
     *
     * @return the number of characters that were copied
     */
    static int copy(final StringBuilder source, final int offset, final CharBuffer destination) {
        final int length = Math.min(source.length() - offset, destination.remaining());
        for (int i = offset; i < offset + length; i++) {
            destination.put(source.charAt(i));
        }
        return length;
    }

    CharBuffer getCachedCharBuffer() {
        return cachedCharBuffer;
    }
}
