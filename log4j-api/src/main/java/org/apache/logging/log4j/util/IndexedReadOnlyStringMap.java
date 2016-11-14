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
package org.apache.logging.log4j.util;

/**
 * An extension of {@code ReadOnlyStringMap} that views all key-value pairs as a sequence ordered by key, and allows
 * keys and values to be accessed by their index in the sequence.
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

}
