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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.core.ContextData;
import org.apache.logging.log4j.core.util.BiConsumer;
import org.apache.logging.log4j.core.util.TriConsumer;

/**
 * Exposes methods to add and remove key-value pairs to and from {@code ContextData}.
 *
 * @see ContextData
 * @since 2.7
 */
public interface MutableContextData extends ContextData {

    /**
     * Removes all key-value pairs from this collection.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this context data while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     */
    void clear();

    /**
     * Puts the specified key-value pair into the collection.
     *
     * @param key the key to add or remove. Keys may be {@code null}.
     * @param value the value to add. Values may be {@code null}.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this context data while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     */
    void putValue(final String key, final Object value);

    /**
     * Copy all key-value pairs from the specified {@code ContextData} into this {@code MutableContextData}.
     * @param source the {@code ContextData} to copy key-value pairs from
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this context data while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     */
    void putAll(final ContextData source);

    /**
     * Removes the key-value pair for the specified key from this context data collection.
     *
     * @param key the key to remove. May be {@code null}.
     * @throws java.util.ConcurrentModificationException some implementations may not support structural modifications
     *          to this context data while iterating over the contents with {@link #forEach(BiConsumer)} or
     *          {@link #forEach(TriConsumer, Object)}.
     */
    void remove(final String key);
}