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
package org.apache.logging.log4j.core.context;

import java.util.Map;
import org.apache.logging.log4j.spi.CopyOnWrite;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.StringMap;
import org.jspecify.annotations.Nullable;

/**
 * {@code SortedArrayStringMap}-based implementation of the {@code ThreadContextMap} interface that creates a copy of
 * the data structure on every modification. Any particular instance of the data structure is a snapshot of the
 * ThreadContext at some point in time and can safely be passed off to other threads.  Since it is
 * expected that the Map will be passed to many more log events than the number of keys it contains the performance
 * should be much better than if the Map was copied for each event.
 *
 * @since 2.7
 */
public class CopyOnWriteSortedArrayThreadContextMap extends AbstractSortedArrayThreadContextMap implements CopyOnWrite {

    public CopyOnWriteSortedArrayThreadContextMap() {
        this(PropertiesUtil.getProperties());
    }

    CopyOnWriteSortedArrayThreadContextMap(final PropertiesUtil properties) {
        super(properties);
    }

    @Override
    protected @Nullable StringMap copyStringMap(@Nullable StringMap value) {
        if (value != null) {
            final StringMap stringMap = createStringMap(value);
            stringMap.freeze();
            return stringMap;
        }
        return null;
    }

    @Override
    protected @Nullable StringMap getMutableLocalMapOrNull() {
        final StringMap map = localMap.get();
        if (map != null) {
            final StringMap mutableMap = createStringMap(map);
            localMap.set(mutableMap);
            return mutableMap;
        }
        return null;
    }

    private void freezeLocalMap() {
        final StringMap map = localMap.get();
        if (map != null) {
            map.freeze();
        }
    }

    @Override
    public void putValue(final String key, final Object value) {
        super.putValue(key, value);
        freezeLocalMap();
    }

    @Override
    public <V> void putAllValues(final Map<String, V> values) {
        super.putAllValues(values);
        freezeLocalMap();
    }

    @Override
    public void remove(final String key) {
        super.remove(key);
        freezeLocalMap();
    }

    @Override
    public void removeAll(final Iterable<String> keys) {
        super.removeAll(keys);
        freezeLocalMap();
    }

    @Override
    public void clear() {
        localMap.remove();
    }
}
