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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.junit.UsingThreadContextMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * Tests the {@code DefaultThreadContextMap} class.
 */
@UsingThreadContextMap
public class DefaultThreadContextMapTest {

    @Test
    public void testEqualsVsSameKind() {
        final DefaultThreadContextMap map1 = createMap();
        final DefaultThreadContextMap map2 = createMap();
        assertThat(map1).isEqualTo(map1);
        assertThat(map2).isEqualTo(map2);
        assertThat(map2).isEqualTo(map1);
        assertThat(map1).isEqualTo(map2);
    }

    @Test
    public void testHashCodeVsSameKind() {
        final DefaultThreadContextMap map1 = createMap();
        final DefaultThreadContextMap map2 = createMap();
        assertThat(map2.hashCode()).isEqualTo(map1.hashCode());
    }

    @Test
    public void testDoesNothingIfConstructedWithUseMapIsFalse() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(false);
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.containsKey("key")).isFalse();
        map.put("key", "value");

        assertThat(map.isEmpty()).isTrue();
        assertThat(map.containsKey("key")).isFalse();
        assertThat(map.get("key")).isNull();
    }

    @Test
    public void testPut() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.containsKey("key")).isFalse();
        map.put("key", "value");

        assertThat(map.isEmpty()).isFalse();
        assertThat(map.containsKey("key")).isTrue();
        assertThat(map.get("key")).isEqualTo("value");
    }

    @Test
    public void testPutAll() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.containsKey("key")).isFalse();
        final int mapSize = 10;
        final Map<String, String> newMap = new HashMap<>(mapSize);
        for (int i = 1; i <= mapSize; i++) {
            newMap.put("key" + i, "value" + i);
        }
        map.putAll(newMap);
        assertThat(map.isEmpty()).isFalse();
        for (int i = 1; i <= mapSize; i++) {
            assertThat(map.containsKey("key" + i)).isTrue();
            assertThat(map.get("key" + i)).isEqualTo("value" + i);
        }
    }

    /**
     * Test method for
     * {@link org.apache.logging.log4j.spi.DefaultThreadContextMap#remove(java.lang.String)}
     * .
     */
    @Test
    public void testRemove() {
        final DefaultThreadContextMap map = createMap();
        assertThat(map.get("key")).isEqualTo("value");
        assertThat(map.get("key2")).isEqualTo("value2");

        map.remove("key");
        assertThat(map.containsKey("key")).isFalse();
        assertThat(map.get("key2")).isEqualTo("value2");
    }

    @Test
    public void testClear() {
        final DefaultThreadContextMap map = createMap();

        map.clear();
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.containsKey("key")).isFalse();
        assertThat(map.containsKey("key2")).isFalse();
    }

    /**
     * @return
     */
    private DefaultThreadContextMap createMap() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertThat(map.isEmpty()).isTrue();
        map.put("key", "value");
        map.put("key2", "value2");
        assertThat(map.get("key")).isEqualTo("value");
        assertThat(map.get("key2")).isEqualTo("value2");
        return map;
    }
    
    @Test
    public void testGetCopyReturnsMutableMap() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertThat(map.isEmpty()).isTrue();
        final Map<String, String> copy = map.getCopy();
        assertThat(copy.isEmpty()).isTrue();

        copy.put("key", "value"); // mutable
        assertThat(copy.get("key")).isEqualTo("value");

        // thread context map not affected
        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    public void testGetCopyReturnsMutableCopy() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        map.put("key1", "value1");
        assertThat(map.isEmpty()).isFalse();
        final Map<String, String> copy = map.getCopy();
        assertThat(copy.get("key1")).isEqualTo("value1"); // copy has values too

        copy.put("key", "value"); // copy is mutable
        assertThat(copy.get("key")).isEqualTo("value");

        // thread context map not affected
        assertThat(map.containsKey("key")).isFalse();

        // clearing context map does not affect copy
        map.clear();
        assertThat(map.isEmpty()).isTrue();

        assertThat(copy.isEmpty()).isFalse();
    }

    @Test
    public void testGetImmutableMapReturnsNullIfEmpty() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.getImmutableMapOrNull()).isNull();
    }

    @Test
    public void testGetImmutableMapReturnsImmutableMapIfNonEmpty() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        map.put("key1", "value1");
        assertThat(map.isEmpty()).isFalse();

        final Map<String, String> immutable = map.getImmutableMapOrNull();
        assertThat(immutable.get("key1")).isEqualTo("value1"); // copy has values too

        // immutable
        assertThatThrownBy(() -> immutable.put("key", "value")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testGetImmutableMapCopyNotAffectdByContextMapChanges() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        map.put("key1", "value1");
        assertThat(map.isEmpty()).isFalse();

        final Map<String, String> immutable = map.getImmutableMapOrNull();
        assertThat(immutable.get("key1")).isEqualTo("value1"); // copy has values too

        // clearing context map does not affect copy
        map.clear();
        assertThat(map.isEmpty()).isTrue();

        assertThat(immutable.isEmpty()).isFalse();
    }

    @Test
    public void testToStringShowsMapContext() {
        final DefaultThreadContextMap map = new DefaultThreadContextMap(true);
        assertThat(map.toString()).isEqualTo("{}");

        map.put("key1", "value1");
        assertThat(map.toString()).isEqualTo("{key1=value1}");

        map.remove("key1");
        map.put("key2", "value2");
        assertThat(map.toString()).isEqualTo("{key2=value2}");
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    public void testThreadLocalNotInheritableByDefault() {
        System.clearProperty(DefaultThreadContextMap.INHERITABLE_MAP);
        final ThreadLocal<Map<String, String>> threadLocal = DefaultThreadContextMap.createThreadLocalMap(true);
        assertThat(threadLocal instanceof InheritableThreadLocal<?>).isFalse();
    }
    
    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    public void testThreadLocalInheritableIfConfigured() {
        System.setProperty(DefaultThreadContextMap.INHERITABLE_MAP, "true");
        ThreadContextMapFactory.init();
        try {
            final ThreadLocal<Map<String, String>> threadLocal = DefaultThreadContextMap.createThreadLocalMap(true);
            assertThat(threadLocal instanceof InheritableThreadLocal<?>).isTrue();
        } finally {
            System.clearProperty(DefaultThreadContextMap.INHERITABLE_MAP);
        }
    }
}
