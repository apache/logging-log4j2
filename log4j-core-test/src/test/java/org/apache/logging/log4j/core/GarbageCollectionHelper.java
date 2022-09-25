/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core;

import com.google.common.io.ByteStreams;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public final class GarbageCollectionHelper implements Closeable, Runnable {
    private static final OutputStream sink = ByteStreams.nullOutputStream();
    private final AtomicBoolean running = new AtomicBoolean();
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Thread gcThread = new Thread(() -> {
        try {
            while (running.get()) {
                // Allocate data to help suggest a GC
                try {
                    // 1mb of heap
                    sink.write(new byte[1024 * 1024]);
                } catch (final IOException ignored) {
                }
                // May no-op depending on the JVM configuration
                System.gc();
            }
        } finally {
            latch.countDown();
        }
    });

    @Override
    public void run() {
        if (running.compareAndSet(false, true)) {
            gcThread.start();
        }
    }

    @Override
    public void close() {
        running.set(false);
        try {
            assertTrue("GarbageCollectionHelper did not shut down cleanly",
                    latch.await(10, TimeUnit.SECONDS));
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
