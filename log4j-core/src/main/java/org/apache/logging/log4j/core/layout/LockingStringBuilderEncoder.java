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

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Encoder for StringBuilders that locks on the ByteBufferDestination.
 */
public class LockingStringBuilderEncoder implements Encoder<StringBuilder> {

    private final Charset charset;
    private final CharsetEncoder charsetEncoder;
    private final CharBuffer cachedCharBuffer;

    public LockingStringBuilderEncoder(final Charset charset) {
        this(charset, Constants.ENCODER_CHAR_BUFFER_SIZE);
    }

    public LockingStringBuilderEncoder(final Charset charset, final int charBufferSize) {
        this.charset = Objects.requireNonNull(charset, "charset");
        this.charsetEncoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.cachedCharBuffer = CharBuffer.wrap(new char[charBufferSize]);
    }

    private CharBuffer getCharBuffer() {
        return cachedCharBuffer;
    }

    @Override
    public void encode(final StringBuilder source, final ByteBufferDestination destination) {
        try {
            // This synchronized is needed to be able to call destination.getByteBuffer()
            synchronized (destination) {
                TextEncoderHelper.encodeText(
                        charsetEncoder, cachedCharBuffer, destination.getByteBuffer(), source, destination);
            }
        } catch (final Exception ex) {
            logEncodeTextException(ex, source, destination);
            TextEncoderHelper.encodeTextFallBack(charset, source, destination);
        }
    }

    private void logEncodeTextException(
            final Exception ex, final StringBuilder text, final ByteBufferDestination destination) {
        StatusLogger.getLogger().error("Recovering from LockingStringBuilderEncoder.encode('{}') error", text, ex);
    }
}
