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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.DefaultAsyncEventRouter;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.BlockingAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
public class AsyncAppenderEventRouterTest {
    private static final String CONFIG = "log4j-asynch-queue-full.xml";
    static {
        // this must be set before the Log4j context initializes
        System.setProperty("log4j2.AsyncEventRouter", CountingAsyncEventRouter.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty("log4j2.AsyncEventRouter");
    }

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private BlockingAppender blockingAppender;
    private AsyncAppender asyncAppender;

    @Before
    public void before() throws Exception {
        blockingAppender = (BlockingAppender) context.getAppender("Block");
        asyncAppender = (AsyncAppender) context.getAppender("Async");
    }

    @After
    public void after() {
//        blockingAppender.running = false;
    }

    @Test
    public void testRouter() throws Exception {
        final Logger logger = LogManager.getLogger(AsyncAppenderEventRouterTest.class);

        assertEquals(3, asyncAppender.getQueueCapacity());
        logger.error("event 1 - gets taken off the queue");
        logger.warn("event 2");
        logger.info("event 3");
        logger.info("event 4 - now the queue is full");
        assertEquals("queue remaining capacity", 0, asyncAppender.getQueueRemainingCapacity());
        assertEquals("EventRouter invocations", 0, CountingAsyncEventRouter.queueFull.get());

        Thread release = new Thread("AsyncAppenderReleaser") {
            public void run() {
                while (CountingAsyncEventRouter.queueFull.get() == 0) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ignored) {
                        //ignored
                    }
                }
                blockingAppender.running = false;
            }
        };
        release.setDaemon(true);
        release.start();
        logger.fatal("this blocks until queue space available");
        assertEquals(1, CountingAsyncEventRouter.queueFull.get());
    }

    public static class CountingAsyncEventRouter extends DefaultAsyncEventRouter {
        static AtomicLong queueFull = new AtomicLong();
        @Override
        public EventRoute getRoute(long backgroundThreadId, Level level) {
            queueFull.incrementAndGet();
            return EventRoute.ENQUEUE;
        }
    }
}
