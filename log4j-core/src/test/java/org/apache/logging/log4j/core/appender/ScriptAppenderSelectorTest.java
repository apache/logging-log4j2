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
package org.apache.logging.log4j.core.appender;

import java.util.List;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.categories.Scripts;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
@Category(Scripts.Groovy.class)
public class ScriptAppenderSelectorTest {

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        // @formatter:off
        return new Object[][] { 
            { "log4j-appender-selector-groovy.xml" },
            { "log4j-appender-selector-javascript.xml" },
        };
        // @formatter:on
    }

    @Rule
    public final LoggerContextRule loggerContextRule;

    public ScriptAppenderSelectorTest(final String configLocation) {
        this.loggerContextRule = new LoggerContextRule(configLocation);
    }

    private ListAppender getListAppender() {
        return loggerContextRule.getListAppender("SelectIt");
    }

    private void logAndCheck() {
        final Marker marker = MarkerManager.getMarker("HEXDUMP");
        final Logger logger = loggerContextRule.getLogger(ScriptAppenderSelectorTest.class);
        logger.error("Hello");
        final ListAppender listAppender = getListAppender();
        final List<LogEvent> list = listAppender.getEvents();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        logger.error("World");
        assertTrue("Incorrect number of events. Expected 2, got " + list.size(), list.size() == 2);
        logger.error(marker, "DEADBEEF");
        assertTrue("Incorrect number of events. Expected 3, got " + list.size(), list.size() == 3);
    }

    @Test(expected = AssertionError.class)
    public void testAppender1Absence() {
        loggerContextRule.getListAppender("List1");
    }

    @Test(expected = AssertionError.class)
    public void testAppender2Absence() {
        loggerContextRule.getListAppender("List2");
    }

    @Test
    public void testAppenderPresence() {
        getListAppender();
    }

    @Test
    public void testLogging1() {
        logAndCheck();
    }

    @Test
    public void testLogging2() {
        logAndCheck();
    }
}
