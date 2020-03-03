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
package org.apache.logging.log4j.layout.json.template.util;

import java.io.Writer;

public final class BufferedWriter extends Writer {

    private final char[] buffer;

    private int position;

    BufferedWriter(final int capacity) {
        this.buffer = new char[capacity];
        this.position = 0;
    }

    char[] getBuffer() {
        return buffer;
    }

    int getPosition() {
        return position;
    }

    int getCapacity() {
        return buffer.length;
    }

    @Override
    public void write(final int c) {
        buffer[position++] = (char) c;
    }

    @Override
    public void write(final char[] source) {
        write(source, 0, source.length);
    }

    @Override
    public void write(final String string) {
        final int length = string.length();
        string.getChars(0, length, buffer, position);
        position += length;
    }

    @Override
    public void write(
            final String string,
            final int offset,
            final int length) {
        string.getChars(offset, offset + length, buffer, position);
        position += length;
    }

    @Override
    public Writer append(final CharSequence seq) {
        return append(seq, 0, seq.length());
    }

    @Override
    public Writer append(
            final CharSequence seq,
            final int start,
            final int end) {
        for (int i = start; i < end; i++) {
            final char c = seq.charAt(i);
            write(c);
        }
        return this;
    }

    @Override
    public Writer append(char c) {
        write(c);
        return this;
    }

    @Override
    public void write(final char[] source, final int offset, final int length) {
        System.arraycopy(source, offset, buffer, position, length);
        position += length;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        position = 0;
    }

}
