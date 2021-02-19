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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class AsyncAppenderTest {

    static void rewriteTest(final LoggerContext context) throws InterruptedException {
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
        assertThat(messages).isNotNull();
        assertThat(messages.size()).isEqualTo(2);
        final String messagePrefix = AsyncAppenderTest.class.getName() + " rewriteTest ";
        assertThat(messages.get(0)).isEqualTo(messagePrefix + "This is a test");
        assertThat(messages.get(1)).isEqualTo(messagePrefix + "Hello world!");
    }

    static void exceptionTest(final LoggerContext context) throws InterruptedException {
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
        assertThat(messages).isNotNull();
        assertThat(messages.size()).isEqualTo(1);
        assertThat(messages.get(0).contains(parent.getClass().getName())).isTrue();
    }

    @Test
    @LoggerContextSource("log4j-asynch.xml")
    void defaultAsyncAppenderConfig(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @LoggerContextSource("BlockingQueueFactory-ArrayBlockingQueue.xml")
    void arrayBlockingQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @Tag("disruptor")
    @LoggerContextSource("BlockingQueueFactory-DisruptorBlockingQueue.xml")
    void disruptorBlockingQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @Tag("jctools")
    @LoggerContextSource("BlockingQueueFactory-JCToolsBlockingQueue.xml")
    void jcToolsBlockingQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @LoggerContextSource("BlockingQueueFactory-LinkedTransferQueue.xml")
    void linkedTransferQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }

    @Test
    @Timeout(5)
    @LoggerContextSource("log4j-asynch-shutdownTimeout.xml")
    void shutdownTimeout(final LoggerContext context) {
        context.getLogger("Logger").info("This is a test");
        context.stop();
    }

    @Test
    @LoggerContextSource("log4j-asynch-no-location.xml")
    void noLocationInformation(final LoggerContext context, @Named("List") final ListAppender appender) throws InterruptedException {
        final ExtendedLogger logger = context.getLogger(getClass());
        logger.error("This is a test");
        logger.warn("Hello world!");
        final List<String> messages;
        try {
            messages = appender.getMessages(2, 2, TimeUnit.SECONDS);
        } finally {
            appender.clear();
        }
        assertThat(messages).isNotNull();
        assertThat(messages.size()).isEqualTo(2);
        assertThat(messages.get(0)).isEqualTo("?  This is a test");
        assertThat(messages.get(1)).isEqualTo("?  Hello world!");
    }
}
