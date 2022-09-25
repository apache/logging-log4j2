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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.test.ExtendedLevels;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("log4j-customLevel.xml")
public class ExtendedLevelTest {

    private final ListAppender list1;
    private final ListAppender list2;

    public ExtendedLevelTest(@Named("List1") final ListAppender list1, @Named("List2") final ListAppender list2) {
        this.list1 = list1.clear();
        this.list2 = list2.clear();
    }

    @Test
    public void testLevelLogging(final LoggerContext context) {
        org.apache.logging.log4j.Logger logger = context.getLogger("org.apache.logging.log4j.test1");
        logger.log(ExtendedLevels.DETAIL, "Detail message");
        logger.log(Level.DEBUG, "Debug message");
        List<LogEvent> events = list1.getEvents();
        assertNotNull(events, "No events");
        assertThat(events, hasSize(1));
        LogEvent event = events.get(0);
        assertEquals("DETAIL", event.getLevel().name(), "Expected level DETAIL, got" + event.getLevel());
        logger = context.getLogger("org.apache.logging.log4j.test2");
        logger.log(ExtendedLevels.NOTE, "Note message");
        logger.log(Level.INFO, "Info message");
        events = list2.getEvents();
        assertNotNull(events, "No events");
        assertThat(events, hasSize(1));
        event = events.get(0);
        assertEquals("NOTE", event.getLevel().name(), "Expected level NOTE, got" + event.getLevel());
    }
}
