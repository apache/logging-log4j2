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

import java.io.Serializable;
import java.util.Map;

/**
 * A read-only collection of String keys mapped to values of arbitrary type.
 *
 * @since 2.7
 */
public interface ReadOnlyStringMap extends Serializable {

    /**
     * Returns a non-{@code null} mutable {@code Map<String, String>} containing a snapshot of this data structure.
     *
     * @return a mutable copy of this data structure in {@code Map<String, String>} form.
     */
    Map<String, String> toMap();

    /**
     * Returns {@code true} if this data structure contains the specified key, {@code false} otherwise.
     *
     * @param key the key whose presence to check. May be {@code null}.
     * @return {@code true} if this data structure contains the specified key, {@code false} otherwise.
     */
    boolean containsKey(String key);

    /**
     * Performs the given action for each key-value pair in this data structure
     * until all entries have been processed or the action throws an exception.
     * <p>
     * Some implementations may not support structural modifications (adding new elements or removing elements) while
     * iterating over the contents. In such implementations, attempts to add or remove elements from the
     * {@code BiConsumer}'s {@link BiConsumer#accept(Object, Object)} accept} method may cause a
     * {@code ConcurrentModificationException} to be thrown.
     * </p>
     *
     * @param action The action to be performed for each key-value pair in this collection.
     * @param <V> type of the value.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     */
    <V> void forEach(final BiConsumer<String, ? super V> action);

    /**
     * Performs the given action for each key-value pair in this data structure
     * until all entries have been processed or the action throws an exception.
     * <p>
     * The third parameter lets callers pass in a stateful object to be modified with the key-value pairs,
     * so the TriConsumer implementation itself can be stateless and potentially reusable.
     * </p>
     * <p>
     * Some implementations may not support structural modifications (adding new elements or removing elements) while
     * iterating over the contents. In such implementations, attempts to add or remove elements from the
     * {@code TriConsumer}'s {@link TriConsumer#accept(Object, Object, Object) accept} method may cause a
     * {@code ConcurrentModificationException} to be thrown.
     * </p>
     *
     * @param action The action to be performed for each key-value pair in this collection.
     * @param state the object to be passed as the third parameter to each invocation on the specified
     *          triconsumer.
     * @param <V> type of the value.
     * @param <S> type of the third parameter.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     */
    <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state);

    /**
     * Returns the value for the specified key, or {@code null} if the specified key does not exist in this collection.
     *
     * @param key the key whose value to return.
     * @return the value for the specified key or {@code null}.
     */
    <V> V getValue(final String key);

    /**
     * Returns {@code true} if this collection is empty (size is zero), {@code false} otherwise.
     * @return {@code true} if this collection is empty (size is zero).
     */
    boolean isEmpty();

    /**
     * Returns the number of key-value pairs in this collection.
     *
     * @return the number of key-value pairs in this collection.
     */
    int size();
}
