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
package org.apache.logging.log4j.core.config.builder;

import static org.assertj.core.api.Assertions.assertThatCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.CustomLevelConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.junit.jupiter.api.Test;

class CustomBuiltConfigurationTest {

    private static final String CONFIG_NAME = "FooBar Configuration";

    private static final FooBar FOOBAR = new FooBar("wingding");

    /** Test that the build configuration contains the intended attributes. */
    @Test
    void testCustomBuiltConfiguration_Attributes() {

        final LoggerContext loggerContext = new LoggerContext("CustomBuiltConfigurationTest");
        final FoobarConfigurationBuilder builder = createTestBuilder(loggerContext);
        final FooBarConfiguration config = builder.build(false);

        try {

            // build the configuration and set it in the context to start the configuration
            loggerContext.setConfiguration(config);

            assertNotNull(config);
            assertEquals(CONFIG_NAME, config.getName());
            assertEquals(10, config.getMonitorInterval());
            assertEquals(5000, config.getShutdownTimeoutMillis());
            assertThatCollection(config.getPluginPackages()).containsExactlyInAnyOrder("foo", "bar");

        } finally {

            loggerContext.stop();
        }
    }

    /** Test that the custom constructor of the custom configuration was called and the test object is accessible. */
    @Test
    void testCustomBuiltConfiguration_CustomObject() {

        final LoggerContext loggerContext = new LoggerContext("CustomBuiltConfigurationTest");
        final FoobarConfigurationBuilder builder = createTestBuilder(loggerContext);
        final FooBarConfiguration config = builder.build(false);

        try {

            // build the configuration and set it in the context to start the configuration
            loggerContext.setConfiguration(config);

            assertNotNull(config);

            final FooBar fb = config.getFooBar();
            assertNotNull(fb);
            assertEquals(FOOBAR, fb);
            assertEquals(FOOBAR.getValue(), fb.getValue());

        } finally {

            loggerContext.stop();
        }
    }

    /** Test that the build configuration contains the intended appenders. */
    @Test
    void testCustomBuiltConfiguration_Appenders() {

        final LoggerContext loggerContext = new LoggerContext("CustomBuiltConfigurationTest");
        final FoobarConfigurationBuilder builder = createTestBuilder(loggerContext);
        final FooBarConfiguration config = builder.build(false);

        try {

            // build the configuration and set it in the context to start the configuration
            loggerContext.setConfiguration(config);

            assertNotNull(config);
            assertThatCollection(config.getAppenders().keySet()).containsExactlyInAnyOrder("Stdout", "Kafka");

        } finally {

            loggerContext.stop();
        }
    }

    /** Test that the build configuration contains the custom levels. */
    @Test
    void testCustomBuiltConfiguration_CustomLevels() {

        final LoggerContext loggerContext = new LoggerContext("CustomBuiltConfigurationTest");
        final FoobarConfigurationBuilder builder = createTestBuilder(loggerContext);
        final FooBarConfiguration config = builder.build(false);

        try {

            // build the configuration and set it in the context to start the configuration
            loggerContext.setConfiguration(config);

            assertNotNull(config);
            assertThatCollection(config.getCustomLevels().stream()
                            .map(CustomLevelConfig::getLevelName)
                            .collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("Panic");

        } finally {

            loggerContext.stop();
        }
    }

    /** Test that the build configuration contains the intended filter. */
    @Test
    void testCustomBuiltConfiguration_Filter() {

        final LoggerContext loggerContext = new LoggerContext("CustomBuiltConfigurationTest");
        final FoobarConfigurationBuilder builder = createTestBuilder(loggerContext);
        final FooBarConfiguration config = builder.build(false);

        try {

            // build the configuration and set it in the context to start the configuration
            loggerContext.setConfiguration(config);

            assertNotNull(config);
            Filter filter = config.getFilter();
            assertNotNull(filter);
            assertInstanceOf(ThresholdFilter.class, filter);

        } finally {

            loggerContext.stop();
        }
    }

    /** Test that the build configuration contains the intended loggers. */
    @Test
    void testCustomBuiltConfiguration_Loggers() {

        final LoggerContext loggerContext = new LoggerContext("CustomBuiltConfigurationTest");
        final FoobarConfigurationBuilder builder = createTestBuilder(loggerContext);
        final FooBarConfiguration config = builder.build(false);

        try {

            // build the configuration and set it in the context to start the configuration
            loggerContext.setConfiguration(config);

            assertNotNull(config);
            assertThatCollection(config.getLoggers().keySet())
                    .containsExactlyInAnyOrder("", "org.apache.logging.log4j", "org.apache.logging.log4j.core");

        } finally {

            loggerContext.stop();
        }
    }

    /** Test that the build configuration correctly registers and resolves properties. */
    @Test
    void testCustomBuiltConfiguration_PropertyResolution() {

        final LoggerContext loggerContext = new LoggerContext("CustomBuiltConfigurationTest");
        final FoobarConfigurationBuilder builder = createTestBuilder(loggerContext);
        final FooBarConfiguration config = builder.build(false);

        try {

            // build the configuration and set it in the context to start the configuration
            loggerContext.setConfiguration(config);

            assertNotNull(config);
            assertEquals("Wing", config.getConfigurationStrSubstitutor().replace("${P1}"));
            assertEquals("WingDing", config.getConfigurationStrSubstitutor().replace("${P2}"));

            Appender kafkaAppender = config.getAppender("Kafka");
            assertNotNull(kafkaAppender);
            assertInstanceOf(KafkaAppender.class, kafkaAppender);
            final Property p2Property = Arrays.stream(((KafkaAppender) kafkaAppender).getPropertyArray())
                    .collect(Collectors.toMap(Property::getName, Function.identity()))
                    .get("P2");
            assertNotNull(p2Property);
            assertEquals("WingDing", p2Property.getValue());

        } finally {

            loggerContext.stop();
        }
    }

    /**
     * Creates, preconfigures and returns a test builder instance.
     * @param loggerContext the logger context to use
     * @return the created configuration builder
     */
    private FoobarConfigurationBuilder createTestBuilder(final LoggerContext loggerContext) {
        final FoobarConfigurationBuilder builder = new FoobarConfigurationBuilder();
        builder.setLoggerContext(loggerContext);
        addTestFixtures(builder);
        return builder;
    }

    /**
     * Populates the given configuration-builder.
     * @param builder the builder
     */
    private void addTestFixtures(final ConfigurationBuilder<? extends BuiltConfiguration> builder) {
        builder.setConfigurationName(CONFIG_NAME);
        builder.setStatusLevel(Level.ERROR);
        builder.setMonitorInterval(10);
        builder.setShutdownTimeout(5000, TimeUnit.MILLISECONDS);
        builder.add(builder.newScriptFile("target/test-classes/scripts/filter.groovy")
                .addIsWatched(true));
        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                .addAttribute("level", Level.DEBUG));

        final AppenderComponentBuilder appenderBuilder =
                builder.newAppender("Stdout", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(
                builder.newLayout("PatternLayout").addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .addAttribute("marker", "FLOW"));
        builder.add(appenderBuilder);

        final AppenderComponentBuilder appenderBuilder2 =
                builder.newAppender("Kafka", "Kafka").addAttribute("topic", "my-topic");
        appenderBuilder2.addComponent(builder.newProperty("bootstrap.servers", "localhost:9092"));
        appenderBuilder2.addComponent(builder.newProperty("P2", "${P2}"));
        appenderBuilder2.add(builder.newLayout("GelfLayout")
                .addAttribute("host", "my-host")
                .addComponent(builder.newKeyValuePair("extraField", "extraValue")));
        builder.add(appenderBuilder2);

        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG, true)
                .add(builder.newAppenderRef("Stdout"))
                .addAttribute("additivity", false));
        builder.add(builder.newLogger("org.apache.logging.log4j.core").add(builder.newAppenderRef("Stdout")));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));

        builder.addProperty("P1", "Wing");
        builder.addProperty("P2", "${P1}Ding");
        builder.add(builder.newCustomLevel("Panic", 17));
        builder.setPackages("foo,bar");
    }

    //
    // Test implementations
    //

    /** A custom {@link DefaultConfigurationBuilder} implementation that generates a {@link FooBarConfiguration}. */
    public static class FoobarConfigurationBuilder extends DefaultConfigurationBuilder<FooBarConfiguration> {

        public FoobarConfigurationBuilder() {
            super(FooBarConfiguration.class);
        }

        /** {@inheritDoc} */
        @Override
        protected FooBarConfiguration createNewConfigurationInstance(Class<FooBarConfiguration> configurationClass) {
            Objects.requireNonNull(configurationClass, "The 'configurationClass' argument must not be null.");
            try {
                final Constructor<FooBarConfiguration> constructor = FooBarConfiguration.class.getConstructor(
                        LoggerContext.class, ConfigurationSource.class, Component.class, FooBar.class);
                return constructor.newInstance(
                        getLoggerContext().orElse(null),
                        getConfigurationSource().orElse(null),
                        getRootComponent(),
                        FOOBAR);
            } catch (final Exception ex) {
                throw new IllegalStateException(
                        "Configuration class '" + configurationClass.getName() + "' cannot be instantiated.", ex);
            }
        }
    }

    /**
     * A custom {@link BuiltConfiguration} implementation with a custom constructor that takes
     * an additional {@code FooBar} argument.
     */
    public static class FooBarConfiguration extends BuiltConfiguration {

        private int monitorInterval;

        private final FooBar fooBar;

        public FooBarConfiguration(
                LoggerContext loggerContext, ConfigurationSource source, Component rootComponent, FooBar fooBar) {
            super(loggerContext, source, rootComponent);
            this.fooBar = Objects.requireNonNull(fooBar, "fooBar");
        }

        public FooBar getFooBar() {
            return fooBar;
        }

        public int getMonitorInterval() {
            return this.monitorInterval;
        }

        /** {@inheritDoc} */
        @Override
        public void setMonitorInterval(int seconds) {
            super.setMonitorInterval(seconds);
            this.monitorInterval = seconds;
        }
    }

    /** Test object used by custom configuration-builder and configuration. */
    public static class FooBar {

        private final String value;

        public FooBar(final String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
