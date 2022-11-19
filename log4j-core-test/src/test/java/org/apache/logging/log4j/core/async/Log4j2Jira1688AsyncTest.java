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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests LOG4J2-1688 Multiple loggings of arguments are setting these arguments to null.
 */
@RunWith(BlockJUnit4ClassRunner.class)
@Category(AsyncLoggers.class)
public class Log4j2Jira1688AsyncTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME,
                AsyncLoggerContextSelector.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME);
    }

    @Rule
    public LoggerContextRule context = new LoggerContextRule("log4j-list.xml");
    private ListAppender listAppender;

    @Before
    public void before() throws Exception {
        listAppender = context.getListAppender("List");
    }

    private static Object[] createArray(final int size) {
        final Object[] args = new Object[size];
        for (int i = 0; i < args.length; i++) {
            args[i] = i;
        }
        return args;
    }

    @Test
    public void testLog4j2Only() throws InterruptedException {
        final ExtendedLogger log4JLogger = context.getLogger(this.getClass());
        final int limit = 11; // more than unrolled varargs
        final Object[] args = createArray(limit);
        final Object[] originalArgs = Arrays.copyOf(args, args.length);

        listAppender.countDownLatch = new CountDownLatch(1);
        log4JLogger.logIfEnabled("test", Level.ERROR, null, "test {}", args);

        listAppender.countDownLatch.await(1, TimeUnit.SECONDS);
        assertArrayEquals(Arrays.toString(args), originalArgs, args);

        log4JLogger.logIfEnabled("test", Level.ERROR, null, "test {}", args);
        assertArrayEquals(Arrays.toString(args), originalArgs, args);
    }

}
