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
package org.apache.logging.log4j;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class ScopedContextTest {

    @Test
    public void testScope() {
        ScopedContext.where("key1", "Log4j2").run(() -> assertThat(ScopedContext.get("key1"), equalTo("Log4j2")));
        ScopedContext.where("key1", "value1").run(() -> {
            assertThat(ScopedContext.get("key1"), equalTo("value1"));
            ScopedContext.where("key2", "value2").run(() -> {
                assertThat(ScopedContext.get("key1"), equalTo("value1"));
                assertThat(ScopedContext.get("key2"), equalTo("value2"));
            });
        });
    }

    @Test
    public void testRunWhere() {
        ScopedContext.runWhere("key1", "Log4j2", () -> assertThat(ScopedContext.get("key1"), equalTo("Log4j2")));
        ScopedContext.runWhere("key1", "value1", () -> {
            assertThat(ScopedContext.get("key1"), equalTo("value1"));
            ScopedContext.runWhere("key2", "value2", () -> {
                assertThat(ScopedContext.get("key1"), equalTo("value1"));
                assertThat(ScopedContext.get("key2"), equalTo("value2"));
            });
        });
    }

    @Test
    public void testRunThreads() throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        try {
            final long id = Thread.currentThread().getId();
            final AtomicLong counter = new AtomicLong(0);
            ScopedContext.runWhere("key1", "Log4j2", () -> {
                assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                Future<?> future = ScopedContext.runWhere("key2", "value2", executorService, () -> {
                    assertNotEquals(Thread.currentThread().getId(), id);
                    assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                    counter.incrementAndGet();
                });
                try {
                    future.get();
                    assertTrue(future.isDone());
                    assertEquals(1, counter.get());
                } catch (Exception ex) {
                    fail("Failed with " + ex.getMessage());
                }
            });
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testThreads() throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        try {
            final long id = Thread.currentThread().getId();
            final AtomicLong counter = new AtomicLong(0);
            ScopedContext.where("key1", "Log4j2").run(() -> {
                assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                Future<?> future = ScopedContext.where("key2", "value2").run(executorService, () -> {
                    assertNotEquals(Thread.currentThread().getId(), id);
                    assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                    counter.incrementAndGet();
                });
                try {
                    future.get();
                    assertTrue(future.isDone());
                    assertEquals(1, counter.get());
                } catch (Exception ex) {
                    fail("Failed with " + ex.getMessage());
                }
            });
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testThreadException() throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        final AtomicBoolean exceptionCaught = new AtomicBoolean(false);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        try {
            long id = Thread.currentThread().getId();
            ScopedContext.runWhere("key1", "Log4j2", () -> {
                assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                Future<?> future = ScopedContext.where("key2", "value2").run(executorService, () -> {
                    assertNotEquals(Thread.currentThread().getId(), id);
                    throw new NullPointerException("On purpose NPE");
                });
                try {
                    future.get();
                } catch (ExecutionException ex) {
                    assertThat(ex.getMessage(), equalTo("java.lang.NullPointerException: On purpose NPE"));
                    return;
                } catch (Exception ex) {
                    fail("Failed with " + ex.getMessage());
                }
                fail("No exception caught");
            });
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testThreadCall() throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        try {
            final long id = Thread.currentThread().getId();
            final AtomicInteger counter = new AtomicInteger(0);
            int returnVal = ScopedContext.callWhere("key1", "Log4j2", () -> {
                assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                Future<Integer> future = ScopedContext.callWhere("key2", "value2", executorService, () -> {
                    assertNotEquals(Thread.currentThread().getId(), id);
                    assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                    return counter.incrementAndGet();
                });
                Integer val = future.get();
                assertTrue(future.isDone());
                assertEquals(1, counter.get());
                return val;
            });
            assertThat(returnVal, equalTo(1));
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testThreadContext() throws Exception {
        ThreadContext.put("Dog", "Fido");
        ScopedContext.runWhere("key1", "Log4j2", () -> {
            assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
            assertThat(ThreadContext.get("Dog"), equalTo("Fido"));
        });

        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        try {
            final long id = Thread.currentThread().getId();
            final AtomicInteger counter = new AtomicInteger(0);
            int returnVal = ScopedContext.callWhere("key1", "Log4j2", () -> {
                assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                Future<Integer> future = ScopedContext.withThreadContext()
                        .callWhere("key2", "value2", executorService, () -> {
                            assertNotEquals(Thread.currentThread().getId(), id);
                            assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                            assertThat(ThreadContext.get("Dog"), equalTo("Fido"));
                            return counter.incrementAndGet();
                        });
                Integer val = future.get();
                assertTrue(future.isDone());
                assertEquals(1, counter.get());
                return val;
            });
            assertThat(returnVal, equalTo(1));
            ScopedContext.callWhere("key1", "Log4j2", () -> {
                assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                Future<Integer> future = ScopedContext.callWhere("key2", "value2", executorService, () -> {
                    assertNotEquals(Thread.currentThread().getId(), id);
                    assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                    assertThat(ThreadContext.get("Dog"), nullValue());
                    return counter.incrementAndGet();
                });
                Integer val = future.get();
                assertTrue(future.isDone());
                assertEquals(2, counter.get());
                return val;
            });
            assertThat(returnVal, equalTo(1));
            assertThat(ThreadContext.get("Dog"), equalTo("Fido"));
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testAsyncCall() throws Exception {
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(5);
        ExecutorService executorService = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, workQueue);
        try {
            final long id = Thread.currentThread().getId();
            final AtomicInteger counter = new AtomicInteger(0);
            ScopedContext.Instance instance =
                    ScopedContext.where("key1", "Log4j2").where("key2", "value2");
            Future<Integer> future = executorService.submit(instance.wrap(() -> {
                assertThat(ScopedContext.get("key1"), equalTo("Log4j2"));
                assertNotEquals(Thread.currentThread().getId(), id);
                assertThat(ScopedContext.get("key2"), equalTo("value2"));
                return counter.incrementAndGet();
            }));
            Integer val = future.get();
            assertTrue(future.isDone());
            assertEquals(1, counter.get());
            assertThat(val, equalTo(1));
        } finally {
            executorService.shutdown();
        }
    }
}
