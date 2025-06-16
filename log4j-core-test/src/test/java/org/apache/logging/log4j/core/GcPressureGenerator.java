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
package org.apache.logging.log4j.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates GC pressure by continuously allocating and {@link System#gc() triggering GC}.
 */
final class GcPressureGenerator implements AutoCloseable {

    private final AtomicInteger sink = new AtomicInteger(0);

    private final AtomicBoolean running = new AtomicBoolean(true);

    private final CountDownLatch stopLatch = new CountDownLatch(1);

    private GcPressureGenerator() {
        startGeneratorThread();
    }

    private void startGeneratorThread() {
        final String threadName = GcPressureGenerator.class.getSimpleName();
        final Thread thread = new Thread(this::generateGarbage, threadName);
        thread.setDaemon(true); // Avoid blocking JVM exit
        thread.start();
    }

    private void generateGarbage() {
        try {
            while (running.get()) {
                final Object object = new byte[1024 * 1024];
                int positiveValue = Math.abs(object.hashCode());
                sink.set(positiveValue);
                System.gc();
                System.runFinalization();
            }
        } finally {
            stopLatch.countDown();
        }
    }

    static GcPressureGenerator ofStarted() {
        return new GcPressureGenerator();
    }

    @Override
    public void close() {
        final boolean signalled = running.compareAndSet(true, false);
        if (signalled) {
            try {
                final boolean stopped = stopLatch.await(10, TimeUnit.SECONDS);
                assertThat(stopped).isTrue();
                assertThat(sink.get()).isPositive();
            } catch (final InterruptedException error) {
                // Restore the `interrupted` flag
                Thread.currentThread().interrupt();
                throw new RuntimeException(error);
            }
        }
    }
}
