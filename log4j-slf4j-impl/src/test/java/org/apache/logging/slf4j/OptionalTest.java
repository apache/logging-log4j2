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

import java.util.List;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 *
 */
@LoggerContextSource(value = "log4j-test1.xml")
class OptionalTest {

    Logger logger = LoggerFactory.getLogger("EventLogger");
    Marker marker = MarkerFactory.getMarker("EVENT");
    private final LoggerContext CTX;

    public OptionalTest(final LoggerContext context) {
        this.CTX = context;
    }

    @Test
    void testEventLogger() {
        logger.info(marker, "This is a test");
        MDC.clear();
        verify("EventLogger", "o.a.l.s.OptionalTest This is a test" + Strings.LINE_SEPARATOR);
    }

    private void verify(final String name, final String expected) {
        final ListAppender listApp = CTX.getConfiguration().getAppender(name);
        final List<String> events = listApp.getMessages();
        assertEquals(1, events.size(), "Incorrect number of messages. Expected 1 Actual " + events.size());
        final String actual = events.get(0);
        assertEquals(expected, actual, "Incorrect message. Expected " + expected + ". Actual " + actual);
        listApp.clear();
    }

    @BeforeEach
    void cleanup(@Named("List") final ListAppender list, @Named("EventLogger") final ListAppender eventLogger) {
        list.clear();
        eventLogger.clear();
    }
}
