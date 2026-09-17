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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompositeAction;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RollingAppenderRandomDelayTest {

    @Test
    void testRollingFileAppenderPassesMaxRandomDelayToManager(@TempDir File tempDir) {
        final Configuration configuration = new NullConfiguration();
        final int maxRandomDelay = 17;
        final RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("RollingFileAppender")
                .setFileName(new File(tempDir, "app.log").getAbsolutePath())
                .setFilePattern(new File(tempDir, "app-%i.log.gz").getAbsolutePath())
                .setPolicy(NoOpTriggeringPolicy.INSTANCE)
                .setConfiguration(configuration)
                .setMaxRandomDelay(maxRandomDelay)
                .build();
        assertNotNull(appender);
        try {
            assertEquals(maxRandomDelay, appender.getManager().getMaxRandomDelay());
        } finally {
            appender.stop();
        }
    }

    @Test
    void testRollingRandomAccessFileAppenderPassesMaxRandomDelayToManager(@TempDir File tempDir) {
        final Configuration configuration = new NullConfiguration();
        final int maxRandomDelay = 19;
        final RollingRandomAccessFileAppender appender = RollingRandomAccessFileAppender.newBuilder()
                .setName("RollingRandomAccessFileAppender")
                .setFileName(new File(tempDir, "app-ra.log").getAbsolutePath())
                .setFilePattern(new File(tempDir, "app-ra-%i.log.gz").getAbsolutePath())
                .setPolicy(NoOpTriggeringPolicy.INSTANCE)
                .setConfiguration(configuration)
                .setMaxRandomDelay(maxRandomDelay)
                .build();
        assertNotNull(appender);
        try {
            assertEquals(maxRandomDelay, appender.getManager().getMaxRandomDelay());
        } finally {
            appender.stop();
        }
    }

    @Test
    void testRollingFileManagerSchedulesAsyncActionAfterDelayAndKeepsActionsSequential(@TempDir File tempDir)
            throws Exception {
        final long delayMillis = 150;
        final CountDownLatch completed = new CountDownLatch(2);
        final AtomicInteger executionOrder = new AtomicInteger();
        final RecordingAction firstAction = new RecordingAction(executionOrder, completed);
        final RecordingAction secondAction = new RecordingAction(executionOrder, completed);
        final Action asyncAction = new CompositeAction(Arrays.asList(firstAction, secondAction), true);
        final RolloverStrategy strategy =
                manager -> new RolloverDescriptionImpl(manager.getFileName(), false, null, asyncAction);
        final File file = new File(tempDir, "scheduled.log");

        try (TestRollingFileManager manager = new TestRollingFileManager(file, strategy, delayMillis)) {
            final long rolloverStartNanos = System.nanoTime();
            manager.rollover();

            assertTrue(completed.await(3, TimeUnit.SECONDS), "Async rollover actions did not complete");
            final long actualDelayMillis = TimeUnit.NANOSECONDS.toMillis(firstAction.startNanos - rolloverStartNanos);
            assertTrue(
                    actualDelayMillis >= delayMillis - 10,
                    "Async rollover action started before the configured scheduling delay");
            assertEquals(1, firstAction.order);
            assertEquals(2, secondAction.order);
            assertTrue(secondAction.startNanos >= firstAction.endNanos, "Composite actions must execute sequentially");
        }
    }

    @Test
    void testRollingFileManagerUsesNoAsyncActionDelayByDefault(@TempDir File tempDir) {
        final RolloverStrategy strategy =
                manager -> new RolloverDescriptionImpl(manager.getFileName(), false, null, null);
        final File file = new File(tempDir, "immediate.log");

        try (TestRollingFileManager manager = new TestRollingFileManager(file, strategy)) {
            assertEquals(0, manager.getMaxRandomDelay());
            assertEquals(0, manager.getAsyncActionDelayMillis());
        }
    }

    @Test
    void testRollingFileManagerRandomAsyncActionDelayDoesNotRequireCompression(@TempDir File tempDir) {
        final int maxRandomDelay = 2;
        final long maxDelayMillis = TimeUnit.SECONDS.toMillis(maxRandomDelay);
        final RolloverStrategy strategy =
                manager -> new RolloverDescriptionImpl(manager.getFileName(), false, null, null);
        final File file = new File(tempDir, "no-compression-pattern.log");

        try (TestRollingFileManager manager = new TestRollingFileManager(file, strategy)) {
            manager.setMaxRandomDelay(maxRandomDelay);
            boolean nonZeroDelayObserved = false;
            for (int index = 0; index < 100; index++) {
                final long delayMillis = manager.getAsyncActionDelayMillis();
                assertTrue(delayMillis >= 0, "Async rollover delay must not be negative");
                assertTrue(delayMillis <= maxDelayMillis, "Async rollover delay exceeded configured maximum");
                nonZeroDelayObserved |= delayMillis > 0;
            }
            assertTrue(nonZeroDelayObserved, "Configured async rollover delay should not require compression");
        }
    }

    @Test
    void testRollingFileManagerRandomAsyncActionDelayFallsWithinConfiguredRangeForCompression(@TempDir File tempDir) {
        final int maxRandomDelay = 2;
        final long maxDelayMillis = TimeUnit.SECONDS.toMillis(maxRandomDelay);
        final RolloverStrategy strategy =
                manager -> new RolloverDescriptionImpl(manager.getFileName(), false, null, null);
        final File file = new File(tempDir, "random-compression-pattern.log");

        try (TestRollingFileManager manager =
                new TestRollingFileManager(file, file.getAbsolutePath() + ".%i.gz", strategy)) {
            manager.setMaxRandomDelay(maxRandomDelay);
            for (int index = 0; index < 100; index++) {
                final long delayMillis = manager.getAsyncActionDelayMillis();
                assertTrue(delayMillis >= 0, "Async rollover delay must not be negative");
                assertTrue(delayMillis <= maxDelayMillis, "Async rollover delay exceeded configured maximum");
            }
        }
    }

    private static final class TestRollingFileManager extends RollingFileManager {

        private final long asyncActionDelayMillis;

        private TestRollingFileManager(final File file, final RolloverStrategy strategy) {
            this(file, file.getAbsolutePath() + ".%i", strategy, -1);
        }

        private TestRollingFileManager(
                final File file, final RolloverStrategy strategy, final long asyncActionDelayMillis) {
            this(file, file.getAbsolutePath() + ".%i", strategy, asyncActionDelayMillis);
        }

        private TestRollingFileManager(final File file, final String pattern, final RolloverStrategy strategy) {
            this(file, pattern, strategy, -1);
        }

        private TestRollingFileManager(
                final File file,
                final String pattern,
                final RolloverStrategy strategy,
                final long asyncActionDelayMillis) {
            super(
                    null,
                    file.getAbsolutePath(),
                    pattern,
                    new ByteArrayOutputStream(),
                    true,
                    false,
                    0,
                    System.currentTimeMillis(),
                    NoOpTriggeringPolicy.INSTANCE,
                    strategy,
                    null,
                    PatternLayout.createDefaultLayout(),
                    null,
                    null,
                    null,
                    false,
                    ByteBuffer.allocate(256));
            this.asyncActionDelayMillis = asyncActionDelayMillis;
        }

        @Override
        long getAsyncActionDelayMillis() {
            return asyncActionDelayMillis >= 0 ? asyncActionDelayMillis : super.getAsyncActionDelayMillis();
        }
    }

    private static final class RecordingAction extends AbstractAction {

        private final AtomicInteger executionOrder;
        private final CountDownLatch completed;
        private volatile int order;
        private volatile long startNanos;
        private volatile long endNanos;

        private RecordingAction(final AtomicInteger executionOrder, final CountDownLatch completed) {
            this.executionOrder = executionOrder;
            this.completed = completed;
        }

        @Override
        public boolean execute() throws IOException {
            startNanos = System.nanoTime();
            order = executionOrder.incrementAndGet();
            endNanos = System.nanoTime();
            completed.countDown();
            return true;
        }
    }
}
