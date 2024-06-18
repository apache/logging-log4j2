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
package org.apache.logging.log4j.core.config.plugins.visitors;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.junit.jupiter.api.Test;

class PluginBuilderAttributeTest {

    private static final String PLUGIN_NAME = "PluginBuilderAttributeTest_TestLayout";

    @Plugin(name = PLUGIN_NAME, category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
    public static final class TestLayout extends AbstractStringLayout {

        private final Builder builder;

        private TestLayout(Builder builder) {
            super(StandardCharsets.UTF_8);
            this.builder = builder;
        }

        @Override
        public String toSerializable(final LogEvent ignored) {
            throw new UnsupportedOperationException();
        }

        @PluginBuilderFactory
        public static Builder newBuilder() {
            return new Builder();
        }

        public static final class Builder implements org.apache.logging.log4j.core.util.Builder<TestLayout> {

            @PluginBuilderAttribute
            private String literal;

            @PluginBuilderAttribute("literal_II")
            private String literal2;

            @PluginBuilderAttribute
            private String propertyWithSubstitution;

            @PluginBuilderAttribute(substitute = false)
            private String propertyWithoutSubstitution;

            @Override
            public TestLayout build() {
                return new TestLayout(this);
            }
        }
    }

    @Test
    void plugin_attrs_should_get_injected() {

        // Create a configuration using the `TestLayout`
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder();
        configBuilder.addProperty("testProp", "foo");
        final LayoutComponentBuilder layoutComponentBuilder = configBuilder
                .newLayout(PLUGIN_NAME)
                .addAttribute("literal", "bar")
                .addAttribute("literal_II", "bar_II")
                .addAttribute("propertyWithSubstitution", "${testProp}")
                .addAttribute("propertyWithoutSubstitution", "${testProp}");
        final String appenderName = "CONSOLE";
        final AppenderComponentBuilder appenderComponentBuilder = configBuilder
                .newAppender(appenderName, ConsoleAppender.PLUGIN_NAME)
                .add(layoutComponentBuilder);
        configBuilder.add(appenderComponentBuilder);
        final BuiltConfiguration config = configBuilder
                .add(configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(appenderName)))
                .build(false);

        // Create a `LoggerContext` using the created `Configuration`
        try (final LoggerContext loggerContext = Configurator.initialize(config)) {

            // Extract the built layout
            final ConsoleAppender appender = loggerContext.getConfiguration().getAppender(appenderName);
            final TestLayout layout = (TestLayout) appender.getLayout();

            // Verify the property substitution
            assertThat(layout.builder.literal).isEqualTo("bar");
            assertThat(layout.builder.literal2).isEqualTo("bar_II");
            assertThat(layout.builder.propertyWithSubstitution).isEqualTo("foo");
            assertThat(layout.builder.propertyWithoutSubstitution).isEqualTo("${testProp}");
        }
    }
}
