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

import java.util.Stack;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Tests queue full scenarios with pure AsyncLoggers (all loggers async).
 */
@Ignore
@RunWith(BlockJUnit4ClassRunner.class)
@Category(AsyncLoggers.class)
public class QueueFullAsyncLoggerLoggingFromToStringTest2 extends QueueFullAbstractTest {

    @BeforeClass
    public static void beforeClass() {
        //FORMAT_MESSAGES_IN_BACKGROUND
        System.setProperty("log4j.format.msg.async", "true");

        System.setProperty("AsyncLogger.RingBufferSize", "128"); // minimum ringbuffer size
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "log4j2-queueFull.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @Rule
    public LoggerContextRule context = new LoggerContextRule(
            "log4j2-queueFull.xml", AsyncLoggerContextSelector.class);

    @Before
    public void before() throws Exception {
        blockingAppender = context.getRequiredAppender("Blocking", BlockingAppender.class);
        TRACE = true;
    }

    @Ignore
    @Test(timeout = 5000)
    public void testLoggingFromToStringCausesOutOfOrderMessages() throws InterruptedException {
        final Logger logger = LogManager.getLogger(this.getClass());

        blockingAppender.countDownLatch = new CountDownLatch(1);
        unlocker = new Unlocker(new CountDownLatch(129));
        unlocker.start();

        for (int i = 0; i < 1; i++) {
            TRACE("Test logging message " + i  + ". Remaining capacity=" + asyncRemainingCapacity(logger));
            TRACE("Test decrementing unlocker countdown latch. Count=" + unlocker.countDownLatch.getCount());
            unlocker.countDownLatch.countDown();
            final DomainObject obj = new DomainObject(129);
            logger.info(new ParameterizedMessage("logging naughty object #{} {}", i, obj));
        }
        TRACE("Before stop() blockingAppender.logEvents.count=" + blockingAppender.logEvents.size());
        CoreLoggerContexts.stopLoggerContext(false); // stop async thread
        while (blockingAppender.logEvents.size() < 129) { Thread.yield(); }
        TRACE("After  stop() blockingAppender.logEvents.count=" + blockingAppender.logEvents.size());

        final Stack<String> actual = transform(blockingAppender.logEvents);
        assertEquals("Jumped the queue: test(2)+domain1(65)+domain2(61)=128: queue full",
                "Logging in toString() #127 (Log4j2 logged this message out of order to prevent deadlock caused by domain objects logging from their toString method when the async queue is full - LOG4J2-2031)", actual.pop());
        assertEquals("Logging in toString() #128 (Log4j2 logged this message out of order to prevent deadlock caused by domain objects logging from their toString method when the async queue is full - LOG4J2-2031)", actual.pop());
        assertEquals("logging naughty object #0 Who's bad?!", actual.pop());

        for (int i = 0; i < 127; i++) {
            assertEquals("First batch", "Logging in toString() #" + i, actual.pop());
        }
        assertTrue(actual.isEmpty());
    }
}