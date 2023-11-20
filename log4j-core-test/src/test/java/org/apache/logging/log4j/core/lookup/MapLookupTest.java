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
package org.apache.logging.log4j.core.lookup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MapLookup}.
 */
public class MapLookupTest {

    @Test
    public void testEmptyMap() {
        final MapLookup lookup = new MapLookup(new HashMap<String, String>());
        assertNull(lookup.lookup(null));
        assertNull(lookup.lookup("X"));
    }

    @Test
    public void testMap() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("A", "B");
        final MapLookup lookup = new MapLookup(map);
        assertNull(lookup.lookup(null));
        assertEquals("B", lookup.lookup("A"));
    }

    @Test
    public void testNullMap() {
        final MapLookup lookup = new MapLookup();
        assertNull(lookup.lookup(null));
        assertNull(lookup.lookup("X"));
    }

    @Test
    public void testMainMap() {
        MainMapLookup.setMainArguments("--file", "foo.txt");
        final MapLookup lookup = MainMapLookup.MAIN_SINGLETON;
        assertNull(lookup.lookup(null));
        assertNull(lookup.lookup("X"));
        assertEquals("--file", lookup.lookup("0"));
        assertEquals("foo.txt", lookup.lookup("1"));
        assertEquals("foo.txt", lookup.lookup("--file"));
        assertNull(lookup.lookup("foo.txt"));
    }

    @Test
    public void testEventStringMapMessage() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("A", "B");
        final HashMap<String, String> eventMap = new HashMap<>();
        eventMap.put("A1", "B1");
        final StringMapMessage message = new StringMapMessage(eventMap);
        final LogEvent event = Log4jLogEvent.newBuilder().setMessage(message).build();
        final MapLookup lookup = new MapLookup(map);
        assertEquals("B", lookup.lookup(event, "A"));
        assertEquals("B1", lookup.lookup(event, "A1"));
    }

    @Test
    public void testEventMapMessage() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("A", "B");
        final HashMap<String, Object> eventMap = new HashMap<>();
        eventMap.put("A1", 11);
        final MapMessage message = new MapMessage<>(eventMap);
        final LogEvent event = Log4jLogEvent.newBuilder().setMessage(message).build();
        final MapLookup lookup = new MapLookup(map);
        assertEquals("B", lookup.lookup(event, "A"));
        assertEquals("11", lookup.lookup(event, "A1"));
    }

    @Test
    public void testLookupMapMessageIsCheckedBeforeDefaultMap() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("A", "ADefault");
        map.put("B", "BDefault");
        final HashMap<String, Object> eventMap = new HashMap<>();
        eventMap.put("A", "AEvent");
        final MapMessage message = new MapMessage<>(eventMap);
        final LogEvent event = Log4jLogEvent.newBuilder().setMessage(message).build();
        final MapLookup lookup = new MapLookup(map);
        assertEquals("AEvent", lookup.lookup(event, "A"));
        assertEquals("BDefault", lookup.lookup(event, "B"));
    }

    @Test
    public void testNullEvent() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("A", "B");
        final MapLookup lookup = new MapLookup(map);
        assertEquals("B", lookup.lookup(null, "A"));
    }
}
