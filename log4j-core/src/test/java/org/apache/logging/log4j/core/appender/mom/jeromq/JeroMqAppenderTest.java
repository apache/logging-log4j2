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

package org.apache.logging.log4j.core.appender.mom.jeromq;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class JeroMqAppenderTest {

    @AfterClass
    public static void tearDownClass() {
        // JeroMqAppender.shutdown();
    }

    @ClassRule
    public static LoggerContextRule ctx = new LoggerContextRule("JeroMqAppenderTest.xml");

    @Test(timeout = 10000)
    public void testAppenderLifeCycle() throws Exception {
        // do nothing to make sure the appender starts and stops without
        // locking up resources.
        Assert.assertNotNull(JeroMqAppender.getContext());
    }

    @Test(timeout = 10000)
    public void testClientServer() throws Exception {
        final JeroMqAppender appender = ctx.getRequiredAppender("JeroMQAppender", JeroMqAppender.class);
        final int expectedReceiveCount = 3;
        final JeroMqTestClient client = new JeroMqTestClient(JeroMqAppender.getContext(), "tcp://localhost:5556", expectedReceiveCount);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<List<String>> future = executor.submit(client);
            Thread.sleep(100);
            final Logger logger = ctx.getLogger(getClass().getName());
            appender.resetSendRcs();
            logger.info("Hello");
            logger.info("Again");
            ThreadContext.put("foo", "bar");
            logger.info("World");
            final List<String> list = future.get();
            Assert.assertEquals(expectedReceiveCount, appender.getSendRcTrue());
            Assert.assertEquals(0, appender.getSendRcFalse());
            Assert.assertEquals("Hello", list.get(0));
            Assert.assertEquals("Again", list.get(1));
            Assert.assertEquals("barWorld", list.get(2));
        } finally {
            executor.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void testMultiThreadedServer() throws Exception {
        final int nThreads = 10;
        final JeroMqAppender appender = ctx.getRequiredAppender("JeroMQAppender", JeroMqAppender.class);
        final int expectedReceiveCount = 2 * nThreads;
        final JeroMqTestClient client = new JeroMqTestClient(JeroMqAppender.getContext(), "tcp://localhost:5556",
                expectedReceiveCount);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<List<String>> future = executor.submit(client);
            Thread.sleep(100);
            final Logger logger = ctx.getLogger(getClass().getName());
            appender.resetSendRcs();
            final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < 10.; i++) {
                fixedThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        logger.info("Hello");
                        logger.info("Again");
                    }
                });
            }
            final List<String> list = future.get();
            Assert.assertEquals(expectedReceiveCount, appender.getSendRcTrue());
            Assert.assertEquals(0, appender.getSendRcFalse());
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
                    Assert.fail("Unexpected message: " + string);
                }
            }
            Assert.assertEquals(nThreads, hello);
            Assert.assertEquals(nThreads, again);
        } finally {
            executor.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void testServerOnly() throws Exception {
        final Logger logger = ctx.getLogger(getClass().getName());
        final JeroMqAppender appender = ctx.getRequiredAppender("JeroMQAppender", JeroMqAppender.class);
        appender.resetSendRcs();
        logger.info("Hello");
        logger.info("Again");
        Assert.assertEquals(2, appender.getSendRcTrue());
        Assert.assertEquals(0, appender.getSendRcFalse());
    }
}
