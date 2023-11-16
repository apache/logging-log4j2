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
package org.apache.logging.log4j.core.impl;

import java.util.Map;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * Dummy implementation of the StringMap interface for testing.
 */
public class FactoryTestStringMap implements IndexedStringMap {
    private static final long serialVersionUID = -2035823164390218862L;
    int initialCapacity;

    public FactoryTestStringMap() {}

    public FactoryTestStringMap(final int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    @Override
    public void clear() {
        // do nothing
    }

    @Override
    public boolean containsKey(final String key) {
        return false;
    }

    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        // do nothing
    }

    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        // do nothing
    }

    @Override
    public void freeze() {
        // do nothing
    }

    @Override
    public String getKeyAt(final int index) {
        return null;
    }

    @Override
    public <V> V getValue(final String key) {
        return null;
    }

    @Override
    public <V> V getValueAt(final int index) {
        return null;
    }

    @Override
    public int indexOfKey(final String key) {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isFrozen() {
        return false;
    }

    @Override
    public void putAll(final ReadOnlyStringMap source) {
        // do nothing
    }

    @Override
    public void putValue(final String key, final Object value) {
        // do nothing
    }

    @Override
    public void remove(final String key) {
        // do nothing
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Map<String, String> toMap() {
        return null;
    }
}
