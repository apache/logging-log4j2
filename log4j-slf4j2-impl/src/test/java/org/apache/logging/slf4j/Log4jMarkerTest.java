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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Log4jMarkerTest {

    private static Log4jMarkerFactory markerFactory;

    @BeforeAll
    public static void startup() {
        markerFactory = ((Log4jLoggerFactory) org.slf4j.LoggerFactory.getILoggerFactory()).getMarkerFactory();
    }

    @Test
    public void testEquals() {
        final Marker markerA = MarkerManager.getMarker(Log4jMarkerTest.class.getName() + "-A");
        final Marker markerB = MarkerManager.getMarker(Log4jMarkerTest.class.getName() + "-B");
        final Log4jMarker marker1 = new Log4jMarker(markerFactory, markerA);
        final Log4jMarker marker2 = new Log4jMarker(markerFactory, markerA);
        final Log4jMarker marker3 = new Log4jMarker(markerFactory, markerB);
        assertEquals(marker1, marker2);
        assertNotEquals(marker1, null);
        assertNotEquals(null, marker1);
        assertNotEquals(marker1, marker3);
    }
}
