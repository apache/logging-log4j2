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
package org.apache.log4j;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.junit.jupiter.api.Test;

class CategoryConcurrencyTest {

    @Test
    void removeAllAppendersSynchronizesOnCategory() throws Exception {
        final Category category = new TestCategory("org.example.concurrent.lock");
        final ListAppender appender = new ListAppender();
        appender.setName("lockAppender");
        final AppenderAttachableImpl aai = new AppenderAttachableImpl();
        aai.addAppender(appender);
        category.aai = aai;

        final CountDownLatch enteredMonitor = new CountDownLatch(1);
        final CountDownLatch removerFinished = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        final Thread remover = new Thread(() -> {
            try {
                assertTrue(enteredMonitor.await(5, TimeUnit.SECONDS));
                category.removeAllAppenders();
            } catch (final Throwable t) {
                failure.set(t);
            } finally {
                removerFinished.countDown();
            }
        });
        remover.start();

        final boolean aaiNulledWhileLocked;
        synchronized (category) {
            enteredMonitor.countDown();
            assertFalse(removerFinished.await(100, TimeUnit.MILLISECONDS));
            aaiNulledWhileLocked = category.aai == null;
        }

        assertTrue(removerFinished.await(5, TimeUnit.SECONDS));
        remover.join(5000);
        assertNull(failure.get(), () -> "Unexpected failure: " + failure.get());
        assertFalse(aaiNulledWhileLocked, "aai was nulled while holding Category monitor");
        assertNull(category.aai);
    }

    @Test
    void concurrentRemoveAllAppendersDoesNotThrowNpe() throws Exception {
        final Category category = new TestCategory("org.example.concurrent.stress");
        final ListAppender appender = new ListAppender();
        appender.setName("stressAppender");

        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final int iterations = 20_000;

        final Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < iterations && failure.get() == null; i++) {
                    synchronized (category) {
                        if (category.aai == null) {
                            category.aai = new AppenderAttachableImpl();
                        }
                        category.aai.addAppender(appender);
                    }
                    category.removeAllAppenders();
                }
            } catch (final Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                running.set(false);
            }
        });

        final Thread reader = new Thread(() -> {
            try {
                while (running.get() && failure.get() == null) {
                    category.isAttached(appender);
                    category.removeAppender(appender);
                    category.removeAppender("stressAppender");
                    synchronized (category) {
                        if (category.aai != null) {
                            category.aai.getAllAppenders();
                        }
                    }
                }
            } catch (final Throwable t) {
                failure.compareAndSet(null, t);
            }
        });

        writer.start();
        reader.start();
        writer.join(10_000);
        running.set(false);
        reader.join(10_000);

        assertNull(failure.get(), () -> "Concurrent appender access threw: " + failure.get());
    }

    private static final class TestCategory extends Logger {
        TestCategory(final String name) {
            super(name);
        }
    }
}
