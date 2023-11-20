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

import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A {@link CharSequence} wrapper that allows mutation of the pointed delegate sequence.
 */
public final class CharSequencePointer implements CharSequence {

    private CharSequence delegate;

    private int startIndex;

    private int length = -1;

    public void reset(final CharSequence delegate, final int startIndex, final int endIndex) {

        // Check & set the delegate.
        Objects.requireNonNull(delegate, "delegate");
        this.delegate = delegate;

        // Check & set the start.
        if (startIndex < 0) {
            throw new IndexOutOfBoundsException("invalid start: " + startIndex);
        }

        // Check & set length.
        if (endIndex > delegate.length()) {
            throw new IndexOutOfBoundsException("invalid end: " + endIndex);
        }
        this.length = Math.subtractExact(endIndex, startIndex);
        if (length < 0) {
            throw new IndexOutOfBoundsException("invalid length: " + length);
        }

        // Set fields.
        this.delegate = delegate;
        this.startIndex = startIndex;
    }

    @Override
    public int length() {
        requireReset();
        return length;
    }

    @Override
    public char charAt(final int startIndex) {
        requireReset();
        final int delegateStartIndex = Math.addExact(this.startIndex, startIndex);
        return delegate.charAt(delegateStartIndex);
    }

    @Override
    public CharSequence subSequence(final int startIndex, final int endIndex) {
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
    public String toString() {
        requireReset();
        final int endIndex = Math.addExact(startIndex, length);
        return delegate.toString().substring(startIndex, endIndex);
    }

    private void requireReset() {
        if (length < 0) {
            throw new IllegalStateException("pointer must be reset first");
        }
    }
}
