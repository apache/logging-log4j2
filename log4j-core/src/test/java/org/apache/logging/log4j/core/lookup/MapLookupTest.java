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
package org.apache.logging.log4j.core.lookup;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.junit.Test;

/**
 * Tests {@link MapLookup}.
 */
public class MapLookupTest {

    @Test
    public void testEmptyMap() {
        final MapLookup lookup = new MapLookup(new HashMap<String, String>());
        assertEquals(null, lookup.lookup(null));
        assertEquals(null, lookup.lookup("X"));
    }

    @Test
    public void testMap() {
        final HashMap<String, String> map = new HashMap<>();
        map.put("A", "B");
        final MapLookup lookup = new MapLookup(map);
        assertEquals(null, lookup.lookup(null));
        assertEquals("B", lookup.lookup("A"));
    }

    @Test
    public void testNullMap() {
        final MapLookup lookup = new MapLookup();
        assertEquals(null, lookup.lookup(null));
        assertEquals(null, lookup.lookup("X"));
    }

    @Test
    public void testMainMap() {
        MapLookup.setMainArguments(new String[] {
                "--file",
                "foo.txt" });
        final MapLookup lookup = MainMapLookup.MAIN_SINGLETON;
        assertEquals(null, lookup.lookup(null));
        assertEquals(null, lookup.lookup("X"));
        assertEquals("--file", lookup.lookup("0"));
        assertEquals("foo.txt", lookup.lookup("1"));
        assertEquals("foo.txt", lookup.lookup("--file"));
        assertEquals(null, lookup.lookup("foo.txt"));
    }

    @Test
    public void testEventMapMessage() {
      final HashMap<String, String> map = new HashMap<>();
      map.put("A", "B");
      final HashMap<String, String> eventMap = new HashMap<>();
      eventMap.put("A1", "B1");
      final MapMessage message = new MapMessage(eventMap);
      final LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(message)
                .build();
      final MapLookup lookup = new MapLookup(map);
      assertEquals("B", lookup.lookup(event, "A"));
      assertEquals("B1", lookup.lookup(event, "A1"));
    }

    @Test
    public void testNullEvent() {
      final HashMap<String, String> map = new HashMap<>();
      map.put("A", "B");
      final MapLookup lookup = new MapLookup(map);
      assertEquals("B", lookup.lookup(null, "A"));
    }
}
