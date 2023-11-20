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
package org.apache.logging.log4j.layout.template.json.util;

import java.io.Writer;
import java.util.Objects;
import java.util.stream.IntStream;

final class TruncatingBufferedWriter extends Writer implements CharSequence {

    private final char[] buffer;

    private int position;

    private boolean truncated;

    TruncatingBufferedWriter(final int capacity) {
        this.buffer = new char[capacity];
        this.position = 0;
        this.truncated = false;
    }

    char[] buffer() {
        return buffer;
    }

    int position() {
        return position;
    }

    void position(final int index) {
        if (index < 0 || index >= buffer.length) {
            throw new IllegalArgumentException("invalid index: " + index);
        }
        position = index;
    }

    int capacity() {
        return buffer.length;
    }

    boolean truncated() {
        return truncated;
    }

    @Override
    public void write(final int c) {
        if (position < buffer.length) {
            buffer[position++] = (char) c;
        } else {
            truncated = true;
        }
    }

    @Override
    public void write(final char[] source) {
        Objects.requireNonNull(source, "source");
        write(source, 0, source.length);
    }

    @Override
    public void write(final char[] source, final int offset, final int length) {

        // Check arguments.
        Objects.requireNonNull(source, "source");
        if (offset < 0 || offset >= source.length) {
            throw new IndexOutOfBoundsException("invalid offset: " + offset);
        }
        if (length < 0 || Math.addExact(offset, length) > source.length) {
            throw new IndexOutOfBoundsException("invalid length: " + length);
        }

        // If input fits as is.
        final int maxLength = buffer.length - position;
        if (length < maxLength) {
            System.arraycopy(source, offset, buffer, position, length);
            position += length;
        }

        // If truncation is possible.
        else if (maxLength > 0) {
            System.arraycopy(source, offset, buffer, position, maxLength);
            position += maxLength;
            truncated = true;
        }
    }

    @Override
    public void write(final String string) {

        // Check arguments.
        Objects.requireNonNull(string, "string");
        final int length = string.length();
        final int maxLength = buffer.length - position;

        // If input fits as is.
        if (length < maxLength) {
            string.getChars(0, length, buffer, position);
            position += length;
        }

        // If truncation is possible.
        else if (maxLength > 0) {
            string.getChars(0, maxLength, buffer, position);
            position += maxLength;
            truncated = true;
        }
    }

    @Override
    public void write(final String string, final int offset, final int length) {

        // Check arguments.
        Objects.requireNonNull(string, "string");
        if (offset < 0 || offset >= string.length()) {
            throw new IndexOutOfBoundsException("invalid offset: " + offset);
        }
        if (length < 0 || Math.addExact(offset, length) > string.length()) {
            throw new IndexOutOfBoundsException("invalid length: " + length);
        }

        // If input fits as is.
        final int maxLength = buffer.length - position;
        if (length < maxLength) {
            string.getChars(offset, offset + length, buffer, position);
            position += length;
        }

        // If truncation is possible.
        else if (maxLength > 0) {
            string.getChars(offset, offset + maxLength, buffer, position);
            position += maxLength;
            truncated = true;
        }
    }

    @Override
    public Writer append(final char c) {
        write(c);
        return this;
    }

    @Override
    public Writer append(final CharSequence seq) {
        return seq == null ? append("null", 0, 4) : append(seq, 0, seq.length());
    }

    @Override
    public Writer append(final CharSequence seq, final int start, final int end) {

        // Short-circuit on null sequence.
        if (seq == null) {
            write("null");
            return this;
        }

        // Check arguments.
        if (start < 0 || start >= seq.length()) {
            throw new IndexOutOfBoundsException("invalid start: " + start);
        }
        if (end < start || end > seq.length()) {
            throw new IndexOutOfBoundsException("invalid end: " + end);
        }

        // If input fits as is.
        final int length = end - start;
        final int maxLength = buffer.length - position;
        if (length < maxLength) {
            for (int i = start; i < end; i++) {
                final char c = seq.charAt(i);
                buffer[position++] = c;
            }
        }

        // If truncation is possible.
        else if (maxLength > 0) {
            final int truncatedEnd = start + maxLength;
            for (int i = start; i < truncatedEnd; i++) {
                final char c = seq.charAt(i);
                buffer[position++] = c;
            }
            truncated = true;
        }
        return this;
    }

    @Override
    public int length() {
        return position;
    }

    @Override
    public char charAt(final int index) {
        return buffer[index];
    }

    @Override
    public String subSequence(final int startIndex, final int endIndex) {
        throw new UnsupportedOperationException(
                "operation requires allocation, contradicting with the purpose of the class");
    }

    @Override
    public IntStream chars() {
        throw new UnsupportedOperationException(
                "operation requires allocation, contradicting with the purpose of the class");
    }

    @Override
    public IntStream codePoints() {
        throw new UnsupportedOperationException(
                "operation requires allocation, contradicting with the purpose of the class");
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        position = 0;
        truncated = false;
    }

    @Override
    public String toString() {
        return new String(buffer, 0, position);
    }
}
