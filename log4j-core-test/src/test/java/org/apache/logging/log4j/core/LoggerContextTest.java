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
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFactory2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class LoggerContextTest {

    private static final int LOGGER_COUNT = 1024;
    private static final int CONCURRENCY_LEVEL = 16;

    @Test
    void newInstance_should_honor_name_and_message_factory(final TestInfo testInfo) {
        final String testName = testInfo.getDisplayName();
        try (final LoggerContext loggerContext = new LoggerContext(testName)) {
            final String loggerName = testName + "-loggerName";
            final MessageFactory2 messageFactory = mock(MessageFactory2.class);
            final Logger logger = loggerContext.newInstance(loggerContext, loggerName, messageFactory);
            assertThat(logger.getName()).isEqualTo(loggerName);
            assertThat((MessageFactory) logger.getMessageFactory()).isSameAs(messageFactory);
        }
    }

    @Test
    void getLoggers_can_be_updated_concurrently(final TestInfo testInfo) {
        final String testName = testInfo.getDisplayName();
        final ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENCY_LEVEL);
        try (LoggerContext loggerContext = new LoggerContext(testName)) {
            // Create a logger
            Collection<Future<?>> tasks = IntStream.range(0, CONCURRENCY_LEVEL)
                    .mapToObj(i -> executorService.submit(() -> {
                        // Iterates over loggers
                        loggerContext.updateLoggers();
                        // Create some loggers
                        for (int j = 0; j < LOGGER_COUNT; j++) {
                            loggerContext.getLogger(testName + "-" + i + "-" + j);
                        }
                        // Iterate over loggers again
                        loggerContext.updateLoggers();
                    }))
                    .collect(Collectors.toList());
            Assertions.assertDoesNotThrow(() -> {
                for (Future<?> task : tasks) {
                    task.get();
                }
            });
        } finally {
            executorService.shutdown();
        }
    }
}
