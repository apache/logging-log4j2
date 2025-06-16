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

import static org.apache.logging.log4j.core.GcHelper.awaitGarbageCollection;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junitpioneer.jupiter.SetSystemProperty;

@Tag("functional")
class EventParameterMemoryLeakTest {

    @Test
    @SetSystemProperty(key = "log4j2.enableDirectEncoders", value = "true")
    @SetSystemProperty(key = "log4j2.enableThreadLocals", value = "true")
    void parameters_should_be_garbage_collected(final TestInfo testInfo) throws Throwable {
        awaitGarbageCollection(() -> {
            final ListAppender[] appenderRef = {null};
            final Logger[] loggerRef = {null};
            try (final LoggerContext ignored = createLoggerContext(testInfo, appenderRef, loggerRef)) {

                // Log messages
                final ParameterObject parameter = new ParameterObject("paramValue");
                loggerRef[0].info("Message with parameter {}", parameter);
                loggerRef[0].info(parameter);
                loggerRef[0].info("test", new ObjectThrowable(parameter));
                loggerRef[0].info("test {}", "hello", new ObjectThrowable(parameter));

                // Verify the logging
                final List<String> messages = appenderRef[0].getMessages();
                assertThat(messages).hasSize(4);
                assertThat(messages.get(0)).isEqualTo("Message with parameter %s", parameter.value);
                assertThat(messages.get(1)).isEqualTo(parameter.value);
                assertThat(messages.get(2))
                        .startsWith(String.format("test%n%s: %s", ObjectThrowable.class.getName(), parameter.value));
                assertThat(messages.get(3))
                        .startsWith(
                                String.format("test hello%n%s: %s", ObjectThrowable.class.getName(), parameter.value));

                // Return the GC subject
                return parameter;
            }
        });
    }

    private static LoggerContext createLoggerContext(
            final TestInfo testInfo, final ListAppender[] appenderRef, final Logger[] loggerRef) {
        final String loggerContextName = String.format("%s-LC", testInfo.getDisplayName());
        final LoggerContext loggerContext = new LoggerContext(loggerContextName);
        final String appenderName = "LIST";
        final Configuration configuration = createConfiguration(appenderName);
        loggerContext.start(configuration);
        appenderRef[0] = configuration.getAppender(appenderName);
        assertThat(appenderRef[0]).isNotNull();
        final Class<?> testClass = testInfo.getTestClass().orElse(null);
        assertThat(testClass).isNotNull();
        loggerRef[0] = loggerContext.getLogger(testClass);
        return loggerContext;
    }

    @SuppressWarnings("SameParameterValue")
    private static Configuration createConfiguration(final String appenderName) {
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder();
        final LayoutComponentBuilder layoutComponentBuilder =
                configBuilder.newLayout("PatternLayout").addAttribute("pattern", "%m");
        final AppenderComponentBuilder appenderComponentBuilder =
                configBuilder.newAppender(appenderName, "List").add(layoutComponentBuilder);
        final RootLoggerComponentBuilder loggerComponentBuilder =
                configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(appenderName));
        return configBuilder
                .add(appenderComponentBuilder)
                .add(loggerComponentBuilder)
                .build(false);
    }

    private static final class ParameterObject {

        private final String value;

        private ParameterObject(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class ObjectThrowable extends RuntimeException {

        private final Object object;

        private ObjectThrowable(final Object object) {
            super(String.valueOf(object));
            this.object = object;
        }

        @Override
        public String toString() {
            return "ObjectThrowable " + object;
        }
    }
}
