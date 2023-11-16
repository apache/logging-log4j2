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

    private TextEncoderHelper() {}

    static void encodeTextFallBack(
            final Charset charset, final StringBuilder text, final ByteBufferDestination destination) {
        final byte[] bytes = text.toString().getBytes(charset);
        destination.writeBytes(bytes, 0, bytes.length);
    }

    /**
     * Converts the specified text to bytes and writes the resulting bytes to the specified destination.
     * Attempts to postpone synchronizing on the destination as long as possible to minimize lock contention.
     *
     * @param charsetEncoder thread-local encoder instance for converting chars to bytes
     * @param charBuf thread-local text buffer for converting text to bytes
     * @param byteBuf thread-local buffer to temporarily hold converted bytes before copying them to the destination
     * @param text the text to convert and write to the destination
     * @param destination the destination to write the bytes to
     */
    public static void encodeText(
            final CharsetEncoder charsetEncoder,
            final CharBuffer charBuf,
            final ByteBuffer byteBuf,
            final StringBuilder text,
            final ByteBufferDestination destination) {
        charsetEncoder.reset();
        if (text.length() > charBuf.capacity()) {
            encodeChunkedText(charsetEncoder, charBuf, byteBuf, text, destination);
            return;
        }
        charBuf.clear();
        text.getChars(0, text.length(), charBuf.array(), charBuf.arrayOffset());
        charBuf.limit(text.length());
        final CoderResult result = charsetEncoder.encode(charBuf, byteBuf, true);
        writeEncodedText(charsetEncoder, charBuf, byteBuf, destination, result);
    }

    /**
     * This method is called when the CharEncoder has encoded (but not yet flushed) content from the CharBuffer
     * into the ByteBuffer. A CoderResult of UNDERFLOW means that the contents fit into the ByteBuffer and we can move
     * on to the next step, flushing. Otherwise, we need to synchronize on the destination, copy the ByteBuffer to the
     * destination and encode the remainder of the CharBuffer while holding the lock on the destination.
     *
     * @since 2.9
     */
    private static void writeEncodedText(
            final CharsetEncoder charsetEncoder,
            final CharBuffer charBuf,
            final ByteBuffer byteBuf,
            final ByteBufferDestination destination,
            CoderResult result) {
        if (!result.isUnderflow()) {
            writeChunkedEncodedText(charsetEncoder, charBuf, destination, byteBuf, result);
            return;
        }
        result = charsetEncoder.flush(byteBuf);
        if (!result.isUnderflow()) {
            synchronized (destination) {
                flushRemainingBytes(charsetEncoder, destination, byteBuf);
            }
            return;
        }
        // Thread-safety note: no explicit synchronization on ByteBufferDestination below. This is safe, because
        // if the byteBuf is actually the destination's buffer, this method call should be protected with
        // synchronization on the destination object at some level, so the call to destination.getByteBuffer() should
        // be safe. If the byteBuf is an unrelated buffer, the comparison between the buffers should fail despite
        // destination.getByteBuffer() is not protected with the synchronization on the destination object.
        if (byteBuf != destination.getByteBuffer()) {
            byteBuf.flip();
            destination.writeBytes(byteBuf);
            byteBuf.clear();
        }
    }

    /**
     * This method is called when the CharEncoder has encoded (but not yet flushed) content from the CharBuffer
     * into the ByteBuffer and we found that the ByteBuffer is too small to hold all the content.
     * Therefore, we need to synchronize on the destination, copy the ByteBuffer to the
     * destination and encode the remainder of the CharBuffer while holding the lock on the destination.
     *
     * @since 2.9
     */
    private static void writeChunkedEncodedText(
            final CharsetEncoder charsetEncoder,
            final CharBuffer charBuf,
            final ByteBufferDestination destination,
            ByteBuffer byteBuf,
            final CoderResult result) {
        synchronized (destination) {
            byteBuf = writeAndEncodeAsMuchAsPossible(charsetEncoder, charBuf, true, destination, byteBuf, result);
            flushRemainingBytes(charsetEncoder, destination, byteBuf);
        }
    }

    /**
     * This method is called <em>before</em> the CharEncoder has encoded any content from the CharBuffer
     * into the ByteBuffer, but we have already detected that the CharBuffer contents is too large to fit into the
     * ByteBuffer. Therefore, at some point we need to synchronize on the destination, copy the ByteBuffer to the
     * destination and encode the remainder of the CharBuffer while holding the lock on the destination.
     *
     * @since 2.9
     */
    private static void encodeChunkedText(
            final CharsetEncoder charsetEncoder,
            final CharBuffer charBuf,
            ByteBuffer byteBuf,
            final StringBuilder text,
            final ByteBufferDestination destination) {

        // LOG4J2-1874 ByteBuffer, CharBuffer and CharsetEncoder are thread-local, so no need to synchronize while
        // modifying these objects. Postpone synchronization until accessing the ByteBufferDestination.
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
            byteBuf = writeAndEncodeAsMuchAsPossible(charsetEncoder, charBuf, endOfInput, destination, byteBuf, result);
            while (!endOfInput) {
                result = CoderResult.UNDERFLOW;
                while (!endOfInput && result.isUnderflow()) {
                    charBuf.clear();
                    final int copied = copy(text, start, charBuf);
                    start += copied;
                    endOfInput = start >= text.length();
                    charBuf.flip();
                    result = charsetEncoder.encode(charBuf, byteBuf, endOfInput);
                }
                byteBuf = writeAndEncodeAsMuchAsPossible(
                        charsetEncoder, charBuf, endOfInput, destination, byteBuf, result);
            }
            flushRemainingBytes(charsetEncoder, destination, byteBuf);
        }
    }

    /**
     * For testing purposes only.
     */
    @Deprecated
    public static void encodeText(
            final CharsetEncoder charsetEncoder, final CharBuffer charBuf, final ByteBufferDestination destination) {
        charsetEncoder.reset();
        synchronized (destination) {
            ByteBuffer byteBuf = destination.getByteBuffer();
            byteBuf = encodeAsMuchAsPossible(charsetEncoder, charBuf, true, destination, byteBuf);
            flushRemainingBytes(charsetEncoder, destination, byteBuf);
        }
    }

    /**
     * Continues to write the contents of the ByteBuffer to the destination and encode more of the CharBuffer text
     * into the ByteBuffer until the remaining encoded text fit into the ByteBuffer, at which point the ByteBuffer
     * is returned (without flushing the CharEncoder).
     * <p>
     * This method is called when the CharEncoder has encoded (but not yet flushed) content from the CharBuffer
     * into the ByteBuffer and we found that the ByteBuffer is too small to hold all the content.
     * </p><p>
     * Thread-safety note: This method should be called while synchronizing on the ByteBufferDestination.
     * </p>
     * @return the ByteBuffer resulting from draining the temporary ByteBuffer to the destination. In the case
     *          of a MemoryMappedFile, a remap() may have taken place and the returned ByteBuffer is now the
     *          MappedBuffer of the newly mapped region of the memory mapped file.
     * @since 2.9
     */
    private static ByteBuffer writeAndEncodeAsMuchAsPossible(
            final CharsetEncoder charsetEncoder,
            final CharBuffer charBuf,
            final boolean endOfInput,
            final ByteBufferDestination destination,
            ByteBuffer temp,
            CoderResult result) {
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

    // @since 2.9
    private static void throwException(final CoderResult result) {
        try {
            result.throwException();
        } catch (final CharacterCodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ByteBuffer encodeAsMuchAsPossible(
            final CharsetEncoder charsetEncoder,
            final CharBuffer charBuf,
            final boolean endOfInput,
            final ByteBufferDestination destination,
            ByteBuffer temp) {
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

    /**
     * If the CoderResult indicates the ByteBuffer is full, synchronize on the destination and write the content
     * of the ByteBuffer to the destination. If the specified ByteBuffer is owned by the destination, we have
     * reached the end of a MappedBuffer and we call drain() on the destination to remap().
     * <p>
     * If the CoderResult indicates more can be encoded, this method does nothing and returns the temp ByteBuffer.
     * </p>
     *
     * @param destination the destination to write bytes to
     * @param temp the ByteBuffer containing the encoded bytes. May be a temporary buffer or may be the ByteBuffer of
     *              the ByteBufferDestination
     * @param result the CoderResult from the CharsetEncoder
     * @return the ByteBuffer to encode into for the remainder of the text
     */
    private static ByteBuffer drainIfByteBufferFull(
            final ByteBufferDestination destination, final ByteBuffer temp, final CoderResult result) {
        if (result.isOverflow()) { // byte buffer full
            // all callers already synchronize on destination but for safety ensure we are synchronized because
            // below calls to drain() may cause destination to swap in a new ByteBuffer object
            synchronized (destination) {
                final ByteBuffer destinationBuffer = destination.getByteBuffer();
                if (destinationBuffer != temp) {
                    temp.flip();
                    ByteBufferDestinationHelper.writeToUnsynchronized(temp, destination);
                    temp.clear();
                    return destination.getByteBuffer();
                } else {
                    return destination.drain(destinationBuffer);
                }
            }
        } else {
            return temp;
        }
    }

    private static void flushRemainingBytes(
            final CharsetEncoder charsetEncoder, final ByteBufferDestination destination, ByteBuffer temp) {
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
