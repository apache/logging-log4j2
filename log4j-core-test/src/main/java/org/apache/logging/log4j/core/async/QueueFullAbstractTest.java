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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.test.async.BlockingAppender;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

/**
 * Tests queue full scenarios abstract superclass.
 */
@Tag("async")
@UsingStatusListener
@Timeout(value = 5, unit = SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
public abstract class QueueFullAbstractTest {
    protected static boolean TRACE = Boolean.getBoolean(QueueFullAbstractTest.class.getSimpleName() + ".TRACE");
    protected BlockingAppender blockingAppender;
    protected Unlocker unlocker;

    protected static final String APPENDER_NAME = "Blocking";
    protected static final int BUFFER_COUNT = 128;
    protected static final int MESSAGE_COUNT = BUFFER_COUNT + 2;
    protected static final Logger LOGGER = StatusLogger.getLogger();

    protected static class Unlocker extends Thread {

        final CountDownLatch countDownLatch;
        final BlockingAppender blockingAppender;

        Unlocker(final CountDownLatch countDownLatch, final BlockingAppender blockingAppender) {
            this.countDownLatch = countDownLatch;
            this.blockingAppender = blockingAppender;
        }

        @Override
        public void run() {
            try {
                countDownLatch.await();
                LOGGER.info("Unlocker activated. Sleeping 500 millis before taking action...");
                Thread.sleep(500);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            LOGGER.info("Unlocker signalling BlockingAppender to proceed...");
            blockingAppender.countDownLatch.countDown();
        }
    }

    protected void testNormalQueueFullKeepsMessagesInOrder(
            final LoggerContext ctx, final BlockingAppender blockingAppender) throws Exception {
        checkConfig(ctx);
        final Logger logger = ctx.getLogger(getClass());

        blockingAppender.countDownLatch = new CountDownLatch(1);
        final Unlocker unlocker = new Unlocker(new CountDownLatch(MESSAGE_COUNT - 1), blockingAppender);
        unlocker.start();
        asyncTest(logger, unlocker, blockingAppender);
        unlocker.join();
    }

    protected abstract void checkConfig(final LoggerContext ctx) throws Exception;

    protected void asyncTest(final Logger logger, final Unlocker unlocker, final BlockingAppender blockingAppender) {
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            LOGGER.info(
                    "Test logging message {}. Ring buffer capacity was {}, countdown latch was {}.",
                    i,
                    asyncRemainingCapacity(logger),
                    unlocker.countDownLatch.getCount());
            unlocker.countDownLatch.countDown();
            final String param = "I'm innocent";
            logger.info("Logging innocent object #{}: {}", i, param);
        }
        LOGGER.info(
                "Waiting for message delivery: blockingAppender.logEvents.count={}.",
                blockingAppender.logEvents.size());
        while (blockingAppender.logEvents.size() < MESSAGE_COUNT) {
            Thread.yield();
        }
        LOGGER.info(
                "All {} message have been delivered: blockingAppender.logEvents.count={}.",
                MESSAGE_COUNT,
                blockingAppender.logEvents.size());

        final Stack<String> actual = transform(blockingAppender.logEvents);
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            assertThat(actual.pop()).isEqualTo("Logging innocent object #%d: I'm innocent", i);
        }
        assertThat(actual).isEmpty();
    }

    static Stack<String> transform(final List<LogEvent> logEvents) {
        final List<String> filtered = getMessages(logEvents);
        Collections.reverse(filtered);
        final Stack<String> result = new Stack<>();
        result.addAll(filtered);
        return result;
    }

    static List<String> getMessages(final List<LogEvent> logEvents) {
        final List<String> filtered = new ArrayList<>(logEvents.size());
        for (LogEvent event : logEvents) {
            filtered.add(event.getMessage().getFormattedMessage());
        }
        return filtered;
    }

    protected long asyncRemainingCapacity(final Logger logger) {
        final LoggerConfig loggerConfig = ((org.apache.logging.log4j.core.Logger) logger).get();
        final Appender async = loggerConfig.getAppenders().get("async");
        if (async instanceof AsyncAppender) {
            return ((AsyncAppender) async).getQueueCapacity();
        }
        throw new IllegalStateException("Neither Async Loggers nor AsyncAppender are configured");
    }

    protected static void assertAsyncAppender(final LoggerContext ctx) {
        final Configuration config = ctx.getConfiguration();
        assertThat(config).isNotNull();

        final Collection<Appender> appenders =
                config.getRootLogger().getAppenders().values();
        assertThat(appenders).hasSize(1).allMatch(AsyncAppender.class::isInstance);
    }

    protected static void assertFormatMessagesInBackground() {
        assertThat(Constants.FORMAT_MESSAGES_IN_BACKGROUND).isTrue();
    }
}
