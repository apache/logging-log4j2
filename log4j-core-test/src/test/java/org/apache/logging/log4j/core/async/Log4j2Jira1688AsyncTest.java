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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests LOG4J2-1688 Multiple loggings of arguments are setting these arguments to null.
 */
@Tag("async")
@LoggerContextSource(value = "log4j-list.xml", selector = AsyncLoggerContextSelector.class)
public class Log4j2Jira1688AsyncTest {

    private static Object[] createArray(final int size) {
        final Object[] args = new Object[size];
        for (int i = 0; i < args.length; i++) {
            args[i] = i;
        }
        return args;
    }

    @Test
    public void testLog4j2Only(@Named("List") final ListAppender listAppender, final ExtendedLogger log4JLogger) throws InterruptedException {
        final int limit = 11; // more than unrolled varargs
        final Object[] args = createArray(limit);
        final Object[] originalArgs = Arrays.copyOf(args, args.length);

        listAppender.countDownLatch = new CountDownLatch(1);
        log4JLogger.logIfEnabled("test", Level.ERROR, null, "test {}", args);

        listAppender.countDownLatch.await(1, TimeUnit.SECONDS);
        assertArrayEquals(originalArgs, args, Arrays.toString(args));

        log4JLogger.logIfEnabled("test", Level.ERROR, null, "test {}", args);
        assertArrayEquals(originalArgs, args, Arrays.toString(args));
    }

}
