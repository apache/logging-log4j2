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
package org.apache.logging.log4j.tojul;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.testing.TestLogHandler;
import java.io.IOException;
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

    @Before
    public void setupLogCapture() {
        handler = new TestLogHandler();
        // Beware, the order here should not be changed!
        // Let the bridge do whatever it does BEFORE we create a JUL Logger (which SHOULD be the same)
        log4jLogger = LogManager.getLogger(getClass());
        assertThat(log4jLogger).isInstanceOf(JULLogger.class);
        julLogger = java.util.logging.Logger.getLogger(getClass().getName());
        assertThat(julLogger).isSameAs(((JULLogger) log4jLogger).getWrappedLogger());
        julLogger.addHandler(handler);

        julLoggerDefaultLevel = julLogger.getLevel();

        // Check that there is no configuration file which invalidates our assumption that the root logger is the parent
        // of our julLogger
        assertThat(julLogger.getParent()).isEqualTo(rootLogger);
    }

    @After
    public void clearLogs() {
        julLogger.removeHandler(handler);
        // Reset all Levels what any tests set anymore
        julLogger.setLevel(julLoggerDefaultLevel);
        rootLogger.setLevel(rootLevel);
        globalLogger.setLevel(globalLevel);
    }

    @Test
    public void infoAtInfo() {
        julLogger.setLevel(Level.INFO);
        log4jLogger.info("hello, world");

        final List<LogRecord> logs = handler.getStoredLogRecords();
        assertThat(logs).hasSize(1);
        final LogRecord log1 = logs.get(0);
        assertThat(log1.getLoggerName()).isEqualTo(getClass().getName());
        assertThat(log1.getLevel()).isEqualTo(java.util.logging.Level.INFO);
        assertThat(log1.getMessage()).isEqualTo("hello, world");
        assertThat(log1.getParameters()).isNull();
        assertThat(log1.getThrown()).isNull();
        assertThat(log1.getSourceClassName()).isEqualTo(getClass().getName());
        assertThat(log1.getSourceMethodName()).isEqualTo("infoAtInfo");
    }

    @Test
    public void infoAtInfoWithParameters() {
        julLogger.setLevel(Level.INFO);
        log4jLogger.info("hello, {}", "world");

        final List<LogRecord> logs = handler.getStoredLogRecords();
        assertThat(logs).hasSize(1);
        final LogRecord log1 = logs.get(0);
        assertThat(log1.getMessage()).isEqualTo("hello, world");
        assertThat(log1.getParameters()).isNull();
        assertThat(log1.getThrown()).isNull();
    }

    @Test
    public void errorAtSevereWithException() {
        julLogger.setLevel(Level.SEVERE);
        log4jLogger.error("hello, {}", "world", new IOException("Testing, testing"));

        final List<LogRecord> logs = handler.getStoredLogRecords();
        assertThat(logs).hasSize(1);
        final LogRecord log1 = logs.get(0);
        assertThat(log1.getMessage()).isEqualTo("hello, world");
        assertThat(log1.getParameters()).isNull();
        assertThat(log1.getThrown()).isInstanceOf(IOException.class);
    }

    @Test
    public void infoAtInfoWithLogBuilder() {
        julLogger.setLevel(Level.INFO);
        log4jLogger.atInfo().log("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @Test
    public void infoAtInfoOnParent() {
        julLogger.getParent().setLevel(Level.INFO);
        log4jLogger.info("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @Test
    public void infoWithoutAnyLevel() {
        // We're not setting any level.
        log4jLogger.info("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @Test
    public void debugAtInfo() {
        julLogger.setLevel(Level.INFO);
        log4jLogger.debug("hello, world");
        assertThat(handler.getStoredLogRecords()).isEmpty();
    }

    @Test
    public void debugAtFiner() {
        julLogger.setLevel(Level.FINER);
        log4jLogger.debug("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @Test
    public void traceAtFine() {
        julLogger.setLevel(Level.FINE);
        log4jLogger.trace("hello, world");
        assertThat(handler.getStoredLogRecords()).isEmpty();
    }

    @Test
    public void traceAtAllOnParent() {
        julLogger.getParent().setLevel(Level.ALL);
        log4jLogger.trace("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @Test
    public void fatalAtOff() {
        julLogger.getParent().setLevel(Level.OFF);
        log4jLogger.fatal("hello, world");
        assertThat(handler.getStoredLogRecords()).isEmpty();
    }

    @Test
    public void fatalAtSevere() {
        julLogger.getParent().setLevel(Level.SEVERE);
        log4jLogger.atFatal().log("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @Test
    public void warnAtFatal() {
        julLogger.getParent().setLevel(Level.SEVERE);
        log4jLogger.atWarn().log("hello, world");
        assertThat(handler.getStoredLogRecords()).isEmpty();
    }

    @Test
    public void customLevelJustUnderWarning() {
        julLogger.getParent().setLevel(new CustomLevel("Just under Warning", Level.WARNING.intValue() - 1));

        log4jLogger.info("hello, world");
        assertThat(handler.getStoredLogRecords()).isEmpty();

        log4jLogger.warn("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);

        log4jLogger.error("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(2);
    }

    @Test
    public void customLevelJustAboveWarning() {
        julLogger.getParent().setLevel(new CustomLevel("Just above Warning", Level.WARNING.intValue() + 1));

        log4jLogger.info("hello, world");
        assertThat(handler.getStoredLogRecords()).isEmpty();

        log4jLogger.warn("hello, world");
        assertThat(handler.getStoredLogRecords()).isEmpty();

        log4jLogger.error("hello, world");
        assertThat(handler.getStoredLogRecords()).hasSize(1);
    }

    @SuppressWarnings("serial")
    private static class CustomLevel extends Level {
        CustomLevel(final String name, final int value) {
            super(name, value);
        }
    }

    /**
     * Test that the {@link LogRecord#getSourceClassName()}, which we already tested above in infoAtInfo()
     * also works as expected if the logging happened in a class that we have called (indirect), not in the test method itself.
     */
    @Test
    public void indirectSource() {
        java.util.logging.Logger.getLogger(Another.class.getName()).setLevel(Level.INFO);
        new Another(handler);
        final List<LogRecord> logs = handler.getStoredLogRecords();
        assertThat(logs).hasSize(1);
        final LogRecord log1 = logs.get(0);
        assertThat(log1.getSourceClassName()).isEqualTo(Another.class.getName());
        assertThat(log1.getSourceMethodName()).isEqualTo("<init>");
    }

    static class Another {
        org.apache.logging.log4j.Logger anotherLog4jLogger = LogManager.getLogger(getClass());
        java.util.logging.Logger anotherJULLogger =
                java.util.logging.Logger.getLogger(getClass().getName());

        Another(final TestLogHandler handler) {
            anotherJULLogger.addHandler(handler);
            anotherLog4jLogger.info("hello, another world");
        }
    }

    @Test
    public void placeholdersInFormat() {
        julLogger.setLevel(Level.INFO);
        log4jLogger.info("hello, {0} {}", "world");

        final List<LogRecord> logs = handler.getStoredLogRecords();
        assertThat(logs).hasSize(1);
        final LogRecord log1 = logs.get(0);
        final String formattedMessage = new java.util.logging.SimpleFormatter().formatMessage(log1);
        assertThat(formattedMessage).isEqualTo("hello, {0} world");
    }

    @Test
    public void placeholdersInFormattedMessage() {
        julLogger.setLevel(Level.INFO);
        log4jLogger.info("hello, {}", "{0} world");

        final List<LogRecord> logs = handler.getStoredLogRecords();
        assertThat(logs).hasSize(1);
        final LogRecord log1 = logs.get(0);
        final String formattedMessage = new java.util.logging.SimpleFormatter().formatMessage(log1);
        assertThat(formattedMessage).isEqualTo("hello, {0} world");
    }
}
