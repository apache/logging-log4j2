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
import org.apache.logging.log4j.spi.RecyclerFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * {@link Encoder} for {@link StringBuilder}s.
 * <p>
 * <b>{@link StringBuilderEncoder#encode(StringBuilder, ByteBufferDestination) encode()} is not thread-safe!</b>
 * Users are expected to recycle {@link StringBuilderEncoder} instances, e.g., using a {@link RecyclerFactory}.
 * </p>
 */
public class StringBuilderEncoder implements Encoder<StringBuilder> {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private final CharsetEncoder charsetEncoder;

    private final CharBuffer charBuffer;

    private final ByteBuffer byteBuffer;

    private final Charset charset;

    public StringBuilderEncoder(final Charset charset) {
        this(charset, Constants.ENCODER_CHAR_BUFFER_SIZE, Constants.ENCODER_BYTE_BUFFER_SIZE);
    }

    public StringBuilderEncoder(final Charset charset, final int charBufferSize, final int byteBufferSize) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.charsetEncoder = charset
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.charBuffer = CharBuffer.allocate(charBufferSize);
        this.byteBuffer = ByteBuffer.allocate(byteBufferSize);
    }

    /**
     * Encodes the given source to the given destination.
     * <p>
     * <b>This method is not thread-safe!</b>
     * Users are expected to recycle {@link StringBuilderEncoder} instances, e.g., using a {@link RecyclerFactory}.
     * </p>
     *
     * @param source a source
     * @param destination a destination
     */
    @Override
    public void encode(final StringBuilder source, final ByteBufferDestination destination) {
        try {
            TextEncoderHelper.encodeText(charsetEncoder, charBuffer, byteBuffer, source, destination);
        } catch (final Exception error) {
            LOGGER.error("Due to `TextEncoderHelper.encodeText()` failure, falling back to `String#getBytes(Charset)`", error);
            byte[] sourceBytes = source.toString().getBytes(charset);
            destination.writeBytes(sourceBytes, 0, sourceBytes.length);
        } finally {
            charsetEncoder.reset();
            charBuffer.clear();
            byteBuffer.clear();
        }
    }

}
