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
package org.apache.logging.log4j.core.config;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Performs reconfiguration whilst logging.
 *
 * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-620">LOG4J2-620</a>
 * @see TestAppender
 */
@LoggerContextSource("reconfiguration-deadlock.xml")
public class ReconfigurationDeadlockTest {

    private static final int WORKER_COUNT = 100;

    private ExecutorService executor;

    @BeforeEach
    public void startExecutor() {
        executor = Executors.newFixedThreadPool(WORKER_COUNT);
    }

    @AfterEach
    public void stopExecutor() throws InterruptedException {
        executor.shutdownNow();
        final boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);
        Assertions.assertTrue(terminated, "couldn't terminate the executor");
    }

    @RepeatedTest(100)
    public void reconfiguration_should_not_cause_deadlock_for_ongoing_logging() throws Exception {

        // Try to update the config file to ensure that we can indeed update it.
        updateConfigFileModTime();

        // Start the workers.
        final CountDownLatch workerStartLatch = new CountDownLatch(WORKER_COUNT);
        final List<Future<?>> workerFutures = initiateWorkers(workerStartLatch, executor);

        // Await workers to start and update the config file.
        workerStartLatch.await(10, TimeUnit.SECONDS);
        updateConfigFileModTime();

        // Verify that all workers have finished okay.
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            final Future<?> workerFuture = workerFutures.get(workerIndex);
            try {
                final Object workerResult = workerFuture.get(30, TimeUnit.SECONDS);
                Assertions.assertNull(workerResult);
            } catch (final Throwable failure) {
                final String message =
                        String.format("check for worker %02d/%02d has failed", (workerIndex + 1), WORKER_COUNT);
                throw new AssertionError(message, failure);
            }
        }
    }

    private static void updateConfigFileModTime() {
        final File file = new File("target/test-classes/reconfiguration-deadlock.xml");
        final boolean fileModified = file.setLastModified(System.currentTimeMillis());
        Assertions.assertTrue(fileModified, "couldn't update file modification time");
    }

    @SuppressWarnings("SameParameterValue")
    private static List<Future<?>> initiateWorkers(
            final CountDownLatch workerStartLatch, final ExecutorService executor) {
        final Logger logger = LogManager.getRootLogger();
        return IntStream.range(0, WORKER_COUNT)
                .mapToObj((final int workerIndex) -> executor.submit(() -> {
                    int i = 0;
                    for (; i < 1_000; i++) {
                        logger.error("worker={}, iteration={}", workerIndex, i);
                    }
                    workerStartLatch.countDown();
                    for (; i < 5_000; i++) {
                        logger.error("worker={}, iteration={}", workerIndex, i);
                    }
                }))
                .collect(Collectors.toList());
    }

    /**
     * A dummy appender doing nothing but burning CPU cycles whilst randomly accessing the logger.
     */
    @Plugin(
            name = "ReconfigurationDeadlockTestAppender",
            category = Core.CATEGORY_NAME,
            elementType = Appender.ELEMENT_TYPE,
            printObject = true)
    public static final class TestAppender extends AbstractAppender {

        private final Logger logger;

        private TestAppender(
                final String name, final Filter filter, final Layout<?> layout, final boolean ignoreExceptions) {
            super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
            this.logger = LogManager.getRootLogger();
        }

        @PluginFactory
        public static TestAppender createAppender(
                @PluginAttribute("name") @Required(message = "A name for the Appender must be specified")
                        final String name,
                @PluginAttribute("ignoreExceptions") final boolean ignore,
                @PluginElement("Layout") final Layout<?> layout,
                @PluginElement("Filter") final Filter filter) {
            return new TestAppender(name, filter, layout, ignore);
        }

        /**
         * Does nothing but burning CPU cycles and accessing to the logger.
         */
        @Override
        public void append(final LogEvent event) {
            boolean endOfBatch;
            final int eventHashCode = event.hashCode();
            switch (Math.abs(eventHashCode % 4)) {
                case 0:
                    endOfBatch = logger.isTraceEnabled();
                    break;
                case 1:
                    endOfBatch = logger.isDebugEnabled();
                    break;
                case 2:
                    endOfBatch = logger.isInfoEnabled();
                    break;
                case 3:
                    endOfBatch = logger.isWarnEnabled();
                    break;
                default:
                    throw new IllegalStateException();
            }
            event.setEndOfBatch(endOfBatch);
        }
    }
}
