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

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.internal.InternalLoggerRegistry;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InternalLoggerRegistryTest {
    private InternalLoggerRegistry registry;
    private MessageFactory messageFactory;

    @BeforeEach
    void setUp() {
        registry = new InternalLoggerRegistry();
        messageFactory = new SimpleMessageFactory();
    }

    @Test
    void testGetLoggerReturnsNullForNonExistentLogger() {
        assertNull(registry.getLogger("nonExistent", messageFactory));
    }

    @Test
    void testComputeIfAbsentCreatesLogger() {
        Logger logger =
                registry.computeIfAbsent("testLogger", messageFactory, (name, factory) -> LoggerContext.getContext()
                        .getLogger(name, factory));
        assertNotNull(logger);
        assertEquals("testLogger", logger.getName());
    }

    @Test
    void testGetLoggerRetrievesExistingLogger() {
        Logger logger =
                registry.computeIfAbsent("testLogger", messageFactory, (name, factory) -> LoggerContext.getContext()
                        .getLogger(name, factory));
        assertSame(logger, registry.getLogger("testLogger", messageFactory));
    }

    @Test
    void testHasLoggerReturnsCorrectStatus() {
        assertFalse(registry.hasLogger("testLogger", messageFactory));
        registry.computeIfAbsent("testLogger", messageFactory, (name, factory) -> LoggerContext.getContext()
                .getLogger(name, factory));
        assertTrue(registry.hasLogger("testLogger", messageFactory));
    }

    @Test
    void testExpungeStaleEntriesRemovesGarbageCollectedLoggers() throws InterruptedException {
        Logger logger =
                registry.computeIfAbsent("testLogger", messageFactory, (name, factory) -> LoggerContext.getContext()
                        .getLogger(name, factory));

        WeakReference<Logger> weakRef = new WeakReference<>(logger);
        logger = null; // Dereference to allow GC

        // Retry loop to give GC time to collect
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(100);
            if (weakRef.get() == null) {
                break;
            }
        }

        // Access the registry to potentially trigger cleanup
        registry.computeIfAbsent("tempLogger", messageFactory, (name, factory) -> LoggerContext.getContext()
                .getLogger(name, factory));

        assertNull(weakRef.get(), "Logger should have been garbage collected");
        assertNull(
                registry.getLogger("testLogger", messageFactory), "Stale logger should be removed from the registry");
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                registry.computeIfAbsent("testLogger", messageFactory, (name, factory) -> LoggerContext.getContext()
                        .getLogger(name, factory));
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
}
