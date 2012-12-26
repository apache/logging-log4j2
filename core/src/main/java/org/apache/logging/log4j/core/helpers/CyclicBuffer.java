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
package org.apache.logging.log4j.core.helpers;

import java.lang.reflect.Array;

/**
 * A bounded buffer containing elements of type T. When the number of elements to be added will exceed the
 * size of the buffer the oldest element will be overwritten. Access to the buffer is thread safe.
 */
public class CyclicBuffer<T> {
    private T[] ring;
    private int first = 0;
    private int last = 0;
    private int numElems = 0;
    private final Class<T> clazz;

    /**
     * Instantiate a new CyclicBuffer of at most <code>maxSize</code>
     * events.
     */
    public CyclicBuffer(Class<T> clazz, int size) throws IllegalArgumentException {
        if (size < 1) {
            throw new IllegalArgumentException("The maxSize argument (" + size + ") is not a positive integer.");
        }
        this.ring = makeArray(clazz, size);
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    private T[] makeArray(Class<T> clazz, int size) {
        return (T[]) Array.newInstance(clazz, size);
    }

    /**
     * Add an item as the last event in the buffer.
     */
    public synchronized void add(T item) {
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

    public synchronized T[] removeAll() {
        T[] array = makeArray(clazz, numElems);
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

    public boolean isEmpty() {
        return 0 == numElems;
    }
}
