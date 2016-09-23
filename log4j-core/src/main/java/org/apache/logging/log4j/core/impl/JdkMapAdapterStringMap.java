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

import java.util.Map;

import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * Provides a read-only {@code StringMap} view of a {@code Map<String, String>}.
 */
class JdkMapAdapterStringMap implements StringMap {
    private static final long serialVersionUID = -7348247784983193612L;
    private Map<String, String> map;

    public JdkMapAdapterStringMap(final Map<String, String> map) {
        this.map = map;
    }

    @Override
    public Map<String, String> toMap() {
        return map;
    }

    @Override
    public boolean containsKey(final String key) {
        return map.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            action.accept(entry.getKey(), (V) entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            action.accept(entry.getKey(), (V) entry.getValue(), state);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValue(final String key) {
        return (V) map.get(key);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void clear() {
        fail();
    }

    @Override
    public void freeze() {
    }

    @Override
    public boolean isFrozen() {
        return true;
    }

    @Override
    public void putAll(final ReadOnlyStringMap source) {
        fail();
    }

    @Override
    public void putValue(final String key, final Object value) {
        fail();
    }

    @Override
    public void remove(final String key) {
        fail();
    }

    private void fail() {
        throw new UnsupportedOperationException("This is a read-only data structure");
    }
}
