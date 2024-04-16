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
package org.apache.logging.slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.apache.logging.log4j.spi.CleanableThreadContextMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * Bind the ThreadContextMap to the SLF4J MDC.
 */
@NullMarked
public class MDCContextMap implements CleanableThreadContextMap {

    private static final StringMap EMPTY_CONTEXT_DATA = new SortedArrayStringMap(1);

    static {
        EMPTY_CONTEXT_DATA.freeze();
    }

    private final MDCAdapter mdc;

    public MDCContextMap() {
        this(MDC.getMDCAdapter());
    }

    MDCContextMap(final MDCAdapter mdc) {
        this.mdc = mdc;
    }

    @Override
    public void put(final String key, final @Nullable String value) {
        mdc.put(key, value);
    }

    @Override
    public void putAll(final Map<String, String> m) {
        for (final Entry<String, String> entry : m.entrySet()) {
            mdc.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public @Nullable String get(final String key) {
        return mdc.get(key);
    }

    @Override
    public void remove(final String key) {
        mdc.remove(key);
    }

    @Override
    public void removeAll(final Iterable<String> keys) {
        for (final String key : keys) {
            mdc.remove(key);
        }
    }

    @Override
    public void clear() {
        mdc.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object setContextData(final Object contextMap) {
        final Object current = getContextData();
        final Map<String, String> map = Objects.requireNonNull((Map<String, String>) contextMap);
        if (map.isEmpty()) {
            mdc.clear();
        } else {
            mdc.setContextMap((Map<String, String>) contextMap);
        }
        return current;
    }

    @Override
    public boolean containsKey(final String key) {
        final Map<String, String> map = mdc.getCopyOfContextMap();
        return map != null && map.containsKey(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final Map<String, String> contextMap = mdc.getCopyOfContextMap();
        return contextMap != null ? contextMap : new HashMap<>();
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return mdc.getCopyOfContextMap();
    }

    @Override
    public boolean isEmpty() {
        final Map<String, String> map = mdc.getCopyOfContextMap();
        return map == null || map.isEmpty();
    }

    @Override
    public StringMap getReadOnlyContextData() {
        final Map<String, String> copy = getCopy();
        if (copy.isEmpty()) {
            return EMPTY_CONTEXT_DATA;
        }
        final StringMap result = new SortedArrayStringMap();
        for (final Entry<String, String> entry : copy.entrySet()) {
            result.putValue(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
