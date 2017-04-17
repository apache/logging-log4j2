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
        destination.writeBytes(bytes, 0, bytes.length);
    }

    static void encodeText(final CharsetEncoder charsetEncoder, final CharBuffer charBuf, final ByteBuffer byteBuf,
            final StringBuilder text, final ByteBufferDestination destination)
            throws CharacterCodingException {
        charsetEncoder.reset();
        if (text.length() > charBuf.capacity()) {
            encodeChunkedText(charsetEncoder, charBuf, byteBuf, text, destination);
            return;
        }
        charBuf.clear();
        text.getChars(0, text.length(), charBuf.array(), charBuf.arrayOffset());
        charBuf.limit(text.length());
        CoderResult result = charsetEncoder.encode(charBuf, byteBuf, true);
        writeEncodedText(charsetEncoder, charBuf, byteBuf, destination, result);
    }

    private static void writeEncodedText(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            final ByteBuffer byteBuf, final ByteBufferDestination destination, final CoderResult result) {
        if (!result.isUnderflow()) {
            writeChunkedEncodedText(charsetEncoder, charBuf, destination, byteBuf, result);
            return;
        }
        charsetEncoder.flush(byteBuf);
        if (!result.isUnderflow()) {
            synchronized (destination) {
                flushRemainingBytes(charsetEncoder, destination, byteBuf);
            }
            return;
        }
        // If the byteBuf is actually the destination's buffer, this method call should be protected with
        // synchronization on the destination object at some level, and then destination.getByteBuffer() call should
        // be safe. If the byteBuf is an unrelated buffer, this condition should fail despite
        // destination.getByteBuffer() is not protected with the synchronization on the destination object.
        if (byteBuf != destination.getByteBuffer()) {
            byteBuf.flip();
            destination.writeBytes(byteBuf);
            byteBuf.clear();
        }
    }

    private static void writeChunkedEncodedText(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            final ByteBufferDestination destination, ByteBuffer byteBuf, final CoderResult result) {
        synchronized (destination) {
            byteBuf = writeAndEncodeAsMuchAsPossible(charsetEncoder, charBuf, true, destination, byteBuf,
                    result);
            flushRemainingBytes(charsetEncoder, destination, byteBuf);
        }
    }

    private static void encodeChunkedText(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            ByteBuffer byteBuf, final StringBuilder text, final ByteBufferDestination destination) {
        int start = 0;
        CoderResult result = CoderResult.UNDERFLOW;
        boolean endOfInput = false;
        while (!endOfInput && result.isUnderflow()) {
            charBuf.clear();
            final int copied = copy(text, start, charBuf);
            start += copied;
            endOfInput = start >= text.length();
            charBuf.flip();
            result = charsetEncoder.encode(charBuf, byteBuf, endOfInput);
        }
        if (endOfInput) {
            writeEncodedText(charsetEncoder, charBuf, byteBuf, destination, result);
            return;
        }
        synchronized (destination) {
            byteBuf = writeAndEncodeAsMuchAsPossible(charsetEncoder, charBuf, endOfInput, destination, byteBuf,
                    result);
            while (!endOfInput) {
                while (!endOfInput && result.isUnderflow()) {
                    charBuf.clear();
                    final int copied = copy(text, start, charBuf);
                    start += copied;
                    endOfInput = start >= text.length();
                    charBuf.flip();
                    result = charsetEncoder.encode(charBuf, byteBuf, endOfInput);
                }
                byteBuf = writeAndEncodeAsMuchAsPossible(charsetEncoder, charBuf, endOfInput, destination, byteBuf,
                        result);
            }
            flushRemainingBytes(charsetEncoder, destination, byteBuf);
        }
    }

    /**
     * For testing purposes only.
     */
    @Deprecated
    public static void encodeText(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            final ByteBufferDestination destination) {
        charsetEncoder.reset();
        synchronized (destination) {
            ByteBuffer byteBuf = destination.getByteBuffer();
            byteBuf = encodeAsMuchAsPossible(charsetEncoder, charBuf, true, destination, byteBuf);
            flushRemainingBytes(charsetEncoder, destination, byteBuf);
        }
    }

    private static ByteBuffer writeAndEncodeAsMuchAsPossible(final CharsetEncoder charsetEncoder,
            final CharBuffer charBuf, final boolean endOfInput, final ByteBufferDestination destination,
            ByteBuffer temp, CoderResult result) {
        while (true) {
            temp = drainIfByteBufferFull(destination, temp, result);
            if (!result.isOverflow()) {
                break;
            }
            result = charsetEncoder.encode(charBuf, temp, endOfInput);
        }
        if (!result.isUnderflow()) { // we should have fully read the char buffer contents
            throwException(result);
        }
        return temp;
    }

    private static void throwException(final CoderResult result) {
        try {
            result.throwException();
        } catch (CharacterCodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ByteBuffer encodeAsMuchAsPossible(final CharsetEncoder charsetEncoder, final CharBuffer charBuf,
            final boolean endOfInput, final ByteBufferDestination destination, ByteBuffer temp) {
        CoderResult result;
        do {
            result = charsetEncoder.encode(charBuf, temp, endOfInput);
            temp = drainIfByteBufferFull(destination, temp, result);
        } while (result.isOverflow()); // byte buffer has been drained: retry
        if (!result.isUnderflow()) { // we should have fully read the char buffer contents
            throwException(result);
        }
        return temp;
    }

    private static ByteBuffer drainIfByteBufferFull(final ByteBufferDestination destination, ByteBuffer temp,
            final CoderResult result) {
        if (result.isOverflow()) { // byte buffer full
            ByteBuffer destinationBuffer = destination.getByteBuffer();
            if (destinationBuffer != temp) {
                temp.flip();
                ByteBufferDestinationHelper.writeToUnsynchronized(temp, destination);
                temp.clear();
                return destination.getByteBuffer();
            } else {
                return destination.drain(destinationBuffer);
            }
        } else {
            return temp;
        }
    }

    private static void flushRemainingBytes(final CharsetEncoder charsetEncoder,
            final ByteBufferDestination destination, ByteBuffer temp) {
        CoderResult result;
        do {
            // write any final bytes to the output buffer once the overall input sequence has been read
            result = charsetEncoder.flush(temp);
            temp = drainIfByteBufferFull(destination, temp, result);
        } while (result.isOverflow()); // byte buffer has been drained: retry
        if (!result.isUnderflow()) { // we should have fully flushed the remaining bytes
            throwException(result);
        }
        if (temp.remaining() > 0 && temp != destination.getByteBuffer()) {
            temp.flip();
            ByteBufferDestinationHelper.writeToUnsynchronized(temp, destination);
            temp.clear();
        }
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
        source.getChars(offset, offset + length, array, destination.arrayOffset() + start);
        destination.position(start + length);
        return length;
    }
}
