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

import com.lmax.disruptor.RingBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

/**
 * Checks the location of events that {@link AsyncLogger} logs in the current thread, because the ring buffer is full.
 */
@SetTestProperty(
        key = Constants.LOG4J_CONTEXT_SELECTOR,
        value = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
@SetTestProperty(key = "log4j2.asyncLoggerRingBufferSize", value = "128")
@SetTestProperty(
        key = "log4j2.asyncQueueFullPolicy",
        value = "org.apache.logging.log4j.core.async.AsyncLoggerQueueFullLocationTest$SynchronousQueueFullPolicy")
@Tag(Tags.ASYNC_LOGGERS)
@Timeout(value = 10, unit = SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
class AsyncLoggerQueueFullLocationTest {

    private static final StackTraceElement EXPLICIT_LOCATION =
            new StackTraceElement("com.example.Caller", "explicit", "Caller.java", 42);

    /**
     * Logs all events in the current thread, when the ring buffer is full.
     */
    public static final class SynchronousQueueFullPolicy implements AsyncQueueFullPolicy {

        @Override
        public EventRoute getRoute(final long backgroundThreadId, final Level level) {
            return EventRoute.SYNCHRONOUS;
        }
    }

    @Test
    @LoggerContextSource("AsyncLoggerQueueFullLocationTest.xml")
    void threadLocalTranslator(final LoggerContext context) throws InterruptedException {
        assertLocationsWithFullRingBuffer(context, true);
    }

    @Test
    @LoggerContextSource("AsyncLoggerQueueFullLocationTest.xml")
    void varargTranslator(final LoggerContext context) throws InterruptedException {
        assertLocationsWithFullRingBuffer(context, false);
    }

    private static void assertLocationsWithFullRingBuffer(final LoggerContext context, final boolean useThreadLocals)
            throws InterruptedException {
        final Thread testThread = Thread.currentThread();
        final CountDownLatch release = new CountDownLatch(1);
        // Locations of the events logged in the test thread, by message
        final Map<String, StackTraceElement> locations = new LinkedHashMap<>();
        final AbstractAppender appender = new AbstractAppender("Recording", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(final LogEvent event) {
                if (Thread.currentThread() == testThread) {
                    locations.put(event.getMessage().getFormattedMessage(), event.getSource());
                } else {
                    // Blocks the background thread, until the test is over
                    try {
                        release.await();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        appender.start();
        final Configuration configuration = context.getConfiguration();
        configuration.addAppender(appender);
        configuration.getRootLogger().addAppender(appender, null, null);
        context.updateLoggers();

        final AsyncLogger logger = (AsyncLogger) context.getLogger(AsyncLoggerQueueFullLocationTest.class);
        final AsyncLoggerDisruptor disruptor = logger.getAsyncLoggerDisruptor();
        disruptor.setUseThreadLocals(useThreadLocals);
        final RingBuffer<RingBufferLogEvent> ringBuffer =
                disruptor.getDisruptor().getRingBuffer();
        try {
            // The first event blocks the background thread, the others fill the ring buffer
            while (ringBuffer.remainingCapacity() > 0) {
                logger.info("filler");
            }
            logger.atInfo().withLocation(EXPLICIT_LOCATION).log("explicit");
            logger.atInfo().log("builder");
            logger.info("classic");
        } finally {
            release.countDown();
        }

        assertThat(locations).containsOnlyKeys("explicit", "builder", "classic");
        assertThat(locations.get("explicit")).isEqualTo(EXPLICIT_LOCATION);
        assertThat(locations.get("builder"))
                .isNotNull()
                .extracting(StackTraceElement::getMethodName)
                .isEqualTo("assertLocationsWithFullRingBuffer");
        assertThat(locations.get("classic"))
                .isNotNull()
                .extracting(StackTraceElement::getMethodName)
                .isEqualTo("assertLocationsWithFullRingBuffer");
    }
}
