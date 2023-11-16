/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    /**
     * This ThreadLocal uses raw and inconvenient Object[] to store three heterogeneous objects (CharEncoder, CharBuffer
     * and ByteBuffer) instead of a custom class, because it needs to contain JDK classes, no custom (Log4j) classes.
     * Where possible putting only JDK classes in ThreadLocals is preferable to avoid memory leaks in web containers:
     * the Log4j classes may be loaded by a separate class loader which cannot be garbage collected if a thread pool
     * threadlocal still has a reference to it.
     *
     * Using just one ThreadLocal instead of three separate ones is an optimization: {@link ThreadLocal.ThreadLocalMap}
     * is polluted less, {@link ThreadLocal.ThreadLocalMap#get()} is called only once on each call to {@link #encode}
     * instead of three times.
     */
    private final ThreadLocal<Object[]> threadLocal = new ThreadLocal<>();

    private final Charset charset;
    private final int charBufferSize;
    private final int byteBufferSize;

    public StringBuilderEncoder(final Charset charset) {
        this(charset, Constants.ENCODER_CHAR_BUFFER_SIZE, Constants.ENCODER_BYTE_BUFFER_SIZE);
    }

    public StringBuilderEncoder(final Charset charset, final int charBufferSize, final int byteBufferSize) {
        this.charBufferSize = charBufferSize;
        this.byteBufferSize = byteBufferSize;
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public void encode(final StringBuilder source, final ByteBufferDestination destination) {
        try {
            final Object[] threadLocalState = getThreadLocalState();
            final CharsetEncoder charsetEncoder = (CharsetEncoder) threadLocalState[0];
            final CharBuffer charBuffer = (CharBuffer) threadLocalState[1];
            final ByteBuffer byteBuffer = (ByteBuffer) threadLocalState[2];
            TextEncoderHelper.encodeText(charsetEncoder, charBuffer, byteBuffer, source, destination);
        } catch (final Exception ex) {
            logEncodeTextException(ex, source);
            TextEncoderHelper.encodeTextFallBack(charset, source, destination);
        }
    }

    private Object[] getThreadLocalState() {
        Object[] threadLocalState = threadLocal.get();
        if (threadLocalState == null) {
            threadLocalState = new Object[] {
                charset.newEncoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE),
                CharBuffer.allocate(charBufferSize),
                ByteBuffer.allocate(byteBufferSize)
            };
            threadLocal.set(threadLocalState);
        } else {
            ((CharsetEncoder) threadLocalState[0]).reset();
            ((CharBuffer) threadLocalState[1]).clear();
            ((ByteBuffer) threadLocalState[2]).clear();
        }
        return threadLocalState;
    }

    private static void logEncodeTextException(final Exception ex, final StringBuilder text) {
        StatusLogger.getLogger().error("Recovering from StringBuilderEncoder.encode('{}') error: {}", text, ex, ex);
    }
}
