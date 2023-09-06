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
package org.apache.logging.log4j.jeromq.appender;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.core.util.ExecutorServices;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.zeromq.ZMonitor;
import org.zeromq.ZMonitor.Event;
import org.zeromq.ZMonitor.ZEvent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("zeromq")
@Timeout(value = 20, unit = TimeUnit.SECONDS)
@LoggerContextSource(value = "JeroMqAppenderTest.xml", timeout = 60)
public class JeroMqAppenderTest {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String APPENDER_NAME = "JeroMQAppender";

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    @Test
    public void testAppenderLifeCycle() throws Exception {
        // do nothing to make sure the appender starts and stops without
        // locking up resources.
        assertNotNull(JeroMqManager.getZContext());
    }

    @Test
    public void testClientServer(@Named(APPENDER_NAME) final JeroMqAppender appender, final LoggerContext ctx)
            throws Exception {
        final Logger logger = ctx.getLogger(getClass());
        final int expectedReceiveCount = 3;
        final String endpoint = getTcpEndpoint(appender);
        final JeroMqTestClient client = new JeroMqTestClient(JeroMqManager.getZContext(), endpoint,
                expectedReceiveCount);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final ZMonitor monitor = createMonitor(appender);
        try {
            final Future<List<String>> future = executor.submit(client);
            waitAtMost(DEFAULT_TIMEOUT_MS, MILLISECONDS).until(() -> hasEventOccurred(monitor, Event.ACCEPTED));
            appender.resetSendRcs();
            logger.info("Hello");
            logger.info("Again");
            ThreadContext.put("foo", "bar");
            logger.info("World");
            final List<String> list = future.get();
            assertEquals(expectedReceiveCount, appender.getSendRcTrue());
            assertEquals(0, appender.getSendRcFalse());
            assertEquals("Hello", list.get(0));
            assertEquals("Again", list.get(1));
            assertEquals("barWorld", list.get(2));
        } finally {
            executor.shutdown();
            waitAtMost(DEFAULT_TIMEOUT_MS, MILLISECONDS).until(() -> hasEventOccurred(monitor, Event.DISCONNECTED));
            monitor.destroy();
        }
    }

    @Test
    public void testMultiThreadedServer(@Named(APPENDER_NAME) final JeroMqAppender appender, final LoggerContext ctx)
            throws Exception {
        final Logger logger = ctx.getLogger(getClass());
        final int nThreads = 10;
        final int expectedReceiveCount = 2 * nThreads;
        final String endpoint = getTcpEndpoint(appender);
        final JeroMqTestClient client = new JeroMqTestClient(JeroMqManager.getZContext(), endpoint,
                expectedReceiveCount);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final ZMonitor monitor = createMonitor(appender);
        try {
            final Future<List<String>> future = executor.submit(client);
            waitAtMost(DEFAULT_TIMEOUT_MS, MILLISECONDS).until(() -> hasEventOccurred(monitor, Event.ACCEPTED));
            appender.resetSendRcs();
            final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < 10.; i++) {
                fixedThreadPool.submit(() -> {
                    logger.info("Hello");
                    logger.info("Again");
                });
            }
            final List<String> list = future.get();
            assertEquals(expectedReceiveCount, appender.getSendRcTrue());
            assertEquals(0, appender.getSendRcFalse());
            int hello = 0;
            int again = 0;
            for (final String string : list) {
                switch (string) {
                    case "Hello":
                        hello++;
                        break;
                    case "Again":
                        again++;
                        break;
                    default:
                        fail("Unexpected message: " + string);
                }
            }
            assertEquals(nThreads, hello);
            assertEquals(nThreads, again);
        } finally {
            ExecutorServices.shutdown(executor, DEFAULT_TIMEOUT_MS, MILLISECONDS,
                    JeroMqAppenderTest.class.getSimpleName());
            waitAtMost(DEFAULT_TIMEOUT_MS, MILLISECONDS).until(() -> hasEventOccurred(monitor, Event.DISCONNECTED));
            monitor.destroy();
        }
    }

    @Test
    public void testServerOnly(@Named(APPENDER_NAME) final JeroMqAppender appender, final LoggerContext ctx) {
        final Logger logger = ctx.getLogger(getClass());
        appender.resetSendRcs();
        logger.info("Hello");
        logger.info("Again");
        assertEquals(2, appender.getSendRcTrue());
        assertEquals(0, appender.getSendRcFalse());
    }


    private String getTcpEndpoint(final JeroMqAppender appender) {
        for (final String endpoint : appender.getManager().getEndpoints()) {
            if (endpoint.startsWith("tcp://0.0.0.0")) {
                return endpoint.replace("0.0.0.0", "localhost");
            }
        }
        fail("No TCP endpoint found.");
        return null;
    }

    private boolean hasEventOccurred(final ZMonitor monitor, final Event eventType) {
        ZEvent event;
        while ((event = monitor.nextEvent(false)) != null) {
            if (event.type == eventType) {
                return true;
            }
        }
        return false;
    }

    private ZMonitor createMonitor(final JeroMqAppender appender) {
        final ZMonitor monitor = new ZMonitor(JeroMqManager.getZContext(), appender.getManager().getSocket());
        monitor.add(Event.ACCEPTED, Event.DISCONNECTED);
        monitor.start();
        return monitor;
    }
}
