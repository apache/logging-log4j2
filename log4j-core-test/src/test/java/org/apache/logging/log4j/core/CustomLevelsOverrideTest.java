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
public class CustomLevelsOverrideTest {

    private final ListAppender listAppender;
    private final Level warnLevel;
    private final Level infoLevel;
    private final Level debugLevel;
    private final Logger logger;

    public CustomLevelsOverrideTest(final LoggerContext context, @Named("List1") final ListAppender appender) {
        warnLevel = Level.getLevel("WARN");
        infoLevel = Level.getLevel("INFO");
        debugLevel = Level.getLevel("DEBUG");
        listAppender = appender.clear();
        logger = context.getLogger(getClass().getName());
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
        assertThat(listAppender.getEvents(), hasSize(0));
        logger.debug("Hello, {}", "World");
        assertThat(listAppender.getEvents(), hasSize(1));
        logger.log(warnLevel, "Hello DIAG");
        assertThat(listAppender.getEvents(), hasSize(2));
        assertEquals(listAppender.getEvents().get(1).getLevel(), warnLevel);
    }
}
