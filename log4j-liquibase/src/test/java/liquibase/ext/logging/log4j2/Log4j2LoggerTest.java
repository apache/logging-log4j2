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

import liquibase.Scope;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class Log4j2LoggerTest {

    static Log4j2Logger logger;

    @BeforeClass
    public static void setupClass() {
        logger = (Log4j2Logger) Scope.getCurrentScope().getLog(Log4j2LoggerTest.class);
        ListAppender.getListAppender("List").clear();
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        verify(getClass().getName() + " liquibase.logging.core.AbstractLogger DEBUG Debug message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void info() {
        logger.info("Info message");
        verify(getClass().getName() + " liquibase.logging.core.AbstractLogger INFO Info message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void warning() {
        logger.warning("Warning message");
        verify(getClass().getName() + " liquibase.logging.core.AbstractLogger WARN Warning message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void severe() {
        logger.severe("Severe message");
        verify(getClass().getName() + " liquibase.logging.core.AbstractLogger FATAL Severe message" + Strings.LINE_SEPARATOR);
    }

    @Test
    public void severeStacktrace() {
        logger.severe("Severe message with stacktrace", new RuntimeException("thrown error"));
        verify(getClass().getName() + " liquibase.logging.core.AbstractLogger FATAL Severe message with stacktrace" + Strings.LINE_SEPARATOR
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
