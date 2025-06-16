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
import static org.junit.jupiter.api.Assertions.assertNull;

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
class MarkerLookupTest {

    private static final String ABSENT_MARKER_NAME = "NONE";
    private final String markerName = "MarkerLookupTest";
    private final StrLookup strLookup = new MarkerLookup();

    @Test
    void testLookupEventExistant() {
        final Marker marker = MarkerManager.getMarker(markerName);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setMarker(marker) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        final String value = strLookup.lookup(event, marker.getName());
        assertEquals(markerName, value);
    }

    @Test
    void testLookupEventNonExistant() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        final String value = strLookup.lookup(event, ABSENT_MARKER_NAME);
        assertNull(value);
    }

    @Test
    void testLookupEventNonExistantKey() {
        final Marker marker = MarkerManager.getMarker(markerName);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setMarker(marker) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!"))
                .build();
        final String value = strLookup.lookup(event, ABSENT_MARKER_NAME);
        assertEquals(markerName, value);
    }

    @Test
    void testLookupEventNullNonExistant() {
        final String value = strLookup.lookup(null, ABSENT_MARKER_NAME);
        assertNull(value);
    }

    @Test
    void testLookupExistant() {
        final String value =
                strLookup.lookup(MarkerManager.getMarker(markerName).getName());
        assertEquals(markerName, value);
    }

    @Test
    void testLookupNonExistant() {
        final String value = strLookup.lookup(ABSENT_MARKER_NAME);
        assertNull(value);
    }
}
