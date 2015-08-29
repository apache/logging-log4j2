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
import static org.junit.Assert.assertNotEquals;
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
public class CustomLevelsOverrideTest {

    private static final String CONFIG = "log4j-customLevels.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private ListAppender listAppender;
    private Level warnLevel;
    private Level infoLevel;
    private Level debugLevel;

    @Before
    public void before() {
        warnLevel = Level.getLevel("WARN");
        infoLevel = Level.getLevel("INFO");
        debugLevel = Level.getLevel("DEBUG");
        listAppender = context.getListAppender("List1").clear();
    }

    @Test
    public void testCustomLevelInts() {
        // assertEquals(350, warnLevel.intLevel());
        // assertEquals(450, infoLevel.intLevel());
        // assertEquals(550, debugLevel.intLevel());
        assertNotEquals(350, warnLevel.intLevel());
        assertNotEquals(450, infoLevel.intLevel());
        assertNotEquals(550, debugLevel.intLevel());
    }

    @Test
    public void testCustomLevelPresence() {
        assertNotNull(warnLevel);
        assertNotNull(infoLevel);
        assertNotNull(debugLevel);
    }

    @Test
    public void testCustomLevelVsStdLevel() {
        assertEquals(Level.WARN, warnLevel);
        assertEquals(Level.INFO, infoLevel);
        assertEquals(Level.DEBUG, debugLevel);
    }

    @Test
    public void testLog() {
        final Logger logger = context.getLogger();
        final List<LogEvent> events = listAppender.getEvents();
        assertThat(events, hasSize(0));
        logger.debug("Hello, {}", "World");
        assertThat(events, hasSize(1));
        logger.log(warnLevel, "Hello DIAG");
        assertThat(events, hasSize(2));
        assertEquals(events.get(1).getLevel(), warnLevel);

    }
}
