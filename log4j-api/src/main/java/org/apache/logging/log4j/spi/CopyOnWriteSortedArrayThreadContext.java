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

import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * ThreadContextMap implementation backed by {@code SortedArrayContextData}.
 * A new ThreadContext Map is created each time it is updated and the Map stored is always
 * immutable. This means the Map can be passed to other threads without concern that it will be updated. Since it is
 * expected that the Map will be passed to many more log events than the number of keys it contains the performance
 * should be much better than if the Map was copied for each event.
 */
public class CopyOnWriteSortedArrayThreadContext implements ThreadContextMap {
    /**
     * Property name ({@value} ) for selecting {@code InheritableThreadLocal} (value "true") or plain
     * {@code ThreadLocal} (value is not "true") in the implementation.
     */
    public static final String INHERITABLE_MAP = "isThreadContextMapInheritable";

    private final ThreadLocal<ArrayContextData> localMap;

    public CopyOnWriteSortedArrayThreadContext() {
        this.localMap = createThreadLocalMap();
    }

    // LOG4J2-479: by default, use a plain ThreadLocal, only use InheritableThreadLocal if configured.
    // (This method is package protected for JUnit tests.)
    static ThreadLocal<ArrayContextData> createThreadLocalMap() {
        final PropertiesUtil managerProps = PropertiesUtil.getProperties();
        final boolean inheritable = managerProps.getBooleanProperty(INHERITABLE_MAP);
        if (inheritable) {
            return new InheritableThreadLocal<ArrayContextData>() {
                @Override
                protected ArrayContextData childValue(final ArrayContextData parentValue) {
                    return parentValue != null ? new ArrayContextData(parentValue) : null;
                }
            };
        }
        // if not inheritable, return plain ThreadLocal with null as initial value
        return new ThreadLocal<>();
    }

    @Override
    public void put(final String key, final String value) {
        ArrayContextData map = localMap.get();
        map = map == null ? new ArrayContextData() : new ArrayContextData(map);
        map.put(key, value);
        localMap.set(map);
    }

    @Override
    public String get(final String key) {
        final ArrayContextData map = localMap.get();
        return map == null ? null : map.get(key);
    }

    @Override
    public void remove(final String key) {
        final ArrayContextData map = localMap.get();
        if (map != null) {
            final ArrayContextData copy = new ArrayContextData(map);
            copy.remove(key);
            localMap.set(copy);
        }
    }

    @Override
    public void clear() {
        localMap.remove();
    }

    @Override
    public boolean containsKey(final String key) {
        final ArrayContextData map = localMap.get();
        return map != null && map.containsKey(key);
    }

    @Override
    public Map<String, String> getCopy() {
        final ArrayContextData map = localMap.get();
        return map == null ? Collections.<String, String>emptyMap() : map.asMap();
    }

    public ContextData getContextData() {
        return localMap.get();
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        final ArrayContextData map = localMap.get();
        return map == null ? null : Collections.unmodifiableMap(map.asMap());
    }

    @Override
    public boolean isEmpty() {
        final ArrayContextData map = localMap.get();
        return map == null || map.size() == 0;
    }

    @Override
    public String toString() {
        final ArrayContextData map = localMap.get();
        return map == null ? "{}" : map.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        final ArrayContextData map = this.localMap.get();
        result = prime * result + ((map == null) ? 0 : map.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ThreadContextMap)) {
            return false;
        }
        final ThreadContextMap other = (ThreadContextMap) obj;
        final Map<String, String> map = this.getImmutableMapOrNull();
        final Map<String, String> otherMap = other.getImmutableMapOrNull();
        if (map == null) {
            if (otherMap != null) {
                return false;
            }
        } else if (!map.equals(otherMap)) {
            return false;
        }
        return true;
    }
}
