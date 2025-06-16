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
package org.apache.logging.log4j.core.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.FailOnceAppender;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies {@link AsyncAppender} works after certain type of {@link Appender}
 * failures.
 * <p>
 * {@code AsyncAppender} thread is known to get killed due to
 * {@link AppenderControl} leaking exceptions in the past. This class is more
 * of an end-to-end test to verify that {@code AsyncAppender} still works even
 * if the background thread gets killed.
 */
class AsyncAppenderExceptionHandlingTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                FailOnceAppender.ThrowableClassName.RUNTIME_EXCEPTION,
                FailOnceAppender.ThrowableClassName.LOGGING_EXCEPTION,
                FailOnceAppender.ThrowableClassName.EXCEPTION,
                FailOnceAppender.ThrowableClassName.ERROR,
                FailOnceAppender.ThrowableClassName.THROWABLE,
                FailOnceAppender.ThrowableClassName.THREAD_DEATH
            })
    void AsyncAppender_should_not_stop_on_appender_failures(final String throwableClassName) {

        // Create the logger.
        final String throwableClassNamePropertyName = "throwableClassName";
        System.setProperty(throwableClassNamePropertyName, throwableClassName);
        try (final LoggerContext loggerContext =
                Configurator.initialize("Test", "AsyncAppenderExceptionHandlingTest.xml")) {
            final Logger logger = loggerContext.getRootLogger();

            // Log the 1st message, which should fail due to the FailOnceAppender.
            logger.info("message #1");

            // Log the 2nd message, which should succeed.
            final String lastLogMessage = "message #2";
            logger.info(lastLogMessage);

            // Stop the AsyncAppender to drain the queued events.
            final Configuration configuration = loggerContext.getConfiguration();
            final AsyncAppender asyncAppender = configuration.getAppender("Async");
            assertNotNull(asyncAppender, "couldn't obtain the FailOnceAppender");
            asyncAppender.stop();

            // Verify the logged message.
            final FailOnceAppender failOnceAppender = configuration.getAppender("FailOnce");
            assertNotNull(failOnceAppender, "couldn't obtain the FailOnceAppender");
            assertTrue(failOnceAppender.isFailed(), "FailOnceAppender hasn't failed yet");
            final List<String> accumulatedMessages = failOnceAppender.drainEvents().stream()
                    .map(LogEvent::getMessage)
                    .map(Message::getFormattedMessage)
                    .collect(Collectors.toList());
            assertEquals(Collections.singletonList(lastLogMessage), accumulatedMessages);

        } finally {
            System.setProperty(throwableClassNamePropertyName, Strings.EMPTY);
        }
    }
}
