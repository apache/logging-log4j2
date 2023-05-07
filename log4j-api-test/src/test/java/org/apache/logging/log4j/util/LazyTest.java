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
package org.apache.logging.log4j.util;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;

// tests adapted from Kotlin
@Timeout(30)
class LazyTest {
    final List<Throwable> asyncErrors = Collections.synchronizedList(new ArrayList<>());
    final ThreadGroup threadGroup = new ThreadGroup("LazyTest");

    @AfterEach
    void tearDown() {
        threadGroup.interrupt();
        assertThat(asyncErrors).isEmpty();
    }

    @Test
    void strictLazy() {
        AtomicInteger counter = new AtomicInteger();
        Lazy<Integer> lazy = Lazy.lazy(() -> {
            int value = counter.incrementAndGet();
            // simulate some computation
            runCatching(() -> Thread.sleep(16));
            return value;
        });
        int threads = 3;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Thread> runners = IntStream.range(0, threads)
                .mapToObj(i -> {
                    Thread thread = new Thread(threadGroup, () -> runCatching(() -> {
                        barrier.await();
                        lazy.get();
                    }));
                    thread.start();
                    return thread;
                })
                .collect(Collectors.toList());
        runners.forEach(thread -> runCatching(thread::join));
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void relaxedLazy() {
        AtomicInteger counter = new AtomicInteger();
        int nrThreads = 3;
        int[] values = ThreadLocalRandom.current()
                .ints(nrThreads)
                .map(i -> 100 + i % 50)
                .toArray();
        int[] runs = new int[nrThreads];
        Lazy<Integer> lazy = Lazy.relaxed(() -> {
            int id = counter.getAndIncrement();
            int value = values[id];
            runs[id] = value;
            runCatching(() -> Thread.sleep(value));
            return value;
        });
        CyclicBarrier barrier = new CyclicBarrier(nrThreads);
        List<Thread> threads = IntStream.range(0, nrThreads)
                .mapToObj(i -> {
                    Thread thread = new Thread(threadGroup, () -> runCatching(() -> {
                        barrier.await();
                        lazy.get();
                    }));
                    thread.start();
                    return thread;
                })
                .collect(Collectors.toList());
        while (!lazy.isInitialized()) {
            runCatching(() -> Thread.sleep(1));
        }
        int result = lazy.get();
        threads.forEach(thread -> runCatching(thread::join));
        assertThat(counter.get()).isEqualTo(nrThreads);
        assertThat(lazy.get()).isEqualTo(result);
        assertThat(runs).contains(result);
    }

    @Test
    void strictLazyRace() {
        racyTest(3, 5000,
                () -> {
                    AtomicInteger counter = new AtomicInteger();
                    return Lazy.lazy(counter::incrementAndGet);
                },
                (lazy, ignored) -> lazy.value(),
                result -> result.stream().allMatch(i -> i == 1));
    }

    @Test
    void relaxedLazyRace() {
        racyTest(3, 5000,
                () -> Lazy.relaxed(() -> Thread.currentThread().getId()),
                (lazy, ignored) -> lazy.value(),
                result -> result.stream().allMatch(v -> Objects.equals(v, result.get(0))));
    }

    void runCatching(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            asyncErrors.add(e);
        }
    }

    <S, T> void racyTest(int nrThreads, int runs, Supplier<S> stateInitializer,
                         BiFunction<S, Integer, T> run, Predicate<List<T>> resultsValidator) {
        List<T> runResult = Collections.synchronizedList(new ArrayList<>());
        List<Map.Entry<Integer, List<T>>> invalidResults = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger currentRunId = new AtomicInteger(0);
        AtomicReference<S> state = new AtomicReference<>();
        CyclicBarrier barrier = new CyclicBarrier(nrThreads, () -> {
            int runId = currentRunId.getAndIncrement();
            if (runId > 0) {
                if (!resultsValidator.test(runResult)) {
                    invalidResults.add(Map.entry(runId, List.copyOf(runResult)));
                }
                runResult.clear();
            }
            state.set(stateInitializer.get());
        });
        List<Thread> runners = IntStream.range(0, nrThreads)
                .mapToObj(i -> {
                    Thread thread = new Thread(threadGroup, () -> runCatching(() -> {
                        barrier.await();
                        for (int j = 0; j < runs; j++) {
                            runResult.add(run.apply(state.get(), j));
                            barrier.await();
                        }
                    }));
                    thread.start();
                    return thread;
                })
                .collect(Collectors.toList());
        runners.forEach(thread -> runCatching(thread::join));
        assertThat(invalidResults).isEmpty();
    }
}
