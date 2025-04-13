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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(AsyncLoggers.class)
public class AsyncLoggersWithAsyncLoggerConfigTest {

    @ClassRule
    public static LoggerContextRule context =
            new LoggerContextRule("AsyncLoggersWithAsyncLoggerConfigTest.xml", AsyncLoggerContextSelector.class);

    @Test
    public void testLoggingWorks() throws Exception {
        final Logger logger = LogManager.getLogger();
        logger.error("This is a test");
        logger.warn("Hello world!");
        Thread.sleep(100);
        final List<String> list = context.getListAppender("List").getMessages();
        assertNotNull("No events generated", list);
        assertEquals("Incorrect number of events. Expected 2, got " + list.size(), 2, list.size());
        String msg = list.get(0);
        String expected = getClass().getName() + " This is a test";
        assertEquals("Expected " + expected + ", Actual " + msg, expected, msg);
        msg = list.get(1);
        expected = getClass().getName() + " Hello world!";
        assertEquals("Expected " + expected + ", Actual " + msg, expected, msg);
    }
}
