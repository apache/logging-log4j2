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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests LOG4J2-1688 Multiple loggings of arguments are setting these arguments to null.
 */
@Tag(Tags.ASYNC_LOGGERS)
public class Log4j2Jira1688Test {

    private ListAppender listAppender;

    @BeforeAll
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "log4j-list.xml");
    }

    @AfterAll
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @BeforeEach
    public void before(LoggerContext context) {
        listAppender = context.getConfiguration().getAppender("List");
    }

    private static Object[] createArray(final int size) {
        final Object[] args = new Object[size];
        for (int i = 0; i < args.length; i++) {
            args[i] = i;
        }
        return args;
    }

    @Test
    @LoggerContextSource("log4j-list.xml")
    public void testLog4j2Only(LoggerContext context) throws InterruptedException {
        final org.apache.logging.log4j.Logger log4JLogger = LogManager.getLogger(this.getClass());
        final int limit = 11; // more than unrolled varargs
        final Object[] args = createArray(limit);
        final Object[] originalArgs = Arrays.copyOf(args, args.length);

        listAppender.countDownLatch = new CountDownLatch(1);
        ((ExtendedLogger) log4JLogger).logIfEnabled("test", Level.ERROR, null, "test {}", args);

        listAppender.countDownLatch.await(1, TimeUnit.SECONDS);
        assertArrayEquals(originalArgs, args, Arrays.toString(args));

        ((ExtendedLogger) log4JLogger).logIfEnabled("test", Level.ERROR, null, "test {}", args);
        assertArrayEquals(originalArgs, args, Arrays.toString(args));
    }
}
