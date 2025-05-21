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
package org.apache.logging.log4j.core.test.util;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.internal.InternalLoggerRegistry;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class InternalLoggerRegistryTest {
    private LoggerContext loggerContext;
    private InternalLoggerRegistry registry;
    private MessageFactory messageFactory;

    @BeforeEach
    void setUp(TestInfo testInfo) throws NoSuchFieldException, IllegalAccessException {
        loggerContext = new LoggerContext(testInfo.getDisplayName());
        Field registryField = loggerContext.getClass().getDeclaredField("loggerRegistry");
        registryField.setAccessible(true);
        registry = (InternalLoggerRegistry) registryField.get(loggerContext);
        messageFactory = SimpleMessageFactory.INSTANCE;
    }

    @AfterEach
    void tearDown() {
        if (loggerContext != null) {
            loggerContext.stop();
        }
    }

    @Test
    void testGetLoggerReturnsNullForNonExistentLogger() {
        assertNull(registry.getLogger("nonExistent", messageFactory));
    }

    @Test
    void testComputeIfAbsentCreatesLogger() {
        Logger logger = registry.computeIfAbsent(
                "testLogger", messageFactory, (name, factory) -> loggerContext.getLogger(name, factory));
        assertNotNull(logger);
        assertEquals("testLogger", logger.getName());
    }

    @Test
    void testGetLoggerRetrievesExistingLogger() {
        Logger logger = registry.computeIfAbsent(
                "testLogger", messageFactory, (name, factory) -> loggerContext.getLogger(name, factory));
        assertSame(logger, registry.getLogger("testLogger", messageFactory));
    }

    @Test
    void testHasLoggerReturnsCorrectStatus() {
        assertFalse(registry.hasLogger("testLogger", messageFactory));
        registry.computeIfAbsent(
                "testLogger", messageFactory, (name, factory) -> loggerContext.getLogger(name, factory));
        assertTrue(registry.hasLogger("testLogger", messageFactory));
    }

    @Test
    void testExpungeStaleWeakReferenceEntries() {
        String loggerNamePrefix = "testLogger_";
        int numberOfLoggers = 1000;

        for (int i = 0; i < numberOfLoggers; i++) {
            Logger logger = registry.computeIfAbsent(
                    loggerNamePrefix + i, messageFactory, (name, factory) -> loggerContext.getLogger(name, factory));
            logger.info("Using logger {}", logger.getName());
        }

        await().atMost(10, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(() -> {
            System.gc();
            System.runFinalization();
            registry.computeIfAbsent(
                    "triggerExpunge", messageFactory, (name, factory) -> loggerContext.getLogger(name, factory));

            Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerRefByNameByMessageFactory =
                    reflectAndGetLoggerMapFromRegistry();
            Map<String, WeakReference<Logger>> loggerRefByName = loggerRefByNameByMessageFactory.get(messageFactory);

            boolean isExpungeStaleEntries = true;
            for (int i = 0; i < numberOfLoggers; i++) {
                if (loggerRefByName.containsKey(loggerNamePrefix + i)) {
                    isExpungeStaleEntries = false;
                    break;
                }
            }
            assertTrue(
                    isExpungeStaleEntries,
                    "Stale WeakReference entries were not removed from the inner map for MessageFactory");
        });
    }

    @Test
    void testExpungeStaleMessageFactoryEntry() {
        Logger logger = registry.computeIfAbsent(
                "testLogger", messageFactory, (name, factory) -> loggerContext.getLogger(name, factory));
        logger.info("Using logger {}", logger.getName());
        logger = null;

        await().atMost(10, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(() -> {
            System.gc();
            System.runFinalization();
            registry.getLogger("testLogger", messageFactory);

            Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerRefByNameByMessageFactory =
                    reflectAndGetLoggerMapFromRegistry();
            assertNull(
                    loggerRefByNameByMessageFactory.get(messageFactory),
                    "Stale MessageFactory entry was not removed from the outer map");
        });
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                registry.computeIfAbsent(
                        "testLogger", messageFactory, (name, factory) -> loggerContext.getLogger(name, factory));
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        // Verify logger was created and is accessible after concurrent creation
        assertNotNull(
                registry.getLogger("testLogger", messageFactory),
                "Logger should be accessible after concurrent creation");
    }

    private Map<MessageFactory, Map<String, WeakReference<Logger>>> reflectAndGetLoggerMapFromRegistry()
            throws NoSuchFieldException, IllegalAccessException {
        Field loggerMapField = registry.getClass().getDeclaredField("loggerRefByNameByMessageFactory");
        loggerMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerMap =
                (Map<MessageFactory, Map<String, WeakReference<Logger>>>) loggerMapField.get(registry);
        return loggerMap;
    }
}
