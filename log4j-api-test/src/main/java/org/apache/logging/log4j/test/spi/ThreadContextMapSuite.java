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
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Provides test cases to apply to all implementations of {@link ThreadContextMap}.
 * @since 2.24.0
 */
@Execution(ExecutionMode.CONCURRENT)
public abstract class ThreadContextMapSuite {

    private static final String KEY = "key";

    /**
     * Checks if the context map does not propagate to other threads by default.
     */
    protected static void threadLocalNotInheritableByDefault(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "threadLocalNotInheritableByDefault");
        verifyThreadContextValueFromANewThread(contextMap, null);
    }

    /**
     * Checks if the context map can be configured to propagate to other threads.
     */
    protected static void threadLocalInheritableIfConfigured(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "threadLocalInheritableIfConfigured");
        verifyThreadContextValueFromANewThread(contextMap, "threadLocalInheritableIfConfigured");
    }

    /**
     * Checks basic put/remove pattern.
     */
    protected static void singleValue(final ThreadContextMap contextMap) {
        assertThat(contextMap.isEmpty()).as("Map is empty").isTrue();
        contextMap.put(KEY, "testPut");
        assertThat(contextMap.isEmpty()).as("Map is not empty").isFalse();
        assertThat(contextMap.containsKey(KEY)).as("Map key exists").isTrue();
        assertThat(contextMap.get(KEY)).as("Map contains expected value").isEqualTo("testPut");
        contextMap.remove(KEY);
        assertThat(contextMap.isEmpty()).as("Map is empty").isTrue();
    }

    /**
     * Checks mutable copy
     */
    protected static void getCopyReturnsMutableCopy(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "testGetCopyReturnsMutableCopy");

        final Map<String, String> copy = contextMap.getCopy();
        assertThat(copy).as("Copy contains same value").containsExactly(entry(KEY, "testGetCopyReturnsMutableCopy"));

        copy.put(KEY, "testGetCopyReturnsMutableCopy2");
        assertThat(contextMap.get(KEY))
                .as("Original map is not affected by changes in the copy")
                .isEqualTo("testGetCopyReturnsMutableCopy");

        contextMap.clear();
        assertThat(contextMap.isEmpty()).as("Original map is empty").isTrue();
        assertThat(copy)
                .as("Copy is not affected by changes in the map.")
                .containsExactly(entry(KEY, "testGetCopyReturnsMutableCopy2"));
    }

    /**
     * The immutable copy must be {@code null} if the map is empty.
     */
    protected static void getImmutableMapReturnsNullIfEmpty(final ThreadContextMap contextMap) {
        assertThat(contextMap.isEmpty()).as("Original map is empty").isTrue();
        assertThat(contextMap.getImmutableMapOrNull())
                .as("Immutable copy is null")
                .isNull();
    }

    /**
     * The result of {@link ThreadContextMap#getImmutableMapOrNull()} must be immutable.
     */
    protected static void getImmutableMapReturnsImmutableMapIfNonEmpty(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "getImmutableMapReturnsImmutableMapIfNonEmpty");

        final Map<String, String> immutable = contextMap.getImmutableMapOrNull();
        assertThat(immutable)
                .as("Immutable copy contains same value")
                .containsExactly(entry(KEY, "getImmutableMapReturnsImmutableMapIfNonEmpty"));

        assertThrows(
                UnsupportedOperationException.class, () -> immutable.put(KEY, "getImmutableMapReturnsNullIfEmpty2"));
    }

    /**
     * The immutable copy is not affected by changes to the original map.
     */
    protected static void getImmutableMapCopyNotAffectedByContextMapChanges(final ThreadContextMap contextMap) {
        contextMap.put(KEY, "getImmutableMapCopyNotAffectedByContextMapChanges");

        final Map<String, String> immutable = contextMap.getImmutableMapOrNull();
        contextMap.put(KEY, "getImmutableMapCopyNotAffectedByContextMapChanges2");
        assertThat(immutable)
                .as("Immutable copy contains the original value")
                .containsExactly(entry(KEY, "getImmutableMapCopyNotAffectedByContextMapChanges"));
    }

    private static void verifyThreadContextValueFromANewThread(
            final ThreadContextMap contextMap, final String expected) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            assertThat(executorService.submit(() -> contextMap.get(KEY)))
                    .succeedsWithin(Duration.ofSeconds(1))
                    .isEqualTo(expected);
        } finally {
            executorService.shutdown();
        }
    }
}
