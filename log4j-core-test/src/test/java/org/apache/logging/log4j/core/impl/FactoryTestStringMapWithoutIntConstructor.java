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
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * Dummy implementation of the StringMap interface for testing.
 */
public class FactoryTestStringMapWithoutIntConstructor implements StringMap {
    private static final long serialVersionUID = -3239395494628445052L;
    int initialCapacity;

    public FactoryTestStringMapWithoutIntConstructor() {}

    @Override
    public Map<String, String> toMap() {
        return null;
    }

    @Override
    public boolean containsKey(final String key) {
        return false;
    }

    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {}

    @Override
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {}

    @Override
    public <V> V getValue(final String key) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {}

    @Override
    public void freeze() {}

    @Override
    public boolean isFrozen() {
        return false;
    }

    @Override
    public void putAll(final ReadOnlyStringMap source) {}

    @Override
    public void putValue(final String key, final Object value) {}

    @Override
    public void remove(final String key) {}

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ReadOnlyStringMap)) {
            return false;
        }
        if (size() != ((ReadOnlyStringMap) obj).size()) {
            return false;
        }

        // Convert to maps and compare
        final Map<String, String> thisMap = toMap();
        final Map<String, String> otherMap = ((ReadOnlyStringMap) obj).toMap();
        return thisMap.equals(otherMap);
    }

    @Override
    public int hashCode() {
        return toMap().hashCode();
    }
}
