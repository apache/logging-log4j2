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

/**
 * Helper class to encode text to binary data without allocating temporary objects.
 *
 * @since 2.6
 */
public class TextEncoderHelper {

    private TextEncoderHelper() {
    }

    static void encodeTextFallBack(final Charset charset, final StringBuilder text,
            final ByteBufferDestination destination) {
        final byte[] bytes = text.toString().getBytes(charset);
        synchronized (destination) {
            ByteBuffer buffer = destination.getByteBuffer();
            int offset = 0;
            do {
                final int length = Math.min(bytes.length - offset, buffer.remaining());
                buffer.put(bytes, offset, length);
                offset += length;
                if (offset < bytes.length) {
                    buffer = destination.drain(buffer);
                }
            } while (offset < bytes.length);
        }
    }

    static void encodeTextWithCopy(final CharsetEncoder charsetEncoder, final CharBuffer charBuf, final ByteBuffer temp,
            final StringBuilder text, final ByteBufferDestination destination) {

        encodeText(charsetEncoder, charBuf, temp, text, destination);
        copyDataToDestination(temp, destination);
    }

    private static void copyDataToDestination(final ByteBuffer temp, final ByteBufferDestination destination) {
        synchronized (destination) {
            ByteBuffer destinationBuffer = destination.getByteBuffer();
            if (destinationBuffer != temp) { // still need to write to the destination
                temp.flip();
                if (temp.remaining() > destinationBuffer.remaining()) {
                    destinationBuffer = destination.drain(destinationBuffer);
                }
                destinationBuffer.put(temp);
                temp.clear();
            }
        }
    }

    static void encodeText(final CharsetEncoder charsetEncoder, final CharBuffer charBuf, final ByteBuffer byteBuf,
            final StringBuilder text, final ByteBufferDestination destination) {
        charsetEncoder.reset();
        ByteBuffer temp = byteBuf; // may be the destination's buffer or a temporary buffer
        int start = 0;
        int todoChars = text.length();
        boolean endOfInput = true;
        do {
            charBuf.clear(); // reset character buffer position to zero, limit to capacity
            final int copied = copy(text, start, charBuf);
            start += copied;
            todoChars -= copied;
            endOfInput = todoChars <= 0;

            charBuf.flip(); // prepare for reading: set limit to position, position to zero
            temp = encode(charsetEncoder, charBuf, endOfInput, destination, temp);
        } while (!endOfInput);
    }

    /**
     * For testing purposes only.
     */
    @Deprecated
    public static void encodeText(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            final ByteBufferDestination destination) {
        synchronized (destination) {
            charsetEncoder.reset();
            final ByteBuffer byteBuf = destination.getByteBuffer();
            encode(charsetEncoder, charBuf, true, destination, byteBuf);
        }
    }

    private static ByteBuffer encode(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            final boolean endOfInput, final ByteBufferDestination destination, ByteBuffer byteBuf) {
        try {
            byteBuf = encodeAsMuchAsPossible(charsetEncoder, charBuf, endOfInput, destination, byteBuf);
            if (endOfInput) {
                byteBuf = flushRemainingBytes(charsetEncoder, destination, byteBuf);
            }
        } catch (final CharacterCodingException ex) {
            throw new IllegalStateException(ex);
        }
        return byteBuf;
    }

    private static ByteBuffer encodeAsMuchAsPossible(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            final boolean endOfInput, final ByteBufferDestination destination, ByteBuffer temp)
            throws CharacterCodingException {
        CoderResult result;
        do {
            result = charsetEncoder.encode(charBuf, temp, endOfInput);
            temp = drainIfByteBufferFull(destination, temp, result);
        } while (result.isOverflow()); // byte buffer has been drained: retry
        if (!result.isUnderflow()) { // we should have fully read the char buffer contents
            result.throwException();
        }
        return temp;
    }

    private static ByteBuffer drainIfByteBufferFull(final ByteBufferDestination destination, ByteBuffer temp, final CoderResult result) {
        if (result.isOverflow()) { // byte buffer full

            // SHOULD NOT HAPPEN:
            // CALLER SHOULD ONLY PASS TEMP ByteBuffer LARGE ENOUGH TO ENCODE ALL CHARACTERS,
            // AND LOCK ON THE DESTINATION IF THIS IS NOT POSSIBLE
            ByteBuffer destinationBuffer = destination.getByteBuffer();
            if (destinationBuffer != temp) {
                temp.flip();
                destinationBuffer.put(temp);
                temp.clear();
            }
            // destination consumes contents
            // and returns byte buffer with more capacity
            destinationBuffer = destination.drain(destinationBuffer);
            temp = destinationBuffer;
        }
        return temp;
    }

    private static ByteBuffer flushRemainingBytes(final CharsetEncoder charsetEncoder,
            final ByteBufferDestination destination, ByteBuffer temp)
            throws CharacterCodingException {
        CoderResult result;
        do {
            // write any final bytes to the output buffer once the overall input sequence has been read
            result = charsetEncoder.flush(temp);
            temp = drainIfByteBufferFull(destination, temp, result);
        } while (result.isOverflow()); // byte buffer has been drained: retry
        if (!result.isUnderflow()) { // we should have fully flushed the remaining bytes
            result.throwException();
        }
        return temp;
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
        final char[] array = destination.array();
        final int start = destination.position();
        source.getChars(offset, offset + length, array, start);
        destination.position(start + length);
        return length;
    }
}
