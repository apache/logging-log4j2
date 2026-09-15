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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(Tags.ASYNC_LOGGERS)
class AsyncLoggerUncachedThreadNameTest {

    @BeforeAll
    static void beforeClass() {
        System.setProperty("log4j2.asyncLoggerThreadNameStrategy", "UNCACHED");
        System.setProperty("log4j2.contextSelector", AsyncLoggerContextSelector.class.getName());
        System.setProperty("log4j2.configurationFile", "AsyncLoggerTest.xml");
    }

    @AfterAll
    static void afterClass() {
        System.clearProperty("log4j2.asyncLoggerThreadNameStrategy");
        System.clearProperty("log4j2.contextSelector");
        System.clearProperty("log4j2.configurationFile");
    }

    @Test
    void testAsyncLogUsesCurrentThreadName() throws Exception {
        final File file = new File("target", "AsyncLoggerTest.log");
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");

        final String msg = "Async logger msg";
        final AtomicReference<Thread> firstTaskThreadRef = new AtomicReference<>();
        final AtomicReference<Thread> secondTaskThreadRef = new AtomicReference<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<?> firstTask = executor.submit(() -> {
                final Thread currentThread = Thread.currentThread();
                final String originalThreadName = currentThread.getName();
                final String expectedThreadName = "MODIFIED-THREADNAME-1";
                firstTaskThreadRef.set(currentThread);
                try {
                    currentThread.setName(expectedThreadName);
                    log.info(msg);
                } finally {
                    currentThread.setName(originalThreadName);
                }
                return expectedThreadName;
            });
            final Future<?> secondTask = executor.submit(() -> {
                final Thread currentThread = Thread.currentThread();
                final String originalThreadName = currentThread.getName();
                final String expectedThreadName = "MODIFIED-THREADNAME-2";
                secondTaskThreadRef.set(currentThread);
                try {
                    currentThread.setName(expectedThreadName);
                    log.info(msg);
                } finally {
                    currentThread.setName(originalThreadName);
                }
                return expectedThreadName;
            });
            final String firstTaskThreadName = (String) firstTask.get();
            final String secondTaskThreadName = (String) secondTask.get();
            assertNotNull(firstTaskThreadName, "firstTaskThreadName");
            assertNotNull(secondTaskThreadName, "secondTaskThreadName");
            assertSame(firstTaskThreadRef.get(), secondTaskThreadRef.get(), "same async worker thread");
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        CoreLoggerContexts.stopLoggerContext(file);

        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final String line1 = reader.readLine();
            final String line2 = reader.readLine();
            assertNotNull(line1, "line1");
            assertNotNull(line2, "line2");
            assertTrue(line1.contains("MODIFIED-THREADNAME-1"), "line1");
            assertTrue(line2.contains("MODIFIED-THREADNAME-2"), "line2");
        }
    }
}
