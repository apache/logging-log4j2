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
package org.apache.logging.log4j.util;

import java.util.Collections;
import java.util.Map;

/**
 * <em>Consider this class private.</em>
 * Empty pre-frozen implementation of the {@link StringMap} interface.
 *
 * @since 2.7.1
 */
public class EmptyFrozenStringMap implements StringMap {

    /**
     * Singleton instance.
     */
    public static final EmptyFrozenStringMap INSTANCE = new EmptyFrozenStringMap();

    private static final String FROZEN = "Frozen collection cannot be modified";

    /**
     * Private default constructor to enforce singleton.
     */
    private EmptyFrozenStringMap() {
    }

    @Override
    public Map<String, String> toMap() {
        return Collections.emptyMap();
    }

    @Override
    public boolean containsKey(String key) {
        return false;
    }

    @Override
    public <V> void forEach(BiConsumer<String, ? super V> action) {
    }

    @Override
    public <V, S> void forEach(TriConsumer<String, ? super V, S> action, S state) {
    }

    @Override
    public <V> V getValue(String key) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public void freeze() {
    }

    @Override
    public boolean isFrozen() {
        return true;
    }

    @Override
    public void putAll(ReadOnlyStringMap source) {
        if (source == this || source.isEmpty()) { // throw NPE if null
            return; // this.putAll(this) does not modify this collection
        }
        throw new UnsupportedOperationException(FROZEN);
    }

    @Override
    public void putValue(String key, Object value) {
        throw new UnsupportedOperationException(FROZEN);
    }

    @Override
    public void remove(String key) {
    }
}
