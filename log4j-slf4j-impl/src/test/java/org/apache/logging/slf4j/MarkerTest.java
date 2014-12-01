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
package org.apache.logging.slf4j;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class MarkerTest {

    private static final String PARENT_MARKER_NAME = MarkerTest.class.getSimpleName() + "-PARENT";
    private static final String CHILD_MAKER_NAME = MarkerTest.class.getSimpleName() + "-TEST";

    @Before
    @After
    public void clearMarkers() {
        MarkerManager.clear();
    }

    @Test
    public void testMarker() {
        final org.slf4j.Marker slf4jMarker = org.slf4j.MarkerFactory.getMarker(CHILD_MAKER_NAME);
        final org.slf4j.Marker slf4jParent = org.slf4j.MarkerFactory.getMarker(PARENT_MARKER_NAME);
        slf4jMarker.add(slf4jParent);
        final Marker log4jParent = MarkerManager.getMarker(PARENT_MARKER_NAME);
        final Marker log4jMarker = MarkerManager.getMarker(CHILD_MAKER_NAME);

        assertTrue("Incorrect Marker class", slf4jMarker instanceof Log4jMarker);
        assertTrue(String.format("%s (log4jMarker=%s) is not an instance of %s (log4jParent=%s) in Log4j",
                CHILD_MAKER_NAME, PARENT_MARKER_NAME, log4jMarker, log4jParent), log4jMarker.isInstanceOf(log4jParent));
        assertTrue(String.format("%s (slf4jMarker=%s) is not an instance of %s (log4jParent=%s) in SLF4J",
                CHILD_MAKER_NAME, PARENT_MARKER_NAME, slf4jMarker, slf4jParent), slf4jMarker.contains(slf4jParent));
    }

}
