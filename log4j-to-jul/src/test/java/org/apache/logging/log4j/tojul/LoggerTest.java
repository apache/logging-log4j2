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
package org.apache.logging.log4j.tojul;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.TestLogHandler;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LoggerTest {

    // Save levels so that we can reset them @After clearLogs()
    private static final java.util.logging.Logger globalLogger = java.util.logging.Logger.getGlobal();
    private static final java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
    private static final Level globalLevel = globalLogger.getLevel();
    private static final Level rootLevel = rootLogger.getLevel();

    private org.apache.logging.log4j.Logger log4jLogger;
    private java.util.logging.Logger julLogger;
    private Level julLoggerDefaultLevel;

    // https://javadoc.io/doc/com.google.guava/guava-testlib/latest/com/google/common/testing/TestLogHandler.html
    private TestLogHandler handler;


    @Before public void setupLogCapture() {
        handler = new TestLogHandler();
        // Beware, the order here should not be changed!
        // Let the bridge do whatever it does BEFORE we create a JUL Logger (which SHOULD be the same)
        log4jLogger = LogManager.getLogger(LoggerTest.class);
        assertThat(log4jLogger).isInstanceOf(JULLogger.class);
        julLogger = java.util.logging.Logger.getLogger(LoggerTest.class.getName());
        assertThat(julLogger).isSameInstanceAs(((JULLogger)log4jLogger).logger);
        julLogger.addHandler(handler);

        julLoggerDefaultLevel = julLogger.getLevel();

        // Check that there is no configuration file which invalidates our assumption that the root logger is the parent of our julLogger
        assertThat(julLogger.getParent()).isEqualTo(rootLogger);
    }

    @After public void clearLogs() {
        julLogger.removeHandler(handler);
        // Reset all Levels what any tests set anymore
        julLogger.setLevel(julLoggerDefaultLevel);
        rootLogger.setLevel(rootLevel);
        globalLogger.setLevel(globalLevel);
    }

    @Test public void infoAtInfo() {
        julLogger.setLevel(Level.INFO);
        log4jLogger.info("hello, world");

        List<LogRecord> logs = handler.getStoredLogRecords();
        assertThat(logs).hasSize(1);
        LogRecord log1 = logs.get(0);
        assertThat(log1.getLoggerName()).isEqualTo(LoggerTest.class.getName());
        assertThat(log1.getLevel()).isEqualTo(java.util.logging.Level.INFO);
        assertThat(log1.getMessage()).isEqualTo("hello, world");
        assertThat(log1.getParameters()).isNull();
        assertThat(log1.getThrown()).isNull();
    }

    @Test public void infoAtInfoOnParent() {
        julLogger.getParent().setLevel(Level.INFO);
        log4jLogger.info("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @Test public void infoWithoutAnyLevel() {
        // We're not setting any level.
        log4jLogger.info("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }
}
