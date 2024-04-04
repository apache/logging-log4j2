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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.spi.ScopedContextProvider;

/**
 * Provides test that should be passed by all implementations of {@link ScopedContextProviderSuite}.
 * @since 2.24.0
 */
public abstract class ScopedContextProviderSuite {

    private static ScopedContext.Instance where(
            final ScopedContextProvider provider, final String key, final Object value) {
        return provider.newScopedContext(key, value);
    }

    protected static void testScope(final ScopedContextProvider scopedContext) {
        where(scopedContext, "key1", "Log4j2")
                .run(() -> assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2"));
        where(scopedContext, "key1", "value1").run(() -> {
            assertThat(scopedContext.getValue("key1")).isEqualTo("value1");
            where(scopedContext, "key2", "value2").run(() -> {
                assertThat(scopedContext.getValue("key1")).isEqualTo("value1");
                assertThat(scopedContext.getValue("key2")).isEqualTo("value2");
            });
        });
    }

    private static void runWhere(
            final ScopedContextProvider provider, final String key, final Object value, final Runnable task) {
        provider.newScopedContext(key, value).run(task);
    }

    private static Future<Void> runWhere(
            final ScopedContextProvider provider,
            final String key,
            final Object value,
            final ExecutorService executorService,
            final Runnable task) {
        return provider.newScopedContext(key, value).run(executorService, task);
    }

    protected static void testRunWhere(final ScopedContextProvider scopedContext) {
        runWhere(scopedContext, "key1", "Log4j2", () -> assertThat(scopedContext.getValue("key1"))
                .isEqualTo("Log4j2"));
        runWhere(scopedContext, "key1", "value1", () -> {
            assertThat(scopedContext.getValue("key1")).isEqualTo("value1");
            runWhere(scopedContext, "key2", "value2", () -> {
                assertThat(scopedContext.getValue("key1")).isEqualTo("value1");
                assertThat(scopedContext.getValue("key2")).isEqualTo("value2");
            });
        });
    }

    protected static void testRunThreads(final ScopedContextProvider scopedContext) {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        final long id = Thread.currentThread().getId();
        final AtomicLong counter = new AtomicLong(0);
        runWhere(scopedContext, "key1", "Log4j2", () -> {
            assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2");
            Future<?> future = runWhere(scopedContext, "key2", "value2", executorService, () -> {
                assertNotEquals(Thread.currentThread().getId(), id);
                assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2");
                counter.incrementAndGet();
            });
            assertDoesNotThrow(() -> {
                future.get();
                assertTrue(future.isDone());
                assertThat(counter.get()).isEqualTo(1);
            });
        });
    }

    protected static void testThreads(final ScopedContextProvider scopedContext) throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        final long id = Thread.currentThread().getId();
        final AtomicLong counter = new AtomicLong(0);
        where(scopedContext, "key1", "Log4j2").run(() -> {
            assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2");
            Future<?> future = where(scopedContext, "key2", "value2").run(executorService, () -> {
                assertNotEquals(Thread.currentThread().getId(), id);
                assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2");
                counter.incrementAndGet();
            });
            assertDoesNotThrow(() -> {
                future.get();
                assertTrue(future.isDone());
                assertThat(counter.get()).isEqualTo(1);
            });
        });
    }

    protected static void testThreadException(final ScopedContextProvider scopedContext) throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        final AtomicBoolean exceptionCaught = new AtomicBoolean(false);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        long id = Thread.currentThread().getId();
        runWhere(scopedContext, "key1", "Log4j2", () -> {
            assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2");
            Future<?> future = where(scopedContext, "key2", "value2").run(executorService, () -> {
                assertNotEquals(Thread.currentThread().getId(), id);
                throw new NullPointerException("On purpose NPE");
            });
            assertThatThrownBy(future::get)
                    .hasRootCauseInstanceOf(NullPointerException.class)
                    .hasRootCauseMessage("On purpose NPE");
        });
    }

    private static <R> R callWhere(
            final ScopedContextProvider provider, final String key, final Object value, final Callable<R> task)
            throws Exception {
        return provider.newScopedContext(key, value).call(task);
    }

    private static <R> Future<R> callWhere(
            final ScopedContextProvider provider,
            final String key,
            final Object value,
            final ExecutorService executorService,
            final Callable<R> task) {
        return provider.newScopedContext(key, value).call(executorService, task);
    }

    protected static void testThreadCall(final ScopedContextProvider scopedContext) throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        final long id = Thread.currentThread().getId();
        final AtomicInteger counter = new AtomicInteger(0);
        int returnVal = callWhere(scopedContext, "key1", "Log4j2", () -> {
            assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2");
            Future<Integer> future = callWhere(scopedContext, "key2", "value2", executorService, () -> {
                assertNotEquals(Thread.currentThread().getId(), id);
                assertThat(scopedContext.getValue("key1")).isEqualTo("Log4j2");
                return counter.incrementAndGet();
            });
            Integer val = future.get();
            assertTrue(future.isDone());
            assertThat(counter.get()).isEqualTo(1);
            return val;
        });
        assertThat(returnVal).isEqualTo(1);
    }
}
