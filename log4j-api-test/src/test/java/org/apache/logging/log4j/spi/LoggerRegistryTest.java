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
package org.apache.logging.log4j.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.ref.WeakReference;
import java.util.stream.Stream;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.test.TestLogger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LoggerRegistryTest {

    private static final String LOGGER_NAME = LoggerRegistryTest.class.getName();

    static Stream<@Nullable MessageFactory> doesNotLoseLoggerReferences() {
        return Stream.of(
                ParameterizedMessageFactory.INSTANCE,
                ReusableMessageFactory.INSTANCE,
                new ParameterizedMessageFactory(),
                null);
    }

    /**
     * @see <a href="https://github.com/apache/logging-log4j2/issues/3143>Issue #3143</a>
     */
    @ParameterizedTest
    @MethodSource
    void doesNotLoseLoggerReferences(@Nullable MessageFactory messageFactory) {
        LoggerRegistry<TestLogger> loggerRegistry = new LoggerRegistry<>();
        TestLogger logger = new TestLogger(LOGGER_NAME, messageFactory);
        WeakReference<TestLogger> loggerRef = new WeakReference<>(logger);
        // Register logger
        loggerRegistry.putIfAbsent(LOGGER_NAME, messageFactory, logger);
        // The JIT compiler/optimizer might figure out by himself the `logger` and `messageFactory` are no longer used:
        //   https://shipilev.net/jvm/anatomy-quarks/8-local-var-reachability/
        // We help him with the task though.
        logger = null;
        // Trigger a GC run
        System.gc();
        // Check if the logger is still there
        assertThat(loggerRef.get()).isNotNull();
        assertThat(loggerRegistry.getLogger(LOGGER_NAME, messageFactory)).isInstanceOf(TestLogger.class);
    }
}
