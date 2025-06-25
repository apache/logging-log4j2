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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ScriptFilter;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorTest;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.Issue;

@SetTestProperty(key = "log4j2.scriptEnableLanguages", value = "groovy")
class AbstractConfigurationTest {

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
