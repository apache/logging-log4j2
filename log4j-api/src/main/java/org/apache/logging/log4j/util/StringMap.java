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
 * Exposes methods to add and remove key-value pairs to and from {@code ReadOnlyStringMap}.
 *
 * @see ReadOnlyStringMap
 * @since 2.7
 */
public interface StringMap extends ReadOnlyStringMap {

    /**
     * Removes all key-value pairs from this collection.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     * @throws UnsupportedOperationException if this collection has been {@linkplain #isFrozen() frozen}.
     */
    void clear();

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj
     *            the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
     * @see #hashCode()
     */
    @Override
    boolean equals(final Object obj);

    /**
     * Makes this collection immutable. Attempts to modify the collection after the {@code freeze()} method was called
     * will result in an {@code UnsupportedOperationException} being thrown.
     */
    void freeze();

    /**
     * Returns a hash code value for the object.
     * @return a hash code value for this object.
     */
    @Override
    int hashCode();

    /**
     * Returns {@code true} if this object has been {@linkplain #freeze() frozen}, {@code false} otherwise.
     * @return  {@code true} if this object has been {@linkplain #freeze() frozen}, {@code false} otherwise
     */
    boolean isFrozen();

    /**
     * Copies all key-value pairs from the specified {@code ReadOnlyStringMap} into this {@code StringMap}.
     * @param source the {@code ReadOnlyStringMap} to copy key-value pairs from
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     * @throws UnsupportedOperationException if this collection has been {@linkplain #isFrozen() frozen}.
     */
    void putAll(final ReadOnlyStringMap source);

    /**
     * Puts the specified key-value pair into the collection.
     *
     * @param key the key to add or remove. Keys may be {@code null}.
     * @param value the value to add. Values may be {@code null}.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     * @throws UnsupportedOperationException if this collection has been {@linkplain #isFrozen() frozen}.
     */
    void putValue(final String key, final Object value);

    /**
     * Removes the key-value pair for the specified key from this data structure.
     *
     * @param key the key to remove. May be {@code null}.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this data structure while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     * @throws UnsupportedOperationException if this collection has been {@linkplain #isFrozen() frozen}.
     */
    void remove(final String key);
}
