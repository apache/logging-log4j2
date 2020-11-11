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
package org.apache.logging.log4j.jul;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Category(AsyncLoggers.class)
public class AsyncLoggerThreadsTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                AsyncLoggerContextSelector.class.getName());
        System.setProperty("java.util.logging.manager", org.apache.logging.log4j.jul.LogManager.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        System.clearProperty("java.util.logging.manager");
    }

    @Test
    public void testAsyncLoggerThreads() {
        LogManager.getLogger("com.foo.Bar").info("log");
        List<Thread> asyncLoggerThreads = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().matches("Log4j2-TF.*AsyncLogger.*"))
                .collect(Collectors.toList());
        assertEquals(asyncLoggerThreads.toString(), 1, asyncLoggerThreads.size());
    }
}
