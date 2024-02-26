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
package org.apache.logging.log4j.spi;

import java.util.Map;

/**
 * Extension service provider interface to allow putting Object values in the
 * {@link org.apache.logging.log4j.ThreadContext}.
 *
 * @see ThreadContextMap
 * @since 2.8
 */
public interface ObjectThreadContextMap extends CleanableThreadContextMap {

    /**
     * Returns the Object value for the specified key, or {@code null} if the specified key does not exist in this
     * collection.
     *
     * @param key the key whose value to return
     * @return the value for the specified key or {@code null}
     */
    <V> V getValue(String key);

    /**
     * Puts the specified key-value pair into the collection.
     *
     * @param key the key to add or remove. Keys may be {@code null}.
     * @param value the value to add. Values may be {@code null}.
     */
    <V> void putValue(String key, V value);

    /**
     * Puts all given key-value pairs into the collection.
     *
     * @param values the map of key-value pairs to add
     */
    <V> void putAllValues(Map<String, V> values);
}
