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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class AsyncAppenderTest {

    static void exceptionTest(final LoggerContext context) throws InterruptedException {
        assertNotNull(context);
        final ExtendedLogger logger = context.getLogger(AsyncAppender.class);
        final Exception parent = new IllegalStateException("Test");
        final Throwable child = new LoggingException("This is a test", parent);
        logger.error("This is a test", child);
        final ListAppender appender = context.getConfiguration().getAppender("List");
        final List<String> messages;
        try {
            messages = appender.getMessages(1, 2, TimeUnit.SECONDS);
        } finally {
            appender.clear();
        }
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains(parent.getClass().getName()));
    }

    static void rewriteTest(final LoggerContext context) throws InterruptedException {
        assertNotNull(context);
        final ExtendedLogger logger = context.getLogger(AsyncAppender.class);
        logger.error("This is a test");
        logger.warn("Hello world!");
        final ListAppender appender = context.getConfiguration().getAppender("List");
        final List<String> messages;
        try {
            messages = appender.getMessages(2, 2, TimeUnit.SECONDS);
        } finally {
            appender.clear();
        }
        assertNotNull(messages);
        assertEquals(2, messages.size());
        final String messagePrefix = AsyncAppenderTest.class.getName() + " rewriteTest ";
        assertEquals(messagePrefix + "This is a test", messages.get(0));
        assertEquals(messagePrefix + "Hello world!", messages.get(1));
    }

    @Test
    @LoggerContextSource("BlockingQueueFactory-ArrayBlockingQueue.xml")
    public void testArrayBlockingQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @LoggerContextSource("log4j-asynch.xml")
    public void testDefaultAsyncAppenderConfig(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);

        final List<Thread> backgroundThreads = Thread.getAllStackTraces().keySet().stream()
                .filter(AsyncAppenderEventDispatcher.class::isInstance)
                .collect(Collectors.toList());
        assertFalse(backgroundThreads.isEmpty(), "Failed to locate background thread");
        for (Thread thread : backgroundThreads) {
            assertTrue(thread.isDaemon(), "AsyncAppender should use daemon threads");
        }
    }

    @Test
    @Tag("disruptor")
    @LoggerContextSource("BlockingQueueFactory-DisruptorBlockingQueue.xml")
    public void testDisruptorBlockingQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @LoggerContextSource("log4j-asynch.xml")
    public void testGetAppenderRefStrings(final LoggerContext context) throws InterruptedException {
        final AsyncAppender appender = context.getConfiguration().getAppender("Async");
        assertArrayEquals(new String[] {"List"}, appender.getAppenderRefStrings());
        assertNotSame(appender.getAppenderRefStrings(), appender.getAppenderRefStrings());
    }

    @Test
    @LoggerContextSource("log4j-asynch.xml")
    public void testGetAppenders(final LoggerContext context) throws InterruptedException {
        final AsyncAppender appender = context.getConfiguration().getAppender("Async");
        final List<Appender> appenders = appender.getAppenders();
        assertEquals(1, appenders.size());
        final Appender listAppender = appenders.get(0);
        assertEquals("List", listAppender.getName());
        assertTrue(listAppender instanceof ListAppender);
    }

    @Test
    @LoggerContextSource("log4j-asynch.xml")
    public void testGetErrorRef(final LoggerContext context) throws InterruptedException {
        final AsyncAppender appender = context.getConfiguration().getAppender("Async");
        assertEquals("STDOUT", appender.getErrorRef());
    }

    @Test
    @Tag("jctools")
    @LoggerContextSource("BlockingQueueFactory-JCToolsBlockingQueue.xml")
    public void testJcToolsBlockingQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @LoggerContextSource("BlockingQueueFactory-LinkedTransferQueue.xml")
    public void testLinkedTransferQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @LoggerContextSource("log4j-asynch-no-location.xml")
    public void testNoLocationInformation(final LoggerContext context, @Named("List") final ListAppender appender)
            throws InterruptedException {
        final ExtendedLogger logger = context.getLogger(getClass());
        logger.error("This is a test");
        logger.warn("Hello world!");
        final List<String> messages;
        try {
            messages = appender.getMessages(2, 2, TimeUnit.SECONDS);
        } finally {
            appender.clear();
        }
        assertNotNull(messages);
        assertEquals(2, messages.size());
        assertEquals("?  This is a test", messages.get(0));
        assertEquals("?  Hello world!", messages.get(1));
    }

    @Test
    @Timeout(5)
    @LoggerContextSource("log4j-asynch-shutdownTimeout.xml")
    public void testShutdownTimeout(final LoggerContext context) {
        context.getLogger("Logger").info("This is a test");
        context.stop();
    }
}
