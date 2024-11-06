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
package org.apache.logging.log4j.async.logger;

import static org.apache.logging.log4j.core.test.internal.GcHelper.awaitGarbageCollection;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.TestConstants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@Tag("async")
class AsyncLoggerArgumentFreedOnErrorTest {

    /**
     * Tests events are cleared even after failure.
     *
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2725">LOG4J2-2725</a>
     */
    @Test
    @UsingStatusListener // Suppresses `StatusLogger` output, unless there is a failure
    @SetTestProperty(key = TestConstants.GC_ENABLE_DIRECT_ENCODERS, value = "true")
    @SetTestProperty(key = TestConstants.ASYNC_FORMAT_MESSAGES_IN_BACKGROUND, value = "true")
    void parameters_throwing_exception_should_be_garbage_collected(final TestInfo testInfo) throws Exception {
        awaitGarbageCollection(() -> {
            final String loggerContextName = String.format("%s-LC", testInfo.getDisplayName());
            try (final LoggerContext loggerContext =
                    new AsyncLoggerContext(loggerContextName, null, null, DI.createInitializedFactory())) {
                loggerContext.start();
                final Logger logger = loggerContext.getRootLogger();
                final ThrowingMessage parameter = new ThrowingMessage();
                logger.fatal(parameter);
                return parameter;
            }
        });
    }

    private static class ThrowingMessage implements Message, StringBuilderFormattable {

        private ThrowingMessage() {}

        @Override
        public String getFormattedMessage() {
            throw new Error("Expected");
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        @Override
        public void formatTo(final StringBuilder buffer) {
            throw new Error("Expected");
        }
    }
}
