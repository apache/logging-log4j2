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
package org.apache.logging.log4j.async.logger;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmax.disruptor.RingBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.test.async.BlockingAppender;
import org.apache.logging.log4j.core.test.async.QueueFullAbstractTest;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.ListStatusListener;

public abstract class QueueFullAsyncAbstractTest extends QueueFullAbstractTest {

    private ListStatusListener statusListener;

    protected static void assertAsyncLogger(final LoggerContext ctx, final int expectedBufferSize) {
        assertThat(ctx).isInstanceOf(AsyncLoggerContext.class);
        assertThat(((AsyncLoggerContext) ctx)
                        .getAsyncLoggerDisruptor()
                        .getRingBuffer()
                        .getBufferSize())
                .isEqualTo(expectedBufferSize);

        final Configuration config = ctx.getConfiguration();
        assertThat(config).isNotNull();
        assertThat(config.getRootLogger()).isNotInstanceOf(AsyncLoggerConfig.class);
    }

    protected static void assertAsyncLoggerConfig(final LoggerContext ctx, final int expectedBufferSize) {
        assertThat(ctx).isNotInstanceOf(AsyncLoggerContext.class);

        final Configuration config = ctx.getConfiguration();
        assertThat(config).isNotNull();
        assertThat(config.getRootLogger()).isInstanceOf(AsyncLoggerConfig.class);
        final DisruptorConfiguration disruptorConfig = config.getExtension(DisruptorConfiguration.class);
        final AsyncLoggerConfigDisruptor disruptor = disruptorConfig.getLoggerConfigDisruptor();
        assertThat(disruptor.getRingBuffer().getBufferSize()).isEqualTo(expectedBufferSize);
    }

    @Override
    protected long asyncRemainingCapacity(final Logger logger) {
        if (logger instanceof final AsyncLogger asyncLogger) {
            return Optional.of(asyncLogger.getAsyncLoggerDisruptor())
                    .map(AsyncLoggerDisruptor::getRingBuffer)
                    .map(RingBuffer::remainingCapacity)
                    .orElse(0L);
        } else {
            final LoggerConfig loggerConfig = ((org.apache.logging.log4j.core.Logger) logger).get();
            if (loggerConfig instanceof final AsyncLoggerConfig asyncLoggerConfig) {
                return Optional.ofNullable(asyncLoggerConfig.getAsyncLoggerConfigDisruptor())
                        .map(AsyncLoggerConfigDisruptor::getRingBuffer)
                        .map(RingBuffer::remainingCapacity)
                        .orElse(0L);
            }
        }
        return super.asyncRemainingCapacity(logger);
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

    protected class DomainObject {

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
}
