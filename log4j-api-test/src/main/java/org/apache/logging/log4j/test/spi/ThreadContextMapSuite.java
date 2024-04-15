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
package org.apache.logging.log4j.test.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.spi.ThreadContextMap;

/**
 * A simple suite of tests for {@link ThreadContextMap} implementations.
 */
public abstract class ThreadContextMapSuite {
    private static final String KEY = "key";

    /**
     * Checks if new threads either inherit or do not inherit the context data of the current thread.
     *
     * @param threadContext A {@link ThreadContextMap implementation}.
     * @param key           The key to use.
     * @param expectedValue The expected value on a new thread.
     */
    protected static void assertThreadContextValueOnANewThread(
            final ThreadContextMap threadContext, final String key, final String expectedValue) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            assertThat(executorService.submit(() -> threadContext.get(key)))
                    .succeedsWithin(Duration.ofSeconds(1))
                    .isEqualTo(expectedValue);
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Ensures that {@code save/restore} works correctly on the current thread.
     */
    protected static void assertContextDataCanBeSavedAndRestored(final ThreadContextMap threadContext) {
        final String externalValue = "externalValue";
        final String internalValue = "internalValue";
        final RuntimeException throwable = new RuntimeException();
        threadContext.put(KEY, externalValue);
        final Object saved = threadContext.save();
        try {
            threadContext.put(KEY, internalValue);
            assertThat(threadContext.get(KEY)).isEqualTo(internalValue);
            throw throwable;
        } catch (final RuntimeException e) {
            assertThat(e).isSameAs(throwable);
            assertThat(threadContext.get(KEY)).isEqualTo(internalValue);
        } finally {
            threadContext.restore(saved);
        }
        assertThat(threadContext.get(KEY)).isEqualTo(externalValue);
    }

    /**
     * Ensures that the context data obtained through {@link ThreadContextMap#save} can be safely transferred to another
     * thread.
     *
     * @param threadContext The {@link ThreadContextMap} to test.
     */
    protected static void assertContextDataCanBeTransferred(final ThreadContextMap threadContext) {
        final String mainThreadValue = "mainThreadValue";
        final String newThreadValue = "newThreadValue";
        final RuntimeException throwable = new RuntimeException();
        threadContext.put(KEY, mainThreadValue);
        final Object mainThreadSaved = threadContext.save();
        threadContext.remove(KEY);
        // Move to new thread
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            assertThat(executorService.submit(() -> {
                        threadContext.put(KEY, newThreadValue);
                        final Object newThreadSaved = threadContext.restore(mainThreadSaved);
                        try {
                            assertThat(threadContext.get(KEY)).isEqualTo(mainThreadValue);
                            throw throwable;
                        } catch (final RuntimeException e) {
                            assertThat(e).isSameAs(throwable);
                            assertThat(threadContext.get(KEY)).isEqualTo(mainThreadValue);
                        } finally {
                            threadContext.restore(newThreadSaved);
                        }
                        assertThat(threadContext.get(KEY)).isEqualTo(newThreadValue);
                    }))
                    .succeedsWithin(Duration.ofSeconds(1));
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Ensures that the saved value is always not {@code null}, even if there are no context data.
     * <p>
     *     This requirement allows third-party libraries to store the saved value as value of a map, even if the map
     *     does not allow nulls.
     * </p>
     */
    protected static void assertSavedValueNotNullIfMapEmpty(final ThreadContextMap threadContext) {
        threadContext.clear();
        final Object saved = threadContext.save();
        assertThat(saved).as("Saved empty context data.").isNotNull();
    }

    protected static void assertRestoreDoesNotAcceptNull(final ThreadContextMap threadContext) {
        assertThrows(NullPointerException.class, () -> threadContext.restore(null));
    }
}
