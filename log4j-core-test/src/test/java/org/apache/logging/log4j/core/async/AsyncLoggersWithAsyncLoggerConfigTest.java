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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.di.Injector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("async")
public class AsyncLoggersWithAsyncLoggerConfigTest {

    @SingletonFactory
    public ContextSelector contextSelector(final Injector injector) {
        return new AsyncLoggerContextSelector(injector);
    }

    @Test
    @LoggerContextSource("AsyncLoggersWithAsyncLoggerConfigTest.xml")
    public void testLoggingWorks(final Logger logger, @Named("List") final ListAppender appender) throws Exception {
        logger.error("This is a test");
        logger.warn("Hello world!");
        final List<String> list = appender.getMessages(2, 100, TimeUnit.MILLISECONDS);
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
