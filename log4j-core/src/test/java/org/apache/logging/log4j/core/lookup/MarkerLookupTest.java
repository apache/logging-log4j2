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
import static org.junit.Assert.assertNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

/**
 * Tests {@link MarkerLookup}.
 * 
 * @since 2.4
 */
public class MarkerLookupTest {

    private static final String ABSENT_MARKER_NAME = "NONE";
    private final String markerName = "MarkerLookupTest";
    private final StrLookup strLookup = new MarkerLookup();

    @Test
    public void testLookupEventExistant() {
        final Marker marker = MarkerManager.getMarker(markerName);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setMarker(marker) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")).build();
        final String value = strLookup.lookup(event, marker.getName());
        assertEquals(markerName, value);
    }

    @Test
    public void testLookupEventNonExistant() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")).build();
        final String value = strLookup.lookup(event, ABSENT_MARKER_NAME);
        assertNull(value);
    }

    @Test
    public void testLookupEventNonExistantKey() {
        final Marker marker = MarkerManager.getMarker(markerName);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setMarker(marker) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")).build();
        final String value = strLookup.lookup(event, ABSENT_MARKER_NAME);
        assertEquals(markerName, value);
    }

    @Test
    public void testLookupEventNullNonExistant() {
        final String value = strLookup.lookup(null, ABSENT_MARKER_NAME);
        assertNull(value);
    }

    @Test
    public void testLookupExistant() {
        final String value = strLookup.lookup(MarkerManager.getMarker(markerName).getName());
        assertEquals(markerName, value);
    }

    @Test
    public void testLookupNonExistant() {
        final String value = strLookup.lookup(ABSENT_MARKER_NAME);
        assertNull(value);
    }

}