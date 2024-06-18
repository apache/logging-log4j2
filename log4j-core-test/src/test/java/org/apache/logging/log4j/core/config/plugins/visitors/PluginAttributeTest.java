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
import java.util.Map;
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
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.junit.jupiter.api.Test;

class PluginAttributeTest {

    private static final String PLUGIN_NAME = "PluginAttributeTest_TestLayout";

    @SuppressWarnings("FieldCanBeLocal")
    @Plugin(name = PLUGIN_NAME, category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
    public static final class TestLayout extends AbstractStringLayout {

        private final String propertyWithSubstitution;

        private final String propertyWithoutSubstitution;

        private final boolean booleanProperty;

        private final boolean booleanPropertyWithDefault;

        private final byte byteProperty;

        private final byte bytePropertyWithDefault;

        private final char charProperty;

        private final char charPropertyWithDefault;

        private final double doubleProperty;

        private final double doublePropertyWithDefault;

        private final float floatProperty;

        private final float floatPropertyWithDefault;

        private final long longProperty;

        private final long longPropertyWithDefault;

        private final int intProperty;

        private final int intPropertyWithDefault;

        private final short shortProperty;

        private final short shortPropertyWithDefault;

        private final String stringProperty;

        private final String stringPropertyWithDefault;

        private final Class<?> classProperty;

        private final Class<?> classPropertyWithDefault;

        public TestLayout(
                final String propertyWithSubstitution,
                final String propertyWithoutSubstitution,
                final boolean booleanProperty,
                final boolean booleanPropertyWithDefault,
                final byte byteProperty,
                final byte bytePropertyWithDefault,
                final char charProperty,
                final char charPropertyWithDefault,
                final double doubleProperty,
                final double doublePropertyWithDefault,
                final float floatProperty,
                final float floatPropertyWithDefault,
                final long longProperty,
                final long longPropertyWithDefault,
                final int intProperty,
                final int intPropertyWithDefault,
                final short shortProperty,
                final short shortPropertyWithDefault,
                final String stringProperty,
                final String stringPropertyWithDefault,
                final Class<?> classProperty,
                final Class<?> classPropertyWithDefault) {
            super(StandardCharsets.UTF_8);
            this.propertyWithSubstitution = propertyWithSubstitution;
            this.propertyWithoutSubstitution = propertyWithoutSubstitution;
            this.booleanProperty = booleanProperty;
            this.booleanPropertyWithDefault = booleanPropertyWithDefault;
            this.byteProperty = byteProperty;
            this.bytePropertyWithDefault = bytePropertyWithDefault;
            this.charProperty = charProperty;
            this.charPropertyWithDefault = charPropertyWithDefault;
            this.doubleProperty = doubleProperty;
            this.doublePropertyWithDefault = doublePropertyWithDefault;
            this.floatProperty = floatProperty;
            this.floatPropertyWithDefault = floatPropertyWithDefault;
            this.longProperty = longProperty;
            this.longPropertyWithDefault = longPropertyWithDefault;
            this.intProperty = intProperty;
            this.intPropertyWithDefault = intPropertyWithDefault;
            this.shortProperty = shortProperty;
            this.shortPropertyWithDefault = shortPropertyWithDefault;
            this.stringProperty = stringProperty;
            this.stringPropertyWithDefault = stringPropertyWithDefault;
            this.classProperty = classProperty;
            this.classPropertyWithDefault = classPropertyWithDefault;
        }

        @PluginFactory
        public static TestLayout createTestLayout(
                // Following parameters are for testing `substitute` flag
                @PluginAttribute("propertyWithSubstitution") String propertyWithSubstitution,
                @PluginAttribute(value = "propertyWithoutSubstitution", substitute = false)
                        String propertyWithoutSubstitution,
                // Following attributes are for testing
                // 1. Primitive type reads
                // 2. Defaults for primitive types
                @PluginAttribute(value = "booleanProperty") boolean booleanProperty,
                @PluginAttribute(value = "booleanPropertyWithDefault", defaultBoolean = true)
                        boolean booleanPropertyWithDefault,
                @PluginAttribute(value = "byteProperty") byte byteProperty,
                @PluginAttribute(value = "bytePropertyWithDefault", defaultByte = (byte) 42)
                        byte bytePropertyWithDefault,
                @PluginAttribute(value = "charProperty") char charProperty,
                @PluginAttribute(value = "charPropertyWithDefault", defaultChar = 'c') char charPropertyWithDefault,
                @PluginAttribute(value = "doubleProperty") double doubleProperty,
                @PluginAttribute(value = "doublePropertyWithDefault", defaultDouble = 1.0D)
                        double doublePropertyWithDefault,
                @PluginAttribute(value = "floatProperty") float floatProperty,
                @PluginAttribute(value = "floatPropertyWithDefault", defaultFloat = 1.0F)
                        float floatPropertyWithDefault,
                @PluginAttribute(value = "longProperty") long longProperty,
                @PluginAttribute(value = "longPropertyWithDefault", defaultLong = 1L) long longPropertyWithDefault,
                @PluginAttribute(value = "intProperty") int intProperty,
                @PluginAttribute(value = "intPropertyWithDefault", defaultInt = 1) int intPropertyWithDefault,
                @PluginAttribute(value = "shortProperty") short shortProperty,
                @PluginAttribute(value = "shortPropertyWithDefault", defaultShort = 1) short shortPropertyWithDefault,
                @PluginAttribute(value = "stringProperty") String stringProperty,
                @PluginAttribute(value = "stringPropertyWithDefault", defaultString = "baz")
                        String stringPropertyWithDefault,
                @PluginAttribute(value = "classProperty") Class<?> classProperty,
                @PluginAttribute(value = "classPropertyWithDefault", defaultClass = Map.class)
                        Class<?> classPropertyWithDefault) {
            return new TestLayout(
                    propertyWithSubstitution,
                    propertyWithoutSubstitution,
                    booleanProperty,
                    booleanPropertyWithDefault,
                    byteProperty,
                    bytePropertyWithDefault,
                    charProperty,
                    charPropertyWithDefault,
                    doubleProperty,
                    doublePropertyWithDefault,
                    floatProperty,
                    floatPropertyWithDefault,
                    longProperty,
                    longPropertyWithDefault,
                    intProperty,
                    intPropertyWithDefault,
                    shortProperty,
                    shortPropertyWithDefault,
                    stringProperty,
                    stringPropertyWithDefault,
                    classProperty,
                    classPropertyWithDefault);
        }

        @Override
        public String toSerializable(final LogEvent ignored) {
            throw new UnsupportedOperationException();
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
                .addAttribute("propertyWithSubstitution", "${testProp}")
                .addAttribute("propertyWithoutSubstitution", "${testProp}")
                .addAttribute("booleanProperty", "true")
                .addAttribute("byteProperty", "42")
                .addAttribute("charProperty", "c")
                .addAttribute("doubleProperty", "1.0")
                .addAttribute("floatProperty", "1.0")
                .addAttribute("longProperty", "1")
                .addAttribute("intProperty", "1")
                .addAttribute("shortProperty", "1")
                .addAttribute("stringProperty", "baz")
                .addAttribute("classProperty", Map.class.getCanonicalName());

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
            assertThat(layout.propertyWithSubstitution).isEqualTo("foo");
            assertThat(layout.propertyWithoutSubstitution).isEqualTo("${testProp}");
            assertThat(layout.booleanProperty).isTrue();
            assertThat(layout.booleanPropertyWithDefault).isTrue();
            assertThat(layout.byteProperty).isEqualTo((byte) 42);
            assertThat(layout.bytePropertyWithDefault).isEqualTo((byte) 42);
            assertThat(layout.charProperty).isEqualTo('c');
            assertThat(layout.charPropertyWithDefault).isEqualTo('c');
            assertThat(layout.doubleProperty).isEqualTo(1.0D);
            assertThat(layout.doublePropertyWithDefault).isEqualTo(1.0D);
            assertThat(layout.floatProperty).isEqualTo(1.0F);
            assertThat(layout.floatPropertyWithDefault).isEqualTo(1.0F);
            assertThat(layout.longProperty).isEqualTo(1);
            assertThat(layout.longPropertyWithDefault).isEqualTo(1);
            assertThat(layout.intProperty).isEqualTo(1);
            assertThat(layout.intPropertyWithDefault).isEqualTo(1);
            assertThat(layout.shortProperty).isEqualTo((short) 1);
            assertThat(layout.shortPropertyWithDefault).isEqualTo((short) 1);
            assertThat(layout.stringProperty).isEqualTo("baz");
            assertThat(layout.stringPropertyWithDefault).isEqualTo("baz");
            assertThat(layout.classProperty).isEqualTo(Map.class);
            assertThat(layout.classPropertyWithDefault).isEqualTo(Map.class);
        }
    }
}
