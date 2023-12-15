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
package org.apache.logging.log4j.internal.recycler;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.stream.IntStream;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.logging.log4j.util.InternalApi;

/**
 * An array-backed, fixed-length, not-thread-safe {@link java.util.Queue} implementation.
 *
 * @param <E> the element type
 */
@InternalApi
@NotThreadSafe
final class ArrayQueue<E> extends AbstractQueue<E> {

    private final E[] buffer;

    private int head;

    private int tail;

    private int size;

    @SuppressWarnings("unchecked")
    ArrayQueue(final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("invalid capacity: " + capacity);
        }
        buffer = (E[]) new Object[capacity];
        head = 0;
        tail = -1;
        size = 0;
    }

    @Override
    public Iterator<E> iterator() {
        int[] i = {head};
        return IntStream.range(0, size)
                .mapToObj(ignored -> {
                    final E item = buffer[i[0]];
                    i[0] = (i[0] + 1) % buffer.length;
                    return item;
                })
                .iterator();
    }

    @Override
    public boolean offer(final E item) {
        if (size == buffer.length) {
            return false;
        }
        tail = (tail + 1) % buffer.length;
        buffer[tail] = item;
        size++;
        return true;
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        final E item = buffer[head];
        buffer[head] = null; // Clear refs for GC
        head = (head + 1) % buffer.length;
        size--;
        return item;
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return buffer[head];
    }

    @Override
    public int size() {
        return size;
    }
}
