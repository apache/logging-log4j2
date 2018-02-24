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

import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Tests queue full scenarios with pure AsyncLoggers (all loggers async).
 */
@RunWith(BlockJUnit4ClassRunner.class)
@Category(AsyncLoggers.class)
public class QueueFullAsyncLoggerTest2 extends QueueFullAbstractTest {

    @BeforeClass
    public static void beforeClass() {
        //FORMAT_MESSAGES_IN_BACKGROUND
        System.setProperty("log4j.format.msg.async", "true");

        System.setProperty("AsyncLogger.RingBufferSize", "128"); // minimum ringbuffer size
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "log4j2-queueFull.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @Rule
    public LoggerContextRule context = new LoggerContextRule(
            "log4j2-queueFull.xml", AsyncLoggerContextSelector.class);

    @Before
    public void before() throws Exception {
        blockingAppender = context.getRequiredAppender("Blocking", BlockingAppender.class);
    }


    @Test(timeout = 5000)
    public void testNormalQueueFullKeepsMessagesInOrder() throws InterruptedException {
        final Logger logger = LogManager.getLogger(this.getClass());

        blockingAppender.countDownLatch = new CountDownLatch(1);
        unlocker = new Unlocker(new CountDownLatch(129));
        unlocker.start();

        QueueFullAsyncLoggerTest.asyncLoggerTest(logger, unlocker, blockingAppender);
    }
}