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
package org.apache.logging.log4j.core;

import java.io.Serializable;
import java.util.Map;

import org.apache.logging.log4j.core.util.BiConsumer;
import org.apache.logging.log4j.core.util.TriConsumer;

/**
 * A read-only collection of context data. Context data items are String keys and values of arbitrary type that are
 * set by the application to be included in all subsequent log events. A typical source of context data is the
 * {@code ThreadContextMap} and the {@code Properties} defined in the configuration.
 * <p>
 * Applications can put custom data in this collection by installing a custom {@code ContextDataInjector}.
 * </p>
 *
 * @see org.apache.logging.log4j.spi.ThreadContextMap
 * @see org.apache.logging.log4j.core.config.Property
 * @see org.apache.logging.log4j.core.impl.ContextDataInjector
 * @see org.apache.logging.log4j.core.impl.ContextDataInjectorFactory
 * @since 2.7
 */
public interface ContextData extends Serializable {
    /**
     * Returns a map view of this context data.
     * Called to implement {@link LogEvent#getContextMap()}.
     *
     * @return a map view of this context data
     */
    Map<String, String> asMap();

    /**
     * Returns {@code true} if this context data contains the specified key, {@code false} otherwise.
     *
     * @param key the key whose presence to check. May be {@code null}.
     * @return {@code true} if this context data contains the specified key, {@code false} otherwise
     */
    boolean containsKey(String key);

    /**
     * Performs the given action for each key-value pair in this data structure
     * until all entries have been processed or the action throws an exception.
     *
     * @param action The action to be performed for each key-value pair in this collection
     * @param <V> type of the value
     */
    <V> void forEach(final BiConsumer<String, ? super V> action);

    /**
     * Performs the given action for each key-value pair in this data structure
     * until all entries have been processed or the action throws an exception.
     * <p>
     * The third parameter lets callers pass in a stateful object to be modified with the key-value pairs,
     * so the TriConsumer implementation itself can be stateless and potentially reusable.
     * </p>
     *
     * @param action The action to be performed for each key-value pair in this collection
     * @param state the object to be passed as the third parameter to each invocation on the specified
     *          triconsumer
     * @param <V> type of the value
     * @param <S> type of the third parameter
     */
    <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state);

    /**
     * Returns the value for the specified key, or {@code null} if the specified key does not exist in this collection.
     *
     * @param key the key whose value to return
     * @return the value for the specified key or {@code null}
     */
    <V> V getValue(final String key);

    /**
     * Returns {@code true} if this collection is empty (size is zero), {@code false} otherwise.
     * @return {@code true} if this collection is empty (size is zero)
     */
    boolean isEmpty();

    /**
     * Returns the number of key-value pairs in this collection.
     *
     * @return the number of key-value pairs in this collection
     */
    int size();
}
