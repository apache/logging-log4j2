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
package org.apache.logging.jul.tolog4j.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.async.logger.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.test.TestConstants;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.jspecify.annotations.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(AsyncLoggers.class)
public class AsyncLoggerThreadsTest {

    static @Nullable String oldSelector;

    @BeforeClass
    public static void beforeClass() {
        oldSelector = TestConstants.setSystemProperty(
                TestConstants.LOGGER_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        TestConstants.setSystemProperty(TestConstants.LOGGER_CONTEXT_SELECTOR, oldSelector);
        System.clearProperty("java.util.logging.manager");
    }

    @Test
    public void testAsyncLoggerThreads() {
        LogManager.getLogger("com.foo.Bar").info("log");
        final List<Thread> asyncLoggerThreads = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().matches("Log4j2-TF.*AsyncLogger.*"))
                .collect(Collectors.toList());
        assertEquals(asyncLoggerThreads.toString(), 1, asyncLoggerThreads.size());
    }
}
