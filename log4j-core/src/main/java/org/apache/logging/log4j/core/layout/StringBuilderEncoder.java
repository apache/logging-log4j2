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
    private final ThreadLocal<ThreadLocalState> threadLocal = new ThreadLocal<>();
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
        try {
            ThreadLocalState threadLocalState = getThreadLocalState();
            TextEncoderHelper.encodeText(threadLocalState.charsetEncoder, threadLocalState.charBuffer,
                    threadLocalState.byteBuffer, source, destination);
        } catch (final Exception ex) {
            logEncodeTextException(ex, source, destination);
            TextEncoderHelper.encodeTextFallBack(charset, source, destination);
        }
    }

    private ThreadLocalState getThreadLocalState() {
        ThreadLocalState threadLocalState = threadLocal.get();
        if (threadLocalState == null) {
            threadLocalState = new ThreadLocalState(charset, charBufferSize, byteBufferSize);
            threadLocal.set(threadLocalState);
        } else {
            threadLocalState.charsetEncoder.reset();
            threadLocalState.charBuffer.clear();
            threadLocalState.byteBuffer.clear();
        }
        return threadLocalState;
    }

    private void logEncodeTextException(final Exception ex, final StringBuilder text,
            final ByteBufferDestination destination) {
        StatusLogger.getLogger().error("Recovering from StringBuilderEncoder.encode('{}') error: {}", text, ex, ex);
    }

    private static class ThreadLocalState {
        private final CharsetEncoder charsetEncoder;
        private final CharBuffer charBuffer;
        private final ByteBuffer byteBuffer;

        ThreadLocalState(Charset charset, int charBufferSize, int byteBufferSize) {
            charsetEncoder = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            charBuffer = CharBuffer.allocate(charBufferSize);
            byteBuffer = ByteBuffer.allocate(byteBufferSize);
        }
    }
}
