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

import com.lmax.disruptor.dsl.Disruptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.ListStatusListener;
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

    protected static class DomainObject {

        private final Logger innerLogger;
        private final Unlocker unlocker;
        private final int count;

        public DomainObject(final Logger innerLogger, final Unlocker unlocker, final int loggingCount) {
            this.innerLogger = innerLogger;
            this.unlocker = unlocker;
            this.count = loggingCount;
        }

        @Override
        public String toString() {
            for (int i = 0; i < count; i++) {
                LOGGER.info(
                        "DomainObject logging message {}. Ring buffer capacity was {}, countdown latch was {}.",
                        i,
                        asyncRemainingCapacity(innerLogger),
                        unlocker.countDownLatch.getCount());
                unlocker.countDownLatch.countDown();
                innerLogger.info("Logging in toString() #{}", i);
            }
            return "Who's bad?!";
        }
    }

    private ListStatusListener statusListener;

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

    protected void checkConfig(final LoggerContext ctx) throws Exception {}

    protected static void asyncTest(
            final Logger logger, final Unlocker unlocker, final BlockingAppender blockingAppender) {
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

    public void testLoggingFromToStringCausesOutOfOrderMessages(
            final LoggerContext ctx, final BlockingAppender blockingAppender) throws Exception {
        checkConfig(ctx);
        // Non-reusable messages will call `toString()` on the main thread and block it.
        final Logger logger = ctx.getLogger(this.getClass());

        blockingAppender.countDownLatch = new CountDownLatch(1);
        final Unlocker unlocker = new Unlocker(new CountDownLatch(MESSAGE_COUNT - 1), blockingAppender);
        unlocker.start();
        asyncRecursiveTest(logger, unlocker, blockingAppender);
        unlocker.join();
    }

    void asyncRecursiveTest(final Logger logger, final Unlocker unlocker, final BlockingAppender blockingAppender) {
        for (int i = 0; i < 1; i++) {
            LOGGER.info(
                    "Test logging message {}. Ring buffer capacity was {}, countdown latch was {}.",
                    i,
                    asyncRemainingCapacity(logger),
                    unlocker.countDownLatch.getCount());
            unlocker.countDownLatch.countDown();
            final DomainObject obj = new DomainObject(logger, unlocker, MESSAGE_COUNT - 1);
            logger.info("Logging naughty object #{}: {}", i, obj);
        }

        LOGGER.info(
                "Waiting for message delivery: blockingAppender.logEvents.count={}.",
                blockingAppender.logEvents.size());
        while (blockingAppender.logEvents.size() < MESSAGE_COUNT) {
            Thread.yield();
        }
        LOGGER.info(
                "All {} messages have been delivered: blockingAppender.logEvents.count={}.",
                MESSAGE_COUNT,
                blockingAppender.logEvents.size());

        final StatusData mostRecentStatusData = statusListener
                .findStatusData(Level.WARN)
                .reduce((ignored, data) -> data)
                .orElse(null);
        assertThat(mostRecentStatusData).isNotNull();
        assertThat(mostRecentStatusData.getLevel()).isEqualTo(Level.WARN);
        assertThat(mostRecentStatusData.getFormattedStatus())
                .contains("Log4j2 logged an event out of order to prevent deadlock caused by domain "
                        + "objects logging from their toString method when the async queue is full");

        final List<String> actual = blockingAppender.logEvents.stream()
                .map(e -> e.getMessage().getFormattedMessage())
                .collect(Collectors.toList());
        final String[] expected = new String[MESSAGE_COUNT];
        for (int i = 0; i < MESSAGE_COUNT - 1; i++) {
            expected[i] = "Logging in toString() #" + i;
        }
        expected[MESSAGE_COUNT - 1] = "Logging naughty object #0: Who's bad?!";
        assertThat(actual).hasSize(MESSAGE_COUNT).contains(expected);
    }

    static Stack<String> transform(final List<LogEvent> logEvents) {
        final List<String> filtered = new ArrayList<>(logEvents.size());
        for (final LogEvent event : logEvents) {
            filtered.add(event.getMessage().getFormattedMessage());
        }
        Collections.reverse(filtered);
        final Stack<String> result = new Stack<>();
        result.addAll(filtered);
        return result;
    }

    static long asyncRemainingCapacity(final Logger logger) {
        if (logger instanceof AsyncLogger) {
            try {
                final Field f = field(AsyncLogger.class, "loggerDisruptor");
                return ((AsyncLoggerDisruptor) f.get(logger))
                        .getDisruptor()
                        .getRingBuffer()
                        .remainingCapacity();
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            final LoggerConfig loggerConfig = ((org.apache.logging.log4j.core.Logger) logger).get();
            if (loggerConfig instanceof AsyncLoggerConfig) {
                try {
                    final Object delegate =
                            field(AsyncLoggerConfig.class, "delegate").get(loggerConfig);
                    return ((Disruptor) field(AsyncLoggerConfigDisruptor.class, "disruptor")
                                    .get(delegate))
                            .getRingBuffer()
                            .remainingCapacity();
                } catch (final Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                final Appender async = loggerConfig.getAppenders().get("async");
                if (async instanceof AsyncAppender) {
                    return ((AsyncAppender) async).getQueueCapacity();
                }
            }
        }
        throw new IllegalStateException("Neither Async Loggers nor AsyncAppender are configured");
    }

    protected static Field field(final Class<?> c, final String name) throws NoSuchFieldException {
        final Field f = c.getDeclaredField(name);
        ReflectionUtil.makeAccessible(f);
        return f;
    }

    protected static void assertAsyncAppender(final LoggerContext ctx) {
        assertThat(ctx).isNotInstanceOf(AsyncLoggerContext.class);

        final Configuration config = ctx.getConfiguration();
        assertThat(config).isNotNull();
        assertThat(config.getRootLogger()).isNotInstanceOf(AsyncLoggerConfig.class);

        final Collection<Appender> appenders =
                config.getRootLogger().getAppenders().values();
        assertThat(appenders).hasSize(1).allMatch(AsyncAppender.class::isInstance);
    }

    protected static void assertAsyncLogger(final LoggerContext ctx, final int expectedBufferSize) {
        assertThat(ctx).isInstanceOf(AsyncLoggerContext.class);
        final RingBufferAdmin ringBufferAdmin = ((AsyncLoggerContext) ctx).createRingBufferAdmin();
        assertThat(ringBufferAdmin.getRemainingCapacity()).isEqualTo(expectedBufferSize);

        final Configuration config = ctx.getConfiguration();
        assertThat(config).isNotNull();
        assertThat(config.getRootLogger()).isNotInstanceOf(AsyncLoggerConfig.class);
    }

    protected static void assertAsyncLoggerConfig(final LoggerContext ctx, final int expectedBufferSize)
            throws ReflectiveOperationException {
        assertThat(ctx).isNotInstanceOf(AsyncLoggerContext.class);

        final Configuration config = ctx.getConfiguration();
        assertThat(config).isNotNull();
        assertThat(config.getRootLogger()).isInstanceOf(AsyncLoggerConfig.class);
        final AsyncLoggerConfigDisruptor disruptor = (AsyncLoggerConfigDisruptor) config.getAsyncLoggerConfigDelegate();
        final Field sizeField = field(AsyncLoggerConfigDisruptor.class, "ringBufferSize");
        assertThat(sizeField.get(disruptor)).isEqualTo(expectedBufferSize);
    }

    protected static void assertFormatMessagesInBackground() {
        assertThat(Constants.FORMAT_MESSAGES_IN_BACKGROUND).isTrue();
    }
}
