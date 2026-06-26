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
package org.apache.logging.log4j.core.appender.rolling.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

@SetSystemProperty(key = "log4j2.status.entries", value = "10")
@SetSystemProperty(key = "log4j2.StatusLogger.level", value = "WARN")
class AbstractActionTest {

    // Test for LOG4J2-2658
    @Test
    void testExceptionsAreLoggedToStatusLogger() {
        final StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new TestAction().run();
        final List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList, hasSize(1));
        final StatusData statusData = statusDataList.get(0);
        assertEquals(Level.WARN, statusData.getLevel());
        final String formattedMessage = statusData.getFormattedStatus();
        assertThat(
                formattedMessage,
                containsString("Exception reported by action 'class org.apache."
                        + "logging.log4j.core.appender.rolling.action.AbstractActionTest$TestAction'"
                        + System.lineSeparator()
                        + "java.io.IOException: failed" + System.lineSeparator()
                        + "\tat org.apache.logging.log4j.core.appender.rolling.action.AbstractActionTest"
                        + "$TestAction.execute(AbstractActionTest.java:"));
    }

    @Test
    void testRuntimeExceptionsAreLoggedToStatusLogger() {
        final StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new AbstractAction() {
            @Override
            public boolean execute() {
                throw new IllegalStateException();
            }
        }.run();
        final List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList, hasSize(1));
        final StatusData statusData = statusDataList.get(0);
        assertEquals(Level.WARN, statusData.getLevel());
        final String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage, containsString("Exception reported by action"));
    }

    @Test
    void testErrorsAreLoggedToStatusLogger() {
        final StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.clear();
        new AbstractAction() {
            @Override
            public boolean execute() {
                throw new AssertionError();
            }
        }.run();
        final List<StatusData> statusDataList = statusLogger.getStatusData();
        assertThat(statusDataList, hasSize(1));
        final StatusData statusData = statusDataList.get(0);
        assertEquals(Level.WARN, statusData.getLevel());
        final String formattedMessage = statusData.getFormattedStatus();
        assertThat(formattedMessage, containsString("Exception reported by action"));
    }

    // ── blockThread(min, max) ─────────────────────────────────────────────

    /**
     * blockThread(0, 0) must return immediately without sleeping.
     */
    @Test
    void testBlockThreadBothZeroIsInstant() {
        long start = System.currentTimeMillis();
        AbstractAction.blockThread(0, 0);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 500, "blockThread(0,0) should be instant, took " + elapsed + "ms");
    }

    /**
     * blockThread(min, max) with min == max must sleep for exactly that many seconds.
     */
    @Test
    void testBlockThreadFixedDelay() {
        int fixedDelay = 1;
        long start = System.currentTimeMillis();
        AbstractAction.blockThread(fixedDelay, fixedDelay);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(
                elapsed >= fixedDelay * 1000L - 100,
                "blockThread(" + fixedDelay + "," + fixedDelay + ") should sleep ~" + fixedDelay + "s, took "
                        + elapsed + "ms");
        assertTrue(
                elapsed <= fixedDelay * 1000L + 500,
                "blockThread(" + fixedDelay + "," + fixedDelay + ") exceeded expected duration, took " + elapsed
                        + "ms");
    }

    /**
     * blockThread(min, max) with a range must sleep within [min, max] seconds.
     */
    @Test
    void testBlockThreadRangeDelay() {
        int min = 1;
        int max = 2;
        long start = System.currentTimeMillis();
        AbstractAction.blockThread(min, max);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(
                elapsed >= min * 1000L - 100,
                "blockThread(" + min + "," + max + ") should sleep at least " + min + "s, took " + elapsed + "ms");
        assertTrue(
                elapsed <= max * 1000L + 500,
                "blockThread(" + min + "," + max + ") should sleep at most " + max + "s, took " + elapsed + "ms");
    }

    /**
     * blockThread with invalid arguments (min > max) must return immediately without sleeping.
     */
    @Test
    void testBlockThreadInvalidArgsAreIgnored() {
        long start = System.currentTimeMillis();
        AbstractAction.blockThread(5, 2); // min > max — invalid
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 500, "blockThread with invalid args should be instant, took " + elapsed + "ms");
    }

    /**
     * blockThread with negative min must return immediately without sleeping.
     */
    @Test
    void testBlockThreadNegativeMinIsIgnored() {
        long start = System.currentTimeMillis();
        AbstractAction.blockThread(-1, 2);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 500, "blockThread with negative min should be instant, took " + elapsed + "ms");
    }

    private static final class TestAction extends AbstractAction {
        @Override
        public boolean execute() throws IOException {
            throw new IOException("failed");
        }
    }
}
