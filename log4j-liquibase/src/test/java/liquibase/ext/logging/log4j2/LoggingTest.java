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
package liquibase.ext.logging.log4j2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import liquibase.logging.Logger;

import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class LoggingTest {

    private static final String NAME = "LoggerName";

    static Logger logger;

    @BeforeClass
    public static void setupClass() {
        logger = new Log4j2Logger();
        logger.setName(NAME);
        logger.setLogLevel("debug", null);
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        verify(NAME + " " + getClass().getName() + " DEBUG Debug message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void info() {
        logger.info("Info message");
        verify(NAME + " " + getClass().getName() + " INFO Info message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void warning() {
        logger.warning("Warning message");
        verify(NAME + " " + getClass().getName() + " WARN Warning message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void severe() {
        logger.severe("Severe message");
        verify(NAME + " " + getClass().getName() + " ERROR Severe message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void severeStacktrace() {
        logger.severe("Severe message with stacktrace", new RuntimeException("thrown error"));
        verify(NAME + " " + getClass().getName() + " ERROR Severe message with stacktrace" + Strings.LINE_SEPARATOR
                + "java.lang.RuntimeException: thrown error");
    }

    private void verify(final String expected) {
        final ListAppender listApp = ListAppender.getListAppender("List");
        assertNotNull("Missing Appender", listApp);
        final List<String> events = listApp.getMessages();
        assertTrue("Incorrect number of messages. Expected 1 Actual " + events.size(), events.size() == 1);
        final String actual = events.get(0);
        assertEquals("Incorrect message. Expected " + expected + ". Actual " + actual, expected, actual);
        listApp.clear();
    }

    @After
    public void cleanup() {
        ListAppender.getListAppender("List").clear();
    }
}
