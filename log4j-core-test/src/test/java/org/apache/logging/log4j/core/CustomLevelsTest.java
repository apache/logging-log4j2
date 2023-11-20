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
package org.apache.logging.log4j.core;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-customLevels.xml")
public class CustomLevelsTest {

    private final ListAppender listAppender;
    private final Level diagLevel;
    private final Level noticeLevel;
    private final Level verboseLevel;
    private final Logger logger;

    public CustomLevelsTest(final LoggerContext context, @Named("List1") final ListAppender appender) {
        diagLevel = Level.getLevel("DIAG");
        noticeLevel = Level.getLevel("NOTICE");
        verboseLevel = Level.getLevel("VERBOSE");
        listAppender = appender.clear();
        logger = context.getLogger(getClass().getName());
    }

    @Test
    public void testCustomLevelInts() {
        assertEquals(350, diagLevel.intLevel());
        assertEquals(450, noticeLevel.intLevel());
        assertEquals(550, verboseLevel.intLevel());
    }

    @Test
    public void testCustomLevelPresence() {
        assertNotNull(diagLevel);
        assertNotNull(noticeLevel);
        assertNotNull(verboseLevel);
    }

    @Test
    public void testLog() {
        assertThat(listAppender.getEvents(), hasSize(0));
        logger.debug("Hello, {}", "World");
        assertThat(listAppender.getEvents(), hasSize(1));
        logger.log(diagLevel, "Hello DIAG");
        assertThat(listAppender.getEvents(), hasSize(2));
        assertEquals(listAppender.getEvents().get(1).getLevel(), diagLevel);
    }
}
