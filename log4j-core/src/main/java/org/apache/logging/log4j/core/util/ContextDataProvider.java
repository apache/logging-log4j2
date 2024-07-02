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
package org.apache.logging.log4j.core.util;

import java.util.Map;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Source of context data to be added to each log event.
 */
public interface ContextDataProvider {

    /**
     * Returns the key for a value from the context data.
     * @param key the key to locate.
     * @return the value or null if it is not found.
     */
    default String get(String key) {
        return null;
    }

    /**
     * Returns a Map containing context data to be injected into the event or null if no context data is to be added.
     * <p>
     *     Thread-safety note: The returned object can safely be passed off to another thread: future changes in the
     *     underlying context data will not be reflected in the returned object.
     * </p>
     * @return A Map containing the context data or null.
     */
    Map<String, String> supplyContextData();

    /**
     * Returns the context data as a StringMap.
     * <p>
     *     Thread-safety note: The returned object can safely be passed off to another thread: future changes in the
     *     underlying context data will not be reflected in the returned object.
     * </p>
     * @return the context data in a StringMap.
     * @deprecated No longer used since 2.24.0. Will be removed in 3.0.0.
     */
    @Deprecated
    default StringMap supplyStringMap() {
        return new JdkMapAdapterStringMap(supplyContextData(), true);
    }

    /**
     * Returns the number of items in this context.
     * @return the number of items in the context.
     */
    default int size() {
        Map<String, String> contextMap = supplyContextData();
        return contextMap != null ? contextMap.size() : 0;
    }

    /**
     * Add all the keys in the current context to the provided Map.
     * @param map the StringMap to add the keys and values to.
     */
    default void addAll(Map<String, String> map) {
        Map<String, String> contextMap = supplyContextData();
        if (contextMap != null) {
            map.putAll(contextMap);
        }
    }

    /**
     * Add all the keys in the current context to the provided StringMap.
     * @param map the StringMap to add the keys and values to.
     */
    default void addAll(StringMap map) {
        Map<String, String> contextMap = supplyContextData();
        if (contextMap != null) {
            contextMap.forEach(map::putValue);
        }
    }
}
