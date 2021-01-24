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
package org.apache.logging.log4j.layout.template.json.util;

import java.io.PrintWriter;
import java.util.Objects;

public final class TruncatingBufferedPrintWriter
        extends PrintWriter
        implements CharSequence {

    private final TruncatingBufferedWriter writer;

    private TruncatingBufferedPrintWriter(final TruncatingBufferedWriter writer) {
        super(writer, false);
        this.writer = writer;
    }

    public static TruncatingBufferedPrintWriter ofCapacity(final int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException(
                    "was expecting a non-negative capacity: " + capacity);
        }
        final TruncatingBufferedWriter writer = new TruncatingBufferedWriter(capacity);
        return new TruncatingBufferedPrintWriter(writer);
    }

    public char[] buffer() {
        return writer.buffer();
    }

    public int position() {
        return writer.position();
    }

    public void position(final int index) {
        writer.position(index);
    }

    public int capacity() {
        return writer.capacity();
    }

    public boolean truncated() {
        return writer.truncated();
    }

    public int indexOf(final CharSequence seq) {
        Objects.requireNonNull(seq, "seq");
        return writer.indexOf(seq);
    }

    @Override
    public int length() {
        return writer.length();
    }

    @Override
    public char charAt(final int index) {
        return writer.charAt(index);
    }

    @Override
    public CharSequence subSequence(final int startIndex, final int endIndex) {
        return writer.subSequence(startIndex, endIndex);
    }

    @Override
    public void close() {
        writer.close();
    }

    @Override
    public String toString() {
        return writer.toString();
    }

}
