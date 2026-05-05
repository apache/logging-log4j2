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

import static org.apache.logging.log4j.core.util.ReflectionUtil.getFieldValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

@SetTestProperty(key = "log4j2.isWebapp", value = "false")
class ShutdownDisabledTest {

    private static final Field shutdownCallbackField;

    static {
        try {
            shutdownCallbackField = LoggerContext.class.getDeclaredField("shutdownCallback");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @LoggerContextSource("log4j-test3.xml")
    void testShutdownFlag(final Configuration config, final LoggerContext ctx) {
        assertThat(config.isShutdownHookEnabled())
                .as("Shutdown hook is enabled")
                .isFalse();
        assertThat(getFieldValue(shutdownCallbackField, ctx))
                .as("Shutdown callback")
                .isNull();
    }

    @Test
    void whenLoggerContextInitialized_respectsShutdownDisabled(TestInfo testInfo) {
        Configuration configuration = mockConfiguration();
        when(configuration.isShutdownHookEnabled()).thenReturn(false);
        try (final LoggerContext ctx = new LoggerContext(testInfo.getDisplayName())) {
            ctx.start(configuration);
            assertThat(ctx.isStarted()).isTrue();
            assertThat(ctx.getConfiguration()).isSameAs(configuration);
            assertThat(getFieldValue(shutdownCallbackField, ctx))
                    .as("Shutdown callback")
                    .isNull();
        }
    }

    @Test
    void whenLoggerContextStarted_ignoresShutdownDisabled(TestInfo testInfo) {
        // Traditional behavior: during reconfiguration, the shutdown hook is not removed.
        Configuration initialConfiguration = mockConfiguration();
        when(initialConfiguration.isShutdownHookEnabled()).thenReturn(true);
        Configuration configuration = mockConfiguration();
        when(configuration.isShutdownHookEnabled()).thenReturn(false);
        try (final LoggerContext ctx = new LoggerContext(testInfo.getDisplayName())) {
            ctx.start(initialConfiguration);
            assertThat(ctx.isStarted()).isTrue();
            Object shutdownCallback = getFieldValue(shutdownCallbackField, ctx);
            assertThat(shutdownCallback).as("Shutdown callback").isNotNull();
            ctx.start(configuration);
            assertThat(getFieldValue(shutdownCallbackField, ctx))
                    .as("Shutdown callback")
                    .isSameAs(shutdownCallback);
        }
    }

    private static Configuration mockConfiguration() {
        return mock(
                AbstractConfiguration.class,
                withSettings()
                        .useConstructor(null, ConfigurationSource.NULL_SOURCE)
                        .defaultAnswer(CALLS_REAL_METHODS));
    }
}
