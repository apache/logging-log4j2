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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(Tags.ASYNC_LOGGERS)
public class AsyncLoggersWithAsyncLoggerConfigTest {

    @Test
    @LoggerContextSource("AsyncLoggersWithAsyncLoggerConfigTest.xml")
    public void testLoggingWorks(LoggerContext context) throws Exception {
        final Logger logger = LogManager.getLogger();
        logger.error("This is a test");
        logger.warn("Hello world!");
        Thread.sleep(100);
        final ListAppender listAppender = context.getConfiguration().getAppender("List");
        final List<String> list = listAppender.getMessages();

        assertNotNull(list, "No events generated");
        assertEquals(2, list.size(), "Incorrect number of events. Expected 2, got " + list.size());
        String msg = list.get(0);
        String expected = getClass().getName() + " This is a test";
        assertEquals(expected, msg, "Expected " + expected + ", Actual " + msg);
        msg = list.get(1);
        expected = getClass().getName() + " Hello world!";
        assertEquals(expected, msg, "Expected " + expected + ", Actual " + msg);
    }
}
