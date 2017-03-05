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

import java.util.List;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.junit.Assert.*;

/**
 *
 */
public class OptionalTest {

    private static final String CONFIG = "log4j-test1.xml";

    @ClassRule
    public static final LoggerContextRule CTX = new LoggerContextRule(CONFIG);

    Logger logger = LoggerFactory.getLogger("EventLogger");
    Marker marker = MarkerFactory.getMarker("EVENT");

    @Test
    public void testEventLogger() {
        logger.info(marker, "This is a test");
        MDC.clear();
        verify("EventLogger", "o.a.l.s.OptionalTest This is a test" + Strings.LINE_SEPARATOR);
    }

    private void verify(final String name, final String expected) {
        final ListAppender listApp = CTX.getListAppender(name);
        final List<String> events = listApp.getMessages();
        assertTrue("Incorrect number of messages. Expected 1 Actual " + events.size(), events.size()== 1);
        final String actual = events.get(0);
        assertEquals("Incorrect message. Expected " + expected + ". Actual " + actual, expected, actual);
        listApp.clear();
    }

    @Before
    public void cleanup() {
        CTX.getListAppender("List").clear();
        CTX.getListAppender("EventLogger").clear();
    }
}
