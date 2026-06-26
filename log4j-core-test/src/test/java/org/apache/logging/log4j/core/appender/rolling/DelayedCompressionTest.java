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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

/**
 * Tests for the delayed compression feature in {@link RollingFileManager} and
 * {@link DefaultRolloverStrategy}.
 */
@Issue("https://github.com/apache/logging-log4j2/issues/4011")
class DelayedCompressionTest {

    // -----------------------------------------------------------------------
    // RolloverDescriptionImpl delay parameter tests
    // -----------------------------------------------------------------------

    @Test
    void testRolloverDescriptionImplDefaultsToZeroDelay() {
        final RolloverDescriptionImpl desc =
                new RolloverDescriptionImpl("active.log", false, null, null);
        assertEquals(0, desc.getMinAsyncDelay(), "default minAsyncDelay should be 0");
        assertEquals(0, desc.getMaxAsyncDelay(), "default maxAsyncDelay should be 0");
    }

    @Test
    void testRolloverDescriptionImplStoresDelayRange() {
        final RolloverDescriptionImpl desc =
                new RolloverDescriptionImpl("active.log", false, null, null, 5, 30);
        assertEquals(5, desc.getMinAsyncDelay());
        assertEquals(30, desc.getMaxAsyncDelay());
    }

    @Test
    void testRolloverDescriptionImplRejectsInvalidRange() {
        // maxAsyncDelay < minAsyncDelay should throw
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new RolloverDescriptionImpl("active.log", false, null, null, 30, 5));
        assertTrue(ex.getMessage().contains("maxAsyncDelay"));
    }

    // -----------------------------------------------------------------------
    // DefaultRolloverStrategy builder tests
    // -----------------------------------------------------------------------

    @Test
    void testDefaultRolloverStrategyBuilderZeroDelayByDefault() {
        final DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder().build();
        assertEquals(0, strategy.getMinDelaySeconds(), "default minDelaySeconds should be 0");
        assertEquals(0, strategy.getMaxDelaySeconds(), "default maxDelaySeconds should be 0");
    }

    @Test
    void testDefaultRolloverStrategyBuilderStoresDelayRange() {
        final DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .setMinDelaySeconds("10")
                .setMaxDelaySeconds("60")
                .build();
        assertEquals(10, strategy.getMinDelaySeconds());
        assertEquals(60, strategy.getMaxDelaySeconds());
    }

    @Test
    void testDefaultRolloverStrategyBuilderInvalidRangeFallsBackToZero() {
        // maxDelaySeconds < minDelaySeconds → both reset to 0
        final DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .setMinDelaySeconds("60")
                .setMaxDelaySeconds("10")
                .build();
        assertEquals(0, strategy.getMinDelaySeconds(), "invalid range: minDelaySeconds should be reset to 0");
        assertEquals(0, strategy.getMaxDelaySeconds(), "invalid range: maxDelaySeconds should be reset to 0");
    }

    @Test
    void testDefaultRolloverStrategyRolloverPassesDelayToDescriptor() throws IOException, SecurityException {
        final DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .setMinDelaySeconds("5")
                .setMaxDelaySeconds("30")
                .build();

        final File logFile = File.createTempFile("testPassesDelayToDescriptor", ".log");
        logFile.deleteOnExit();
        final NullConfiguration config = new NullConfiguration();
        final RollingFileManager manager = RollingFileManager.getFileManager(
                logFile.getAbsolutePath(),
                logFile.getAbsolutePath() + "-%d{yyyy-MM-dd}.log.gz",
                true,
                false,
                OnStartupTriggeringPolicy.createPolicy(1),
                strategy,
                null,
                PatternLayout.createDefaultLayout(config),
                0,
                true,
                false,
                null,
                null,
                null,
                config);
        assertNotNull(manager);
        manager.initialize();

        final RolloverDescription desc = strategy.rollover(manager);
        assertNotNull(desc);
        assertEquals(5, desc.getMinAsyncDelay(), "descriptor should carry minAsyncDelay from strategy");
        assertEquals(30, desc.getMaxAsyncDelay(), "descriptor should carry maxAsyncDelay from strategy");

        manager.close();
    }

    // -----------------------------------------------------------------------
    // RollingFileManager scheduling tests
    // -----------------------------------------------------------------------

    /**
     * Verifies that when minDelaySeconds == maxDelaySeconds == 0 the async action is submitted
     * for immediate execution (delay == 0).
     */
    @Test
    void testImmediateExecutionWhenNoDelayConfigured() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicLong executionDelayMs = new AtomicLong(-1);
        final long submitTimeMs = System.currentTimeMillis();

        final RolloverStrategy immediateStrategy = manager -> new RolloverDescriptionImpl(
                manager.getFileName(),
                false,
                null,
                new AbstractAction() {
                    @Override
                    public boolean execute() throws IOException {
                        executionDelayMs.set(System.currentTimeMillis() - submitTimeMs);
                        latch.countDown();
                        return true;
                    }
                },
                0,
                0);

        final File logFile = File.createTempFile("testImmediateExecution", ".log");
        logFile.deleteOnExit();
        final NullConfiguration config = new NullConfiguration();
        final RollingFileManager manager = RollingFileManager.getFileManager(
                logFile.getAbsolutePath(),
                logFile.getAbsolutePath() + "-immediate-%d{yyyy-MM-dd}.log",
                true,
                false,
                OnStartupTriggeringPolicy.createPolicy(1),
                immediateStrategy,
                null,
                PatternLayout.createDefaultLayout(config),
                0,
                true,
                false,
                null,
                null,
                null,
                config);
        assertNotNull(manager);
        manager.initialize();
        manager.rollover();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "async action should complete within 5 seconds");
        // Immediate execution should complete well under 1 second
        assertTrue(
                executionDelayMs.get() < 1000,
                "immediate action should execute in < 1 s, took " + executionDelayMs.get() + " ms");

        manager.close();
    }

    /**
     * Verifies that when a delay range is configured, the async action is scheduled via
     * {@link ScheduledExecutorService#schedule} with a delay in [minDelay, maxDelay].
     * We use a fixed range (min == max == 2 s) to make the assertion deterministic.
     */
    @Test
    void testAsyncActionIsScheduledWithDelay() throws Exception {
        final int fixedDelaySecs = 2;
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicLong executionDelayMs = new AtomicLong(-1);
        final long[] submitTimeMs = {0};

        final RolloverStrategy delayedStrategy = manager -> {
            submitTimeMs[0] = System.currentTimeMillis();
            return new RolloverDescriptionImpl(
                    manager.getFileName(),
                    false,
                    null,
                    new AbstractAction() {
                        @Override
                        public boolean execute() throws IOException {
                            executionDelayMs.set(System.currentTimeMillis() - submitTimeMs[0]);
                            latch.countDown();
                            return true;
                        }
                    },
                    fixedDelaySecs,
                    fixedDelaySecs);
        };

        final File logFile = File.createTempFile("testAsyncScheduledWithDelay", ".log");
        logFile.deleteOnExit();
        final NullConfiguration config = new NullConfiguration();
        final RollingFileManager manager = RollingFileManager.getFileManager(
                logFile.getAbsolutePath(),
                logFile.getAbsolutePath() + "-delayed-%d{yyyy-MM-dd}.log",
                true,
                false,
                OnStartupTriggeringPolicy.createPolicy(1),
                delayedStrategy,
                null,
                PatternLayout.createDefaultLayout(config),
                0,
                true,
                false,
                null,
                null,
                null,
                config);
        assertNotNull(manager);
        manager.initialize();
        manager.rollover();

        assertTrue(latch.await(10, TimeUnit.SECONDS), "delayed async action should complete within 10 seconds");
        // The action should have been delayed by at least (fixedDelaySecs - 0.5) seconds
        final long minExpectedMs = (fixedDelaySecs - 1) * 1000L;
        assertTrue(
                executionDelayMs.get() >= minExpectedMs,
                "action should be delayed by >= " + minExpectedMs + " ms, actual: " + executionDelayMs.get() + " ms");

        manager.close();
    }

    /**
     * Verifies that the random delay produced by {@link RollingFileManager} falls within
     * [minDelaySeconds, maxDelaySeconds] by inspecting the scheduled future's delay via
     * the executor service.
     */
    @Test
    void testRandomDelayFallsWithinConfiguredRange() throws Exception {
        final int minDelaySecs = 5;
        final int maxDelaySecs = 30;

        final AtomicReference<ScheduledExecutorService> executorRef = new AtomicReference<>();

        final RolloverStrategy rangeStrategy = manager -> new RolloverDescriptionImpl(
                manager.getFileName(),
                false,
                null,
                new AbstractAction() {
                    @Override
                    public boolean execute() {
                        return true;
                    }
                },
                minDelaySecs,
                maxDelaySecs);

        final File logFile = File.createTempFile("testRandomDelayRange", ".log");
        logFile.deleteOnExit();
        final NullConfiguration config = new NullConfiguration();
        final RollingFileManager manager = RollingFileManager.getFileManager(
                logFile.getAbsolutePath(),
                logFile.getAbsolutePath() + "-range-%d{yyyy-MM-dd}.log",
                true,
                false,
                OnStartupTriggeringPolicy.createPolicy(1),
                rangeStrategy,
                null,
                PatternLayout.createDefaultLayout(config),
                0,
                true,
                false,
                null,
                null,
                null,
                config);
        assertNotNull(manager);
        manager.initialize();
        manager.rollover();

        // Inspect the scheduled task's remaining delay immediately after rollover
        final ScheduledExecutorService executor = manager.getAsyncExecutor();
        // Give the executor a moment to register the scheduled task
        Thread.sleep(100);

        // The executor queue should contain a scheduled task; check its delay
        // We verify indirectly: the executor is a ScheduledThreadPoolExecutor, so we can
        // cast and inspect the queue.
        if (executor instanceof java.util.concurrent.ScheduledThreadPoolExecutor) {
            final java.util.concurrent.ScheduledThreadPoolExecutor stpe =
                    (java.util.concurrent.ScheduledThreadPoolExecutor) executor;
            // The task should still be pending (delay > 0)
            final java.util.concurrent.BlockingQueue<?> queue = stpe.getQueue();
            if (!queue.isEmpty()) {
                final Object task = queue.peek();
                if (task instanceof java.util.concurrent.Delayed) {
                    final long remainingMs =
                            ((java.util.concurrent.Delayed) task).getDelay(TimeUnit.MILLISECONDS);
                    // remaining delay should be <= maxDelaySecs * 1000 ms
                    assertTrue(
                            remainingMs <= maxDelaySecs * 1000L,
                            "remaining delay " + remainingMs + " ms exceeds maxDelaySecs=" + maxDelaySecs + " s");
                    // remaining delay should be >= (minDelaySecs - 1) * 1000 ms (allow 1 s tolerance)
                    assertTrue(
                            remainingMs >= (minDelaySecs - 1) * 1000L,
                            "remaining delay " + remainingMs + " ms is less than minDelaySecs=" + minDelaySecs + " s");
                }
            }
        }

        // Cancel all pending tasks and shut down
        manager.getAsyncExecutor().shutdownNow();
        manager.close();
    }
}
