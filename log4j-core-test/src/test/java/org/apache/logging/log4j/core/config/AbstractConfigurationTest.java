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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ScriptFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorTest;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.ReconfigurationPolicy;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.Issue;

@SetTestProperty(key = "log4j2.scriptEnableLanguages", value = "groovy")
class AbstractConfigurationTest {

    private static final String MISMATCHED_KEY = "AbstractConfigurationTest.mismatch";

    @Test
    void propertiesCanComeLast() {
        final Configuration config = new TestConfiguration(null, Collections.singletonMap("console.name", "CONSOLE"));
        config.initialize();
        final StrSubstitutor substitutor = config.getStrSubstitutor();
        assertThat(substitutor.replace("${console.name}"))
                .as("No interpolation for '${console.name}'")
                .isEqualTo("CONSOLE");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Issue("https://github.com/apache/logging-log4j2/issues/2309")
    void substitutorHasConfigurationAndLoggerContext(final boolean hasProperties) {
        final LoggerContext context = mock(LoggerContext.class);
        final Configuration config = new TestConfiguration(context, hasProperties ? Collections.emptyMap() : null);
        config.initialize();
        final Interpolator runtime = (Interpolator) config.getStrSubstitutor().getVariableResolver();
        final Interpolator configTime =
                (Interpolator) config.getConfigurationStrSubstitutor().getVariableResolver();
        for (final Interpolator interpolator : Arrays.asList(runtime, configTime)) {
            assertThat(InterpolatorTest.getConfiguration(interpolator)).isEqualTo(config);
            assertThat(InterpolatorTest.getLoggerContext(interpolator)).isEqualTo(context);
        }
    }

    @Test
    @LoggerContextSource("log4j2-script-order-test.xml")
    void scriptRefShouldBeResolvedWhenScriptsElementIsLast(final Configuration config) {
        assertThat(config.getFilter())
                .as("Top-level filter should be a CompositeFilter")
                .isInstanceOf(CompositeFilter.class);
        final CompositeFilter compositeFilter = (CompositeFilter) config.getFilter();

        assertThat(compositeFilter.getFilters())
                .as("CompositeFilter should contain one filter")
                .hasSize(1);
        final ScriptFilter scriptFilter =
                (ScriptFilter) compositeFilter.getFilters().get(0);

        assertThat(scriptFilter).isNotNull();
        assertThat(scriptFilter.toString())
                .as("Script name should match the one in the config")
                .isEqualTo("GLOBAL_FILTER");
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3155")
    @LoggerContextSource(value = "log4j-test2.xml", reconfigure = ReconfigurationPolicy.AFTER_EACH)
    void addLoggerAppenderAttachesTheGivenAppenderForADuplicateName(final LoggerContext context) {
        final Configuration config = context.getConfiguration();
        final Logger logger = context.getLogger("AbstractConfigurationTest.duplicateAppender");
        final ListAppender registered = new ListAppender("Duplicate");
        registered.start();
        config.addAppender(registered);
        final ListAppender given = new ListAppender("Duplicate");
        given.start();

        config.addLoggerAppender(logger, given);

        final Appender found = config.getAppender("Duplicate");
        assertThat(found).isSameAs(registered);
        assertThat(logger.getAppenders()).hasSize(1).containsEntry("Duplicate", given);
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3155")
    @LoggerContextSource(value = "log4j-test2.xml", reconfigure = ReconfigurationPolicy.AFTER_EACH)
    void addLoggerFilterCreatesTheLoggerConfigOnceAndIgnoresNullArguments(final LoggerContext context) {
        final Configuration config = context.getConfiguration();
        final Logger logger = context.getLogger("AbstractConfigurationTest.filter");
        final int loggerConfigsBefore = config.getLoggers().size();
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, Filter.Result.ACCEPT, Filter.Result.DENY);

        config.addLoggerFilter(null, filter);
        config.addLoggerFilter(logger, null);
        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore);

        config.addLoggerFilter(logger, filter);
        config.addLoggerFilter(
                logger, ThresholdFilter.createFilter(Level.WARN, Filter.Result.ACCEPT, Filter.Result.DENY));
        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore + 1);
        final LoggerConfig created = config.getLoggerConfig(logger.getName());
        assertThat(created.getName()).isEqualTo(logger.getName());
        assertThat(created.getParent()).isSameAs(config.getRootLogger());
        assertThat(logger.get()).isSameAs(created);
        assertThat(logger.filterCount()).isEqualTo(2);
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3155")
    @LoggerContextSource(value = "log4j-test2.xml", reconfigure = ReconfigurationPolicy.AFTER_EACH)
    void setLoggerAdditiveCreatesTheLoggerConfigOnceAndIgnoresANullLogger(final LoggerContext context) {
        final Configuration config = context.getConfiguration();
        final Logger logger = context.getLogger("AbstractConfigurationTest.additive");
        final int loggerConfigsBefore = config.getLoggers().size();

        config.setLoggerAdditive(null, false);
        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore);

        config.setLoggerAdditive(logger, false);
        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore + 1);
        assertThat(logger.get().getName()).isEqualTo(logger.getName());
        assertThat(logger.get().getParent()).isSameAs(config.getRootLogger());
        assertThat(logger.isAdditive()).isFalse();

