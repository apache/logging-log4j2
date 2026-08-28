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
package org.apache.logging.log4j.core.util.internal;

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
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
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
        final Field registryField = loggerContext.getClass().getDeclaredField("loggerRegistry");
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
        final Logger logger = registry.computeIfAbsent(
                "testLogger", messageFactory, (name, factory) -> new Logger(loggerContext, name, factory) {});
        assertNotNull(logger);
        assertEquals("testLogger", logger.getName());
    }

    @Test
    void testGetLoggerRetrievesExistingLogger() {
        final Logger logger = registry.computeIfAbsent(
                "testLogger", messageFactory, (name, factory) -> new Logger(loggerContext, name, factory) {});
        assertSame(logger, registry.getLogger("testLogger", messageFactory));
    }

    @Test
    void testHasLoggerReturnsCorrectStatus() {
        assertFalse(registry.hasLogger("testLogger", messageFactory));
        registry.computeIfAbsent(
                "testLogger", messageFactory, (name, factory) -> new Logger(loggerContext, name, factory) {});
        assertTrue(registry.hasLogger("testLogger", messageFactory));
    }

    @Test
    void testExpungeStaleWeakReferenceEntries() {
        final String loggerNamePrefix = "testLogger_";
        final int numberOfLoggers = 1000;

        for (int i = 0; i < numberOfLoggers; i++) {
            final Logger logger = registry.computeIfAbsent(
                    loggerNamePrefix + i,
                    messageFactory,
                    (name, factory) -> new Logger(loggerContext, name, factory) {});
            logger.info("Using logger {}", logger.getName());
        }

        await().atMost(10, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(() -> {
            System.gc();
            registry.computeIfAbsent(
                    "triggerExpunge", messageFactory, (name, factory) -> new Logger(loggerContext, name, factory) {});

            final Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerRefByNameByMessageFactory =
                    reflectAndGetLoggerMapFromRegistry();
            final Map<String, WeakReference<Logger>> loggerRefByName =
                    loggerRefByNameByMessageFactory.get(messageFactory);

            int unexpectedCount = 0;
            for (int i = 0; i < numberOfLoggers; i++) {
                if (loggerRefByName.containsKey(loggerNamePrefix + i)) {
                    unexpectedCount++;
                }
            }
            assertEquals(
                    0, unexpectedCount, "Found " + unexpectedCount + " unexpected stale entries for MessageFactory");
        });
    }

    @Test
    void testExpungeStaleMessageFactoryEntry() {
        final SimpleMessageFactory mockMessageFactory = new SimpleMessageFactory();
        Logger logger = registry.computeIfAbsent(
                "testLogger", mockMessageFactory, (name, factory) -> new Logger(loggerContext, name, factory) {});
        logger.info("Using logger {}", logger.getName());
        logger = null;

        await().atMost(10, SECONDS).pollInterval(100, MILLISECONDS).untilAsserted(() -> {
            System.gc();
            registry.getLogger("triggerExpunge", mockMessageFactory);

            final Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerRefByNameByMessageFactory =
                    reflectAndGetLoggerMapFromRegistry();
            assertNull(
                    loggerRefByNameByMessageFactory.get(mockMessageFactory),
                    "Stale MessageFactory entry was not removed from the outer map");
        });
    }

    private Map<MessageFactory, Map<String, WeakReference<Logger>>> reflectAndGetLoggerMapFromRegistry()
            throws NoSuchFieldException, IllegalAccessException {
        final Field loggerMapField = registry.getClass().getDeclaredField("loggerRefByNameByMessageFactory");
        loggerMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<MessageFactory, Map<String, WeakReference<Logger>>> loggerMap =
                (Map<MessageFactory, Map<String, WeakReference<Logger>>>) loggerMapField.get(registry);
        return loggerMap;
    }
}
