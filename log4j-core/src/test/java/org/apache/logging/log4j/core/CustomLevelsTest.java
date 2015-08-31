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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

/**
 *
 */
public class CustomLevelsTest {

    private static final String CONFIG = "log4j-customLevels.xml";
    
    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);
    
    private ListAppender listAppender;
    private Level diagLevel;
    private Level noticeLevel;
    private Level verboseLevel;

    @Before
    public void before() {
        diagLevel = Level.getLevel("DIAG");
        noticeLevel = Level.getLevel("NOTICE");
        verboseLevel = Level.getLevel("VERBOSE");
        listAppender = context.getListAppender("List1").clear();
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
        final Logger logger = context.getLogger();
        final List<LogEvent> events = listAppender.getEvents();
        assertThat(events, hasSize(0));
        logger.debug("Hello, {}", "World");
        assertThat(events, hasSize(1));
        logger.log(diagLevel, "Hello DIAG");
        assertThat(events, hasSize(2));
        assertEquals(events.get(1).getLevel(), diagLevel);

    }
}
