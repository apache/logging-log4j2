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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.lmax.disruptor.dsl.Disruptor;

/**
 * Tests queue full scenarios abstract superclass.
 */
public abstract class QueueFullAbstractTest {
    protected static boolean TRACE = false;
    protected BlockingAppender blockingAppender;
    protected Unlocker unlocker;

    protected static void TRACE(final Object msg) {
        if (TRACE) {
            System.out.println(msg);
        }
    }

    class Unlocker extends Thread {
        final CountDownLatch countDownLatch;
        Unlocker(final CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }
        @Override
        public void run() {
            try {
                countDownLatch.await();
                TRACE("Unlocker activated. Sleeping 500 millis before taking action...");
                Thread.sleep(500);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            TRACE("Unlocker signalling BlockingAppender to proceed...");
            blockingAppender.countDownLatch.countDown();
        }
    }

    class DomainObject {
        private final Logger innerLogger = LogManager.getLogger(DomainObject.class);
        final int count;
        DomainObject(final int loggingCount) {
            this.count = loggingCount;
        }

        @Override
        public String toString() {
            for (int i = 0; i < count; i++) {
                TRACE("DomainObject decrementing unlocker countdown latch before logging. Count was " + unlocker.countDownLatch.getCount());
                unlocker.countDownLatch.countDown();
                TRACE("DomainObject logging message " + i  + ". Remaining capacity=" + asyncRemainingCapacity(innerLogger));
                innerLogger.info("Logging in toString() #" + i);
            }
            return "Who's bad?!";
        }
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
                return ((AsyncLoggerDisruptor) f.get(logger)).getDisruptor().getRingBuffer().remainingCapacity();
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            final LoggerConfig loggerConfig = ((org.apache.logging.log4j.core.Logger) logger).get();
            if (loggerConfig instanceof AsyncLoggerConfig) {
                try {
                    final Object delegate = field(AsyncLoggerConfig.class, "delegate").get(loggerConfig);
                    return ((Disruptor) field(AsyncLoggerConfigDisruptor.class, "disruptor").get(delegate)).getRingBuffer().remainingCapacity();
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
    private static Field field(final Class<?> c, final String name) throws NoSuchFieldException {
        final Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}