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
package org.apache.logging.log4j.util;

/**
 * An extension of {@code ReadOnlyStringMap} that imposes a total ordering on its keys.
 * The map is ordered according to the natural ordering of its keys. This order is reflected when
 * {@link #forEach(BiConsumer) consuming} the key-value pairs with a {@link BiConsumer} or a {@link TriConsumer}.
 * <p>
 * This interface views all key-value pairs as a sequence ordered by key, and allows
 * keys and values to be accessed by their index in the sequence.
 * </p>
 *
 * @see ReadOnlyStringMap
 * @since 2.8
 */
public interface IndexedReadOnlyStringMap extends ReadOnlyStringMap {

    /**
     * Viewing all key-value pairs as a sequence sorted by key, this method returns the key at the specified index,
     * or {@code null} if the specified index is less than zero or greater or equal to the size of this collection.
     *
     * @param index the index of the key to return
     * @return the key at the specified index or {@code null}
     */
    String getKeyAt(final int index);

    /**
     * Viewing all key-value pairs as a sequence sorted by key, this method returns the value at the specified index,
     * or {@code null} if the specified index is less than zero or greater or equal to the size of this collection.
     *
     * @param index the index of the value to return
     * @return the value at the specified index or {@code null}
     */
    <V> V getValueAt(final int index);

    /**
     * Viewing all key-value pairs as a sequence sorted by key, this method returns the index of the specified key in
     * that sequence. If the specified key is not found, this method returns {@code (-(insertion point) - 1)}.
     *
     * @param key the key whose index in the ordered sequence of keys to return
     * @return the index of the specified key or {@code (-(insertion point) - 1)} if the key is not found.
     *          The insertion point is defined as the point at which the key would be inserted into the array:
     *          the index of the first element in the range greater than the key, or {@code size()} if all elements
     *          are less than the specified key. Note that this guarantees that the return value will be &gt;= 0
     *          if and only if the key is found.
     */
    int indexOfKey(final String key);
}
