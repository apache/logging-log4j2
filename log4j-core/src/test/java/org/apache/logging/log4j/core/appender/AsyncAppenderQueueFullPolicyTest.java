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
package org.apache.logging.log4j.core.appender;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.DefaultAsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.BlockingAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the AsyncAppender (LOG4J2-1080) event routing logic:
 * <pre>
 * If not BLOCKING, then offer the event to the queue and send to error appender if queue full.
 * If BLOCKING, then (LOG4J2-471)
 *     if queue full (non-blocking call to queue.offer(event) failed) then
 *         if thread==backgroundThread delegate to event router
 *         else queue.add(event) // blocking call
 * </pre>
 */
public class AsyncAppenderQueueFullPolicyTest {
    private static final String CONFIG = "log4j-asynch-queue-full.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private BlockingAppender blockingAppender;
    private AsyncAppender asyncAppender;
    private CountingAsyncQueueFullPolicy policy;

    @Before
    public void before() throws Exception {
        blockingAppender = context.getAppender("Block", BlockingAppender.class);
        asyncAppender = context.getAppender("Async", AsyncAppender.class);

        final Field field = AsyncAppender.class.getDeclaredField("asyncQueueFullPolicy");
        field.setAccessible(true);
        policy = new CountingAsyncQueueFullPolicy();
        field.set(asyncAppender, policy);
        policy.queueFull.set(0L);
    }

    @After
    public void after() {
        blockingAppender.running = false;
        policy.queueFull.set(0L);
    }

    @Test
    public void testRouter() throws Exception {
        final Logger logger = LogManager.getLogger(AsyncAppenderQueueFullPolicyTest.class);

        assertEquals(4, asyncAppender.getQueueCapacity());
        logger.error("event 1 - gets taken off the queue");
        logger.warn("event 2");
        logger.info("event 3");
        logger.info("event 4");
        while (asyncAppender.getQueueRemainingCapacity() == 0) {
            Thread.yield(); // wait until background thread takes one element off the queue
        }
        logger.info("event 5 - now the queue is full");
        assertEquals("queue remaining capacity", 0, asyncAppender.getQueueRemainingCapacity());
        assertEquals("EventRouter invocations", 0, policy.queueFull.get());

        final Thread release = new Thread("AsyncAppenderReleaser") {
            @Override
            public void run() {
                while (policy.queueFull.get() == 0) {
                    try {
                        Thread.sleep(10L);
                    } catch (final InterruptedException ignored) {
                        //ignored
                    }
                }
                blockingAppender.running = false;
            }
        };
        release.setDaemon(true);
        release.start();
        logger.fatal("this blocks until queue space available");
        assertEquals(1, policy.queueFull.get());
    }

    public static class CountingAsyncQueueFullPolicy extends DefaultAsyncQueueFullPolicy {
        AtomicLong queueFull = new AtomicLong();
        @Override
        public EventRoute getRoute(final long backgroundThreadId, final Level level) {
            queueFull.incrementAndGet();
            return EventRoute.ENQUEUE;
        }
    }
}
