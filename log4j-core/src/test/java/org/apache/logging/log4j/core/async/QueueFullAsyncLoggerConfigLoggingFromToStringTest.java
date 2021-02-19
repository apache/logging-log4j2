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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Tests queue full scenarios with AsyncLoggers in configuration.
 */
@RunWith(BlockJUnit4ClassRunner.class)
@Category(AsyncLoggers.class)
public class QueueFullAsyncLoggerConfigLoggingFromToStringTest extends QueueFullAbstractTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty("AsyncLoggerConfig.RingBufferSize", "128");
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "log4j2-queueFullAsyncLoggerConfig.xml");
    }

    @Rule
    public LoggerContextRule context = new LoggerContextRule(
            "log4j2-queueFullAsyncLoggerConfig.xml");

    @Before
    public void before() throws Exception {
        blockingAppender = context.getRequiredAppender("Blocking", BlockingAppender.class);
    }

    @Test(timeout = 5000)
    public void testLoggingFromToStringCausesOutOfOrderMessages() throws InterruptedException {
        //TRACE = true;
        final Logger logger = LogManager.getLogger(this.getClass());

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
        assertThat(actual.pop()).isEqualTo("Logging in toString() #0");
        List<StatusData> statusDataList = StatusLogger.getLogger().getStatusData();
        assertThat(actual.pop()).describedAs("Jumped the queue: queue full").isEqualTo("Logging in toString() #128");
        StatusData mostRecentStatusData = statusDataList.get(statusDataList.size() - 1);
        assertThat(mostRecentStatusData.getLevel()).describedAs("Expected warn level status message").isEqualTo(Level.WARN);
        assertThat(mostRecentStatusData.getFormattedStatus()).contains("Log4j2 logged an event out of order to prevent deadlock caused by domain " +
                        "objects logging from their toString method when the async queue is full");

        for (int i = 1; i < 128; i++) {
            assertThat(actual.pop()).describedAs("First batch").isEqualTo("Logging in toString() #" + i);
        }
        assertThat(actual.pop()).isEqualTo("logging naughty object #0 Who's bad?!");
        assertThat(actual.isEmpty()).isTrue();
    }
}
