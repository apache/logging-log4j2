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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.GarbageCollectionHelper;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.message.Message;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.junit.Assert.assertTrue;

/**
 * Tests queue full scenarios with pure AsyncLoggers (all loggers async).
 */
@RunWith(BlockJUnit4ClassRunner.class)
@Category(AsyncLoggers.class)
public class QueueFullAsyncLoggerTest3 extends QueueFullAbstractTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND, "true");
        System.setProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY, "discard");
        System.setProperty(Log4jProperties.ASYNC_LOGGER_RING_BUFFER_SIZE, "128"); // minimum ringbuffer size
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(Log4jProperties.ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND);
        System.clearProperty(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY);
        System.clearProperty(Log4jProperties.ASYNC_LOGGER_RING_BUFFER_SIZE);
    }

    @Rule
    public LoggerContextRule context = new LoggerContextRule(
            "log4j2-queueFull.xml", AsyncLoggerContextSelector.class);

    @Before
    public void before() throws Exception {
        blockingAppender = context.getRequiredAppender("Blocking", BlockingAppender.class);
    }


    @Test(timeout = 15000)
    public void discardedMessagesShouldBeGarbageCollected() throws InterruptedException {
        final Logger logger = context.getLogger(this.getClass());

        blockingAppender.logEvents = null;
        blockingAppender.countDownLatch = new CountDownLatch(1);
        int count = 200;
        CountDownLatch garbageCollectionLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            logger.info(new CountdownOnGarbageCollectMessage(garbageCollectionLatch));
        }
        blockingAppender.countDownLatch.countDown();

        try (GarbageCollectionHelper gcHelper = new GarbageCollectionHelper()) {
            gcHelper.run();
            assertTrue("Parameter should have been garbage collected", garbageCollectionLatch.await(30, TimeUnit.SECONDS));
        }
    }

    private static final class CountdownOnGarbageCollectMessage implements Message {

        private final CountDownLatch latch;

        CountdownOnGarbageCollectMessage(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public String getFormattedMessage() {
            return "formatted";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        @Override
        protected void finalize() throws Throwable {
            latch.countDown();
            super.finalize();
        }
    }
}
