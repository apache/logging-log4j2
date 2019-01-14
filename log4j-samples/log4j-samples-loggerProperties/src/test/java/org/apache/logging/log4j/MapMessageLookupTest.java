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

package org.apache.logging.log4j;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.lookup.MapMessageLookup;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link MapMessageLookup}
 */
public class MapMessageLookupTest
{
    @Test
    public void testStructuredDataMessageLookup() {
        // GIVEN: A StructuredDataMessage object
        final StructuredDataMessage message = new StructuredDataMessage("id", "msg", "type");

        message.put("A", "a");
        message.put("B", "b");
        message.put("C", "c");

        // AND: An event with that message
        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.DEBUG).setMessage(message).build();

        // AND: A MapMessageLookup object
        final MapMessageLookup lookup = new MapMessageLookup();

        // WHEN: Lookup is performed
        final String a = lookup.lookup(event, "A");
        final String b = lookup.lookup(event, "B");
        final String c = lookup.lookup(event, "C");

        // THEN: The looked up values are correct
        assertEquals("a", a);
        assertEquals("b", b);
        assertEquals("c", c);
    }

    @Test
    public void testStringMapMessageLookup() {
        // GIVEN: A StringMapMessage object
        final Map<String, String> values = new HashMap<>(3);
        values.put("A", "a");
        values.put("B", "b");
        values.put("C", "c");
        final MapMessage message = new StringMapMessage(values);

        // AND: An event with that message
        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.DEBUG).setMessage(message).build();

        // AND: A MapMessageLookup object
        final MapMessageLookup lookup = new MapMessageLookup();

        // WHEN: Lookup is performed
        final String a = lookup.lookup(event, "A");
        final String b = lookup.lookup(event, "B");
        final String c = lookup.lookup(event, "C");

        // THEN: The looked up values are correct
        assertEquals("a", a);
        assertEquals("b", b);
        assertEquals("c", c);
    }
}
