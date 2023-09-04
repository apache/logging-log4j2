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
package org.apache.logging.log4j.core.appender.mom.jeromq;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.core.util.ExecutorServices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import zmq.SocketBase;
import zmq.ZMQ;
import zmq.socket.pubsub.XPub;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("zeromq")
@Timeout(value = 20, unit = TimeUnit.SECONDS)
@LoggerContextSource(value = "JeroMqAppenderTest.xml", timeout = 60)
public class JeroMqAppenderTest {

    private static final String ENDPOINT = "tcp://localhost:5556";

    private static final String APPENDER_NAME = "JeroMQAppender";

    private static final int DEFAULT_TIMEOUT_MILLIS = 60000;

    @Test
    public void testAppenderLifeCycle() throws Exception {
        // do nothing to make sure the appender starts and stops without
        // locking up resources.
        assertNotNull(JeroMqManager.getContext());
    }

    @Test
    public void testClientServer(@Named(APPENDER_NAME) final JeroMqAppender appender, final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger(getClass());
        final int expectedReceiveCount = 3;
        final JeroMqTestClient client = new JeroMqTestClient(JeroMqManager.getContext(), ENDPOINT, expectedReceiveCount);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<List<String>> future = executor.submit(client);
            waitForSubscription(appender, 5000);
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
        }
    }

    @Test
    public void testMultiThreadedServer(@Named(APPENDER_NAME) final JeroMqAppender appender, final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger(getClass());
        final int nThreads = 10;
        final int expectedReceiveCount = 2 * nThreads;
        final JeroMqTestClient client = new JeroMqTestClient(JeroMqManager.getContext(), ENDPOINT,
                expectedReceiveCount);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<List<String>> future = executor.submit(client);
            waitForSubscription(appender, 5000);
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
            ExecutorServices.shutdown(executor, DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS,
                    JeroMqAppenderTest.class.getSimpleName());
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

    private void waitForSubscription(final JeroMqAppender appender, final int timeoutMs) throws Exception {
        final long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (hasSubscriptions(appender)) {
                return;
            }
            Thread.currentThread().sleep(100);
        }
        throw new TimeoutException();
    }

    private boolean hasSubscriptions(final JeroMqAppender appender) {
        final SocketBase publisher = appender.getManager().getPublisher();
        // Process commands
        publisher.getSocketOpt(ZMQ.ZMQ_EVENTS);
        return assertDoesNotThrow(() -> {
            final Field subscriptionsField = XPub.class.getDeclaredField("subscriptions");
            subscriptionsField.setAccessible(true);
            final Object subscriptions = subscriptionsField.get(publisher);
            final Field pipesField = Class.forName("zmq.socket.pubsub.Mtrie").getDeclaredField("pipes");
            pipesField.setAccessible(true);
            final Set<?> pipes = (Set<?>) pipesField.get(subscriptions);
            return pipes != null && !pipes.isEmpty();
        });
    }
}
