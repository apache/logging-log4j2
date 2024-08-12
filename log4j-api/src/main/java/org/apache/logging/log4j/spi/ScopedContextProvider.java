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
import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.spi.internal.DefaultScopedContextProvider;
import org.apache.logging.log4j.util.StringMap;

/**
 * The service underlying {@link ScopedContext}.
 * @since 2.24.0
 */
public interface ScopedContextProvider {

    static ScopedContextProvider simple() {
        return DefaultScopedContextProvider.INSTANCE;
    }

    /**
     * @return An immutable map with the current context data.
     */
    Map<String, ?> getContextMap();

    /**
     * Adds the current data to the provided {@link StringMap}.
     * @param map The {@link StringMap} to add data to.
     */
    default void addContextMapTo(final StringMap map) {
        getContextMap().forEach(map::putValue);
    }

    /**
     * Return the value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    Object getValue(String key);

    /**
     * Return the value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext converted to {@link String}.
     */
    String getString(String key);

    /**
     * Creates a new context containing the current context data from {@link org.apache.logging.log4j.ThreadContext}.
     * @param key An additional key for the context.
     * @param value An additional value for the context.
     * @return A new instance of a scoped context.
     */
    ScopedContext.Instance newScopedContext(String key, Object value);

    /**
     * Creates a new context containing the current context data from {@link org.apache.logging.log4j.ThreadContext}.
     * @param map Additional data to include in the context.
     * @return A new instance of a scoped context.
     */
    ScopedContext.Instance newScopedContext(Map<String, ?> map);

    /**
     * Creates a new context indicating that the ThreadContext should be included.
     * @param withThreadContext true if the ThreadContext should be included.
     * @return A new instance of a scoped context.
     */
    ScopedContext.Instance newScopedContext(boolean withThreadContext);
}
