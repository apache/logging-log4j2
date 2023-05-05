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
package org.apache.logging.log4j.core.util;

import java.lang.reflect.Array;

/**
 * A bounded buffer containing elements of type T. When the number of elements to be added will exceed the
 * size of the buffer the oldest element will be overwritten. Access to the buffer is thread safe.
 * @param <T> The type of object stored in the buffer.
 */
public final class CyclicBuffer<T> {
    private final T[] ring;
    private int first = 0;
    private int last = 0;
    private int numElems = 0;
    private final Class<T> clazz;

    /**
     * Instantiates a new CyclicBuffer of at most <code>maxSize</code> events.
     * @param clazz The Class associate with the type of object in the buffer.
     * @param size The number of items in the buffer.
     * @throws IllegalArgumentException if the size is negative.
     */
    public CyclicBuffer(final Class<T> clazz, final int size) throws IllegalArgumentException {
        if (size < 0) {
            throw new IllegalArgumentException("The maxSize argument (" + size + ") cannot be negative.");
        }
        this.ring = makeArray(clazz, size);
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    private T[] makeArray(final Class<T> cls, final int size) {
        return (T[]) Array.newInstance(cls, size);
    }

    /**
     * Adds an item as the last event in the buffer.
     * @param item The item to add to the buffer.
     */
    public synchronized void add(final T item) {
        if (ring.length > 0) {
            ring[last] = item;
            if (++last == ring.length) {
                last = 0;
            }

            if (numElems < ring.length) {
                numElems++;
            } else if (++first == ring.length) {
                first = 0;
            }
        }
    }

    /**
     * Removes all the elements from the buffer and returns them.
     * @return An array of the elements in the buffer.
     */
    public synchronized T[] removeAll() {
        final T[] array = makeArray(clazz, numElems);
        int index = 0;
        while (numElems > 0) {
            numElems--;
            array[index++] = ring[first];
            ring[first] = null;
            if (++first == ring.length) {
                first = 0;
            }
        }
        return array;
    }

    /**
     * Determines if the buffer contains elements.
     * @return true if the buffer is empty, false otherwise.
     */
    public boolean isEmpty() {
        return 0 == numElems;
    }
}
