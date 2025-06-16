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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MarkerLookup}.
 *
 * @since 2.4
 */
class EventLookupTest {

    private static final String ABSENT_MARKER_NAME = "NONE";
    private final String markerName = "EventLookupTest";
    private final StrLookup strLookup = new EventLookup();

    @Test
    void testLookupEventMarker() {
        final Marker marker = MarkerManager.getMarker(markerName);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setMarker(marker) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        final String value = strLookup.lookup(event, "Marker");
        assertEquals(markerName, value);
    }

    @Test
    void testLookupEventMessage() {
        final String msg = "Hello, world!";
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage(msg))
                .build();
        final String value = strLookup.lookup(event, "Message");
        assertEquals(msg, value);
    }

    @Test
    void testLookupEventLevel() {
        final String msg = "Hello, world!";
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage(msg))
                .build();
        final String value = strLookup.lookup(event, "Level");
        assertEquals(Level.INFO.toString(), value);
    }

    @Test
    void testLookupEventTimestamp() {
        final String msg = "Hello, world!";
        final long now = System.currentTimeMillis();
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setTimeMillis(now)
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage(msg))
                .build();
        final String value = strLookup.lookup(event, "Timestamp");
        assertEquals(Long.toString(now), value);
    }

    @Test
    void testLookupEventLogger() {
        final String msg = "Hello, world!";
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage(msg))
                .build();
        final String value = strLookup.lookup(event, "Logger");
        assertEquals(this.getClass().getName(), value);
    }

    @Test
    void testLookupEventThreadName() {
        final String msg = "Hello, world!";
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setThreadName("Main")
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage(msg))
                .build();
        final String value = strLookup.lookup(event, "ThreadName");
        assertEquals("Main", value);
    }
}