        config.setLoggerAdditive(logger, true);
        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore + 1);
        assertThat(logger.isAdditive()).isTrue();
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3155")
    @LoggerContextSource(value = "log4j-test2.xml", reconfigure = ReconfigurationPolicy.AFTER_EACH)
    void addLoggerAppenderUpdatesALoggerConfigRegisteredUnderAnotherName(final LoggerContext context) {
        final Configuration config = context.getConfiguration();
        final LoggerConfig registered = registerUnderAnotherName(config);
        final int loggerConfigsBefore = config.getLoggers().size();
        final ListAppender appender = new ListAppender("Mismatch");
        appender.start();

        config.addLoggerAppender(context.getLogger(MISMATCHED_KEY), appender);

        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore);
        assertThat(registered.getAppenders()).containsEntry("Mismatch", appender);
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3155")
    @LoggerContextSource(value = "log4j-test2.xml", reconfigure = ReconfigurationPolicy.AFTER_EACH)
    void addLoggerFilterUpdatesALoggerConfigRegisteredUnderAnotherName(final LoggerContext context) {
        final Configuration config = context.getConfiguration();
        final LoggerConfig registered = registerUnderAnotherName(config);
        final int loggerConfigsBefore = config.getLoggers().size();

        config.addLoggerFilter(
                context.getLogger(MISMATCHED_KEY),
                ThresholdFilter.createFilter(Level.ERROR, Filter.Result.ACCEPT, Filter.Result.DENY));

        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore);
        assertThat(registered.hasFilter()).isTrue();
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3155")
    @LoggerContextSource(value = "log4j-test2.xml", reconfigure = ReconfigurationPolicy.AFTER_EACH)
    void setLoggerAdditiveUpdatesALoggerConfigRegisteredUnderAnotherName(final LoggerContext context) {
        final Configuration config = context.getConfiguration();
        final LoggerConfig registered = registerUnderAnotherName(config);
        final int loggerConfigsBefore = config.getLoggers().size();

        config.setLoggerAdditive(context.getLogger(MISMATCHED_KEY), false);

        assertThat(config.getLoggers()).hasSize(loggerConfigsBefore);
        assertThat(registered.isAdditive()).isFalse();
    }

    /**
     * Registers a LoggerConfig under a key that differs from its name, so the logger mutators find it through
     * {@code putIfAbsent} instead of creating a new one.
     */
    private static LoggerConfig registerUnderAnotherName(final Configuration config) {
        final LoggerConfig registered = new LoggerConfig("AbstractConfigurationTest.other", Level.INFO, true);
        config.addLogger(MISMATCHED_KEY, registered);
        return registered;
    }

    private static class TestConfiguration extends AbstractConfiguration {

        private final Map<String, String> map;

        public TestConfiguration(final LoggerContext context, final Map<String, String> map) {
            super(context, ConfigurationSource.NULL_SOURCE);
            this.map = map;
        }

        @Override
        public void setup() {
            // Nodes
            final Node loggers = newNode(rootNode, "Loggers");
            rootNode.getChildren().add(loggers);

            final Node rootLogger = newNode(loggers, "Root");
            rootLogger.getAttributes().put("level", "INFO");
            loggers.getChildren().add(rootLogger);

            if (map != null) {
                final Node properties = newNode(rootNode, "Properties");
                rootNode.getChildren().add(properties);

                for (final Entry<String, String> entry : map.entrySet()) {
                    final Node property = newNode(properties, "Property");
                    property.getAttributes().put("name", entry.getKey());
                    property.getAttributes().put("value", entry.getValue());
                    properties.getChildren().add(property);
                }
            }
        }

        private Node newNode(final Node parent, final String name) {
            return new Node(parent, name, pluginManager.getPluginType(name));
        }
    }
}
