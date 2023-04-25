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
package org.apache.log4j.helpers;

import org.apache.log4j.spi.LoggingEvent;

/**
 * Holds {@link LoggingEvent LoggingEvents} for immediate or differed display.
 *
 * <p>
 * This buffer gives read access to any element in the buffer not just the first or last element.
 * </p>
 *
 * @since 0.9.0
 */
public class CyclicBuffer {

    LoggingEvent[] ea;
    int first;
    int last;
    int numElems;
    int maxSize;

    /**
     * Constructs a new instance of at most <code>maxSize</code> events.
     *
     * The <code>maxSize</code> argument must a positive integer.
     *
     * @param maxSize The maximum number of elements in the buffer.
     */
    public CyclicBuffer(final int maxSize) throws IllegalArgumentException {
        if (maxSize < 1) {
            throw new IllegalArgumentException("The maxSize argument (" + maxSize + ") is not a positive integer.");
        }
        this.maxSize = maxSize;
        ea = new LoggingEvent[maxSize];
        first = 0;
        last = 0;
        numElems = 0;
    }

    /**
     * Adds an <code>event</code> as the last event in the buffer.
     */
    public void add(final LoggingEvent event) {
        ea[last] = event;
        if (++last == maxSize) {
            last = 0;
        }

        if (numElems < maxSize) {
            numElems++;
        } else if (++first == maxSize) {
            first = 0;
        }
    }

    /**
     * Gets the oldest (first) element in the buffer. The oldest element is removed from the buffer.
     */
    public LoggingEvent get() {
        LoggingEvent r = null;
        if (numElems > 0) {
            numElems--;
            r = ea[first];
            ea[first] = null;
            if (++first == maxSize) {
                first = 0;
            }
        }
        return r;
    }

    /**
     * Gets the <i>i</i>th oldest event currently in the buffer. If <em>i</em> is outside the range 0 to the number of
     * elements currently in the buffer, then <code>null</code> is returned.
     */
    public LoggingEvent get(final int i) {
        if (i < 0 || i >= numElems) {
            return null;
        }

        return ea[(first + i) % maxSize];
    }

    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Gets the number of elements in the buffer. This number is guaranteed to be in the range 0 to <code>maxSize</code>
     * (inclusive).
     */
    public int length() {
        return numElems;
    }

    /**
     * Resizes the cyclic buffer to <code>newSize</code>.
     *
     * @throws IllegalArgumentException if <code>newSize</code> is negative.
     */
    public void resize(final int newSize) {
        if (newSize < 0) {
            throw new IllegalArgumentException("Negative array size [" + newSize + "] not allowed.");
        }
        if (newSize == numElems) {
            return; // nothing to do
        }

        final LoggingEvent[] temp = new LoggingEvent[newSize];

        final int loopLen = newSize < numElems ? newSize : numElems;

        for (int i = 0; i < loopLen; i++) {
            temp[i] = ea[first];
            ea[first] = null;
            if (++first == numElems) {
                first = 0;
            }
        }
        ea = temp;
        first = 0;
        numElems = loopLen;
        maxSize = newSize;
        if (loopLen == newSize) {
            last = 0;
        } else {
            last = loopLen;
        }
    }
}
