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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Tests queue full scenarios with AsyncLoggers in configuration.
 */
@RunWith(BlockJUnit4ClassRunner.class)
@Category(AsyncLoggers.class)
public class QueueFullAsyncLoggerConfigLoggingFromToStringTest2 extends QueueFullAbstractTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Log4jProperties.ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND, "true");
        System.setProperty(Log4jProperties.ASYNC_CONFIG_RING_BUFFER_SIZE, "128");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        System.clearProperty(Log4jProperties.ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND);
        System.clearProperty(Log4jProperties.ASYNC_CONFIG_RING_BUFFER_SIZE);
    }

    @Rule
    public LoggerContextRule context = new LoggerContextRule(
            "log4j2-queueFullAsyncLoggerConfig.xml");

    @Before
    public void before() throws Exception {
        blockingAppender = context.getRequiredAppender("Blocking", BlockingAppender.class);
    }

    @Test(timeout = 5000)
    public void testLoggingFromToStringCausesOutOfOrderMessages() {
        //TRACE = true;
        final Logger logger = context.getLogger(this.getClass());

        blockingAppender.countDownLatch = new CountDownLatch(1);
        unlocker = new Unlocker(new CountDownLatch(129)); // count slightly different from "pure" async loggers
        unlocker.start();

        QueueFullAsyncLoggerConfigLoggingFromToStringTest.asyncLoggerConfigRecursiveTest(logger, unlocker, blockingAppender, this);
    }
}
