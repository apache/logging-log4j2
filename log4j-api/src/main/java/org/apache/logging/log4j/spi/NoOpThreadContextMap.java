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
package org.apache.logging.log4j.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code ThreadContextMap} implementation used when either of system properties
 * {@value LoggingSystemProperties#THREAD_CONTEXT_MAP_DISABLED} or
 * {@value LoggingSystemProperties#THREAD_CONTEXT_DISABLED} is {@code true}. This implementation does nothing.
 *
 * @since 2.7
 */
public class NoOpThreadContextMap implements ThreadContextMap {
    @Override
    public void clear() {
    }

    @Override
    public boolean containsKey(final String key) {
        return false;
    }

    @Override
    public String get(final String key) {
        return null;
    }

    @Override
    public Map<String, String> getCopy() {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void put(final String key, final String value) {
    }

    @Override
    public void remove(final String key) {
    }

    @Override
    public void putAll(final Map<String, String> map) {
    }

    @Override
    public void removeAll(final Iterable<String> keys) {
    }

    @Override
    public <V> V getValue(final String key) {
        return null;
    }

    @Override
    public <V> void putValue(final String key, final V value) {
    }

    @Override
    public <V> void putAllValues(final Map<String, V> values) {
    }
}
