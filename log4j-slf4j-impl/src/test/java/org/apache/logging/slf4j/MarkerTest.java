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
package org.apache.logging.slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class MarkerTest {

    private static final String CHILD_MAKER_NAME = MarkerTest.class.getSimpleName() + "-TEST";
    private static final String PARENT_MARKER_NAME = MarkerTest.class.getSimpleName() + "-PARENT";
    private static Log4jMarkerFactory markerFactory;

    @BeforeAll
    public static void startup() {
        markerFactory = ((Log4jLoggerFactory) org.slf4j.LoggerFactory.getILoggerFactory()).getMarkerFactory();
    }

    @BeforeEach
    @AfterEach
    public void clearMarkers() {
        MarkerManager.clear();
    }

    @Test
    public void testAddMarker() {
        final String childMakerName = CHILD_MAKER_NAME + "-AM";
        final String parentMarkerName = PARENT_MARKER_NAME + "-AM";
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMakerName);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMarkerName);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(parentMarkerName);
        final Marker log4jMarker = MarkerManager.getMarker(childMakerName);

        assertInstanceOf(Log4jMarker.class, slf4jMarker, "Incorrect Marker class");
        assertTrue(
                log4jMarker.isInstanceOf(log4jParent),
                String.format(
                        "%s (log4jMarker=%s) is not an instance of %s (log4jParent=%s) in Log4j",
                        childMakerName, parentMarkerName, log4jMarker, log4jParent));
        assertTrue(
                slf4jMarker.contains(slf4jParent),
                String.format(
                        "%s (slf4jMarker=%s) is not an instance of %s (log4jParent=%s) in SLF4J",
                        childMakerName, parentMarkerName, slf4jMarker, slf4jParent));
    }

    @Test
    public void testAddNullMarker() {
        final String childMarkerName = CHILD_MAKER_NAME + "-ANM";
        final String parentMakerName = PARENT_MARKER_NAME + "-ANM";
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
        final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
        final Log4jMarker log4jSlf4jParent = new Log4jMarker(markerFactory, log4jParent);
        final Log4jMarker log4jSlf4jMarker = new Log4jMarker(markerFactory, log4jMarker);
        final org.slf4j.Marker nullMarker = null;
        try {
            log4jSlf4jParent.add(nullMarker);
            fail("Expected " + IllegalArgumentException.class.getName());
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            log4jSlf4jMarker.add(nullMarker);
            fail("Expected " + IllegalArgumentException.class.getName());
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAddSameMarker() {
        final String childMarkerName = CHILD_MAKER_NAME + "-ASM";
        final String parentMakerName = PARENT_MARKER_NAME + "-ASM";
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
        slf4jMarker.add(slf4jParent);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
        final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
        assertTrue(
                log4jMarker.isInstanceOf(log4jParent),
                String.format(
                        "%s (log4jMarker=%s) is not an instance of %s (log4jParent=%s) in Log4j",
                        childMarkerName, parentMakerName, log4jMarker, log4jParent));
        assertTrue(
                slf4jMarker.contains(slf4jParent),
                String.format(
                        "%s (slf4jMarker=%s) is not an instance of %s (log4jParent=%s) in SLF4J",
                        childMarkerName, parentMakerName, slf4jMarker, slf4jParent));
    }

    @Test
    public void testEquals() {
        final String childMarkerName = CHILD_MAKER_NAME + "-ASM";
        final String parentMakerName = PARENT_MARKER_NAME + "-ASM";
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
        final org.slf4j.Marker slf4jMarker2 = org.slf4j.MarkerFactory.getMarker(childMarkerName);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
        final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
        final Marker log4jMarker2 = MarkerManager.getMarker(childMarkerName);
        assertEquals(log4jMarker, log4jMarker2);
        assertEquals(slf4jMarker, slf4jMarker2);
        assertNotEquals(log4jParent, log4jMarker);
        assertNotEquals(slf4jParent, slf4jMarker);
    }

    @Test
    public void testContainsNullMarker() {
        final String childMarkerName = CHILD_MAKER_NAME + "-CM";
        final String parentMakerName = PARENT_MARKER_NAME + "-CM";
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
        final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
        final Log4jMarker log4jSlf4jParent = new Log4jMarker(markerFactory, log4jParent);
        final Log4jMarker log4jSlf4jMarker = new Log4jMarker(markerFactory, log4jMarker);
        final org.slf4j.Marker nullMarker = null;
        try {
            assertFalse(log4jSlf4jParent.contains(nullMarker));
            fail("Expected " + IllegalArgumentException.class.getName());
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            assertFalse(log4jSlf4jMarker.contains(nullMarker));
            fail("Expected " + IllegalArgumentException.class.getName());
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testContainsNullString() {
        final String childMarkerName = CHILD_MAKER_NAME + "-CS";
        final String parentMakerName = PARENT_MARKER_NAME + "-CS";
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMarkerName);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
        final Marker log4jMarker = MarkerManager.getMarker(childMarkerName);
        final Log4jMarker log4jSlf4jParent = new Log4jMarker(markerFactory, log4jParent);
        final Log4jMarker log4jSlf4jMarker = new Log4jMarker(markerFactory, log4jMarker);
        final String nullStr = null;
        assertFalse(log4jSlf4jParent.contains(nullStr));
        assertFalse(log4jSlf4jMarker.contains(nullStr));
    }

    @Test
    public void testRemoveNullMarker() {
        final String childMakerName = CHILD_MAKER_NAME + "-CM";
        final String parentMakerName = PARENT_MARKER_NAME + "-CM";
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(childMakerName);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(parentMakerName);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(parentMakerName);
        final Marker log4jMarker = MarkerManager.getMarker(childMakerName);
        final Log4jMarker log4jSlf4jParent = new Log4jMarker(markerFactory, log4jParent);
        final Log4jMarker log4jSlf4jMarker = new Log4jMarker(markerFactory, log4jMarker);
        final org.slf4j.Marker nullMarker = null;
        assertFalse(log4jSlf4jParent.remove(nullMarker));
        assertFalse(log4jSlf4jMarker.remove(nullMarker));
    }
}
