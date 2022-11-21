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

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPropertyResolverTest {
    final PropertyResolver resolver = new DefaultPropertyResolver();

    @BeforeEach
    void setUp() {
        final URL json = getClass().getClassLoader().getResource("property-resolver-test.json");
        assertNotNull(json);
        resolver.addSource(JsonResourcePropertySource.fromUrl(json, 0));
        // TODO: test properties file version too
    }

    @Test
    void hasProperty() {
        assertTrue(resolver.hasProperty("log4j2.*.Component1.propertyName1"));
        assertTrue(resolver.hasProperty("TestContext", "log4j2.*.Component1.propertyName1"));
        assertTrue(resolver.hasProperty("log4j2.*.Component1.propertyName2"));
        assertTrue(resolver.hasProperty("TestContext", "log4j2.*.Component2.propertyName5"));
        assertFalse(resolver.hasProperty("log4j2.*.Component1.propertyName5"));
    }

    @Test
    void getString() {
        assertEquals("value1", resolver.getString("log4j2.*.Component1.propertyName1").orElseThrow());
        assertEquals("override", resolver.getString("TestContext", "log4j2.*.Component1.propertyName1").orElseThrow());
    }

    @Test
    void getList() {
        final List<String> list = resolver.getList("log4j2.*.Component2.propertyName6");
        assertEquals(List.of("foo", "bar"), list);
    }

    @Test
    void getBoolean() {
        assertTrue(resolver.getBoolean("log4j2.*.Component1.propertyName4"));
    }

    @Test
    void getInt() {
        assertEquals(3, resolver.getInt("log4j2.*.Component1.propertyName3").orElseThrow());
    }

    @Test
    void getLong() {
        assertEquals(90000L, resolver.getLong("log4j2.*.Component2.propertyName5").orElseThrow());
        assertEquals(0L, resolver.getLong("TestContext", "log4j2.*.Component2.propertyName5").orElseThrow());
    }
}
