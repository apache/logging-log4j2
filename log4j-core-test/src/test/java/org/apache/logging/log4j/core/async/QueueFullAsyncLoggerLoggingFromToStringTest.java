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
package org.apache.logging.log4j.core.async;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.Assert.assertEquals;

/**
 * Tests queue full scenarios with pure AsyncLoggers (all loggers async).
 */
@RunWith(BlockJUnit4ClassRunner.class)
@Category(AsyncLoggers.class)
@SetSystemProperty(key = LoggingSystemProperty.Constant.WEB_IS_WEBAPP, value = "false")
@SetSystemProperty(key = Log4jPropertyKey.Constant.ASYNC_LOGGER_RING_BUFFER_SIZE, value = "128")
@SetSystemProperty(key = LoggingSystemProperty.Constant.THREAD_LOCALS_ENABLE, value = "true")
public class QueueFullAsyncLoggerLoggingFromToStringTest extends QueueFullAbstractTest {

    @Rule
    public LoggerContextRule context = new LoggerContextRule(
            "log4j2-queueFull.xml", AsyncLoggerContextSelector.class);

    @Before
    public void before() throws Exception {
        blockingAppender = context.getRequiredAppender("Blocking", BlockingAppender.class);
    }

    @Test(timeout = 50000)
    public void testLoggingFromToStringCausesOutOfOrderMessages() {
        final Logger logger = context.getLogger(this.getClass());

        blockingAppender.countDownLatch = new CountDownLatch(1);
        unlocker = new Unlocker(new CountDownLatch(129));
        unlocker.start();

        asyncLoggerRecursiveTest(logger, unlocker, blockingAppender, this);
    }

    static void asyncLoggerRecursiveTest(final Logger logger,
                                         final Unlocker unlocker,
                                         final BlockingAppender blockingAppender,
                                         final QueueFullAbstractTest factory) {
        for (int i = 0; i < 1; i++) {
            TRACE("Test logging message " + i  + ". Remaining capacity=" + asyncRemainingCapacity(logger));
            TRACE("Test decrementing unlocker countdown latch. Count=" + unlocker.countDownLatch.getCount());
            unlocker.countDownLatch.countDown();
            final DomainObject obj = factory.new DomainObject(129);
            logger.info(new ParameterizedMessage("logging naughty object #{} {}", i, obj));
        }
        TRACE("Before stop() blockingAppender.logEvents.count=" + blockingAppender.logEvents.size());
        CoreLoggerContexts.stopLoggerContext(false); // stop async thread
        while (blockingAppender.logEvents.size() < 129) { Thread.yield(); }
        TRACE("After  stop() blockingAppender.logEvents.count=" + blockingAppender.logEvents.size());

        final List<String> messages = getMessages(blockingAppender.logEvents);
        assertEquals("Jumped the queue: test(2)+domain1(65)+domain2(61)=128: queue full",
                "Logging in toString() #127", messages.get(messages.size() - 2));
        assertEquals("Logging in toString() #128", messages.get(messages.size() - 1));
        assertEquals("logging naughty object #0 Who's bad?!", messages.get(0));
    }
}
