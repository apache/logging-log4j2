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
package org.apache.logging.log4j.core.async;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests queue full scenarios with AsyncLoggers in configuration.
 */
@Tag("async")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "https://issues.apache.org/jira/browse/LOG4J2-3513")
@SetSystemProperty(key = Log4jProperties.ASYNC_CONFIG_RING_BUFFER_SIZE, value = "128")
public class QueueFullAsyncLoggerConfigLoggingFromToStringTest extends QueueFullAbstractTest {

    @Test
    @LoggerContextSource(value = "log4j2-queueFullAsyncLoggerConfig.xml", timeout = 5)
    public void testLoggingFromToStringCausesOutOfOrderMessages(
            final LoggerContext context, @Named("Blocking") final BlockingAppender blockingAppender) {
        this.blockingAppender = blockingAppender;
        //TRACE = true;
        final Logger logger = context.getLogger(this.getClass());

        blockingAppender.countDownLatch = new CountDownLatch(1);
        unlocker = new Unlocker(new CountDownLatch(129)); // count slightly different from "pure" async loggers
        unlocker.start();

        asyncLoggerConfigRecursiveTest(logger, unlocker, blockingAppender, this);
    }

    static void asyncLoggerConfigRecursiveTest(final Logger logger,
                                               final Unlocker unlocker,
                                               final BlockingAppender blockingAppender,
                                               final QueueFullAbstractTest factory) {
        for (int i = 0; i < 1; i++) {
            TRACE("Test logging message " + i  + ". Remaining capacity=" + asyncRemainingCapacity(logger));
            TRACE("Test decrementing unlocker countdown latch. Count=" + unlocker.countDownLatch.getCount());
            unlocker.countDownLatch.countDown();
            final DomainObject obj = factory.new DomainObject(129);
            logger.info("logging naughty object #{} {}", i, obj);
        }
        TRACE("Before stop() blockingAppender.logEvents.count=" + blockingAppender.logEvents.size());
        //CoreLoggerContexts.stopLoggerContext(false); // stop async thread
        while (blockingAppender.logEvents.size() < 130) { Thread.yield(); }
        TRACE("After  stop() blockingAppender.logEvents.count=" + blockingAppender.logEvents.size());

        final Stack<String> actual = transform(blockingAppender.logEvents);
        assertEquals("Logging in toString() #0", actual.pop());
        List<StatusData> statusDataList = StatusLogger.getLogger().getStatusData();
        assertEquals("Logging in toString() #128", actual.pop(), "Jumped the queue: queue full");
        StatusData mostRecentStatusData = statusDataList.get(statusDataList.size() - 1);
        assertEquals(Level.WARN, mostRecentStatusData.getLevel(), "Expected warn level status message");
        assertThat(mostRecentStatusData.getFormattedStatus(), containsString(
                "Log4j2 logged an event out of order to prevent deadlock caused by domain " +
                        "objects logging from their toString method when the async queue is full"));

        for (int i = 1; i < 128; i++) {
            assertEquals("Logging in toString() #" + i, actual.pop(), "First batch");
        }
        assertEquals("logging naughty object #0 Who's bad?!", actual.pop());
        assertTrue(actual.isEmpty());
    }
}
