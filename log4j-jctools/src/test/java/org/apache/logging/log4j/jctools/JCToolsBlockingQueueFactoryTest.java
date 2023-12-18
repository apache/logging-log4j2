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
package org.apache.logging.log4j.jctools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.Test;

class JCToolsBlockingQueueFactoryTest {

    private static void exceptionTest(final LoggerContext context) throws InterruptedException {
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

    private static void rewriteTest(final LoggerContext context) throws InterruptedException {
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
        final String messagePrefix = JCToolsBlockingQueueFactoryTest.class.getName() + " rewriteTest ";
        assertEquals(messagePrefix + "This is a test", messages.get(0));
        assertEquals(messagePrefix + "Hello world!", messages.get(1));
    }

    @Test
    @LoggerContextSource("JCToolsBlockingQueueFactoryTest.xml")
    public void testJcToolsBlockingQueue(final LoggerContext context) throws InterruptedException {
        rewriteTest(context);
        exceptionTest(context);
    }
}
