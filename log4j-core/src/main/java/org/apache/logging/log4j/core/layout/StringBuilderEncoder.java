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
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Encoder for StringBuilders that uses ThreadLocals to avoid locking as much as possible.
 */
public class StringBuilderEncoder implements Encoder<StringBuilder> {

    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8 * 1024;
    private final ThreadLocal<CharBuffer> charBufferThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<ByteBuffer> byteBufferThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<CharsetEncoder> charsetEncoderThreadLocal = new ThreadLocal<>();
    private final Charset charset;
    private final int charBufferSize;
    private final int byteBufferSize;

    public StringBuilderEncoder(final Charset charset) {
        this(charset, Constants.ENCODER_CHAR_BUFFER_SIZE, DEFAULT_BYTE_BUFFER_SIZE);
    }

    public StringBuilderEncoder(final Charset charset, final int charBufferSize, final int byteBufferSize) {
        this.charBufferSize = charBufferSize;
        this.byteBufferSize = byteBufferSize;
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public void encode(final StringBuilder source, final ByteBufferDestination destination) {
        final ByteBuffer temp = getByteBuffer();
        temp.clear();
        temp.limit(Math.min(temp.capacity(), destination.getByteBuffer().capacity()));
        final CharsetEncoder charsetEncoder = getCharsetEncoder();

        final int estimatedBytes = estimateBytes(source.length(), charsetEncoder.maxBytesPerChar());
        if (temp.remaining() < estimatedBytes) {
            encodeSynchronized(getCharsetEncoder(), getCharBuffer(), source, destination);
        } else {
            encodeWithThreadLocals(charsetEncoder, getCharBuffer(), temp, source, destination);
        }
    }

    private void encodeWithThreadLocals(final CharsetEncoder charsetEncoder, final CharBuffer charBuffer,
            final ByteBuffer temp, final StringBuilder source, final ByteBufferDestination destination) {
        try {
            TextEncoderHelper.encodeTextWithCopy(charsetEncoder, charBuffer, temp, source, destination);
        } catch (final Exception ex) {
            logEncodeTextException(ex, source, destination);
            TextEncoderHelper.encodeTextFallBack(charset, source, destination);
        }
    }

    private static int estimateBytes(final int charCount, final float maxBytesPerChar) {
        return (int) (charCount * (double) maxBytesPerChar);
    }

    private void encodeSynchronized(final CharsetEncoder charsetEncoder, final CharBuffer charBuffer,
            final StringBuilder source, final ByteBufferDestination destination) {
        synchronized (destination) {
            try {
                TextEncoderHelper.encodeText(charsetEncoder, charBuffer, destination.getByteBuffer(), source,
                        destination);
            } catch (final Exception ex) {
                logEncodeTextException(ex, source, destination);
                TextEncoderHelper.encodeTextFallBack(charset, source, destination);
            }
        }
    }

    private CharsetEncoder getCharsetEncoder() {
        CharsetEncoder result = charsetEncoderThreadLocal.get();
        if (result == null) {
            result = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            charsetEncoderThreadLocal.set(result);
        }
        return result;
    }


    private CharBuffer getCharBuffer() {
        CharBuffer result = charBufferThreadLocal.get();
        if (result == null) {
            result = CharBuffer.wrap(new char[charBufferSize]);
            charBufferThreadLocal.set(result);
        }
        return result;
    }

    private ByteBuffer getByteBuffer() {
        ByteBuffer result = byteBufferThreadLocal.get();
        if (result == null) {
            result = ByteBuffer.wrap(new byte[byteBufferSize]);
            byteBufferThreadLocal.set(result);
        }
        return result;
    }

    private void logEncodeTextException(final Exception ex, final StringBuilder text,
                                        final ByteBufferDestination destination) {
        StatusLogger.getLogger().error("Recovering from StringBuilderEncoder.encode('{}') error: {}", text, ex, ex);
    }
}
