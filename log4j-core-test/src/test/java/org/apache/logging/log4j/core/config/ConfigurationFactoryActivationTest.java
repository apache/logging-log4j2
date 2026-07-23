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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.json.JsonConfigurationFactory;
import org.apache.logging.log4j.core.config.yaml.YamlConfigurationFactory;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.WritesSystemProperty;

@WritesSystemProperty
class ConfigurationFactoryActivationTest {

    private static final String[] CONFIGURATION_PROPERTIES = {
        ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
        ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY,
        ConfigurationFactory.LOG4J1_EXPERIMENTAL
    };

    private final InactiveConfigurationFactory inactiveFactory = new InactiveConfigurationFactory();
    private final Map<String, String> previousProperties = new HashMap<>();

    private Field factoriesField;
    private List<ConfigurationFactory> previousFactories;
    private ConfigurationFactory previousConfigurationFactory;

    @BeforeEach
    void setUp() throws Exception {
        previousConfigurationFactory = ConfigurationFactory.getInstance();
        previousFactories = ConfigurationFactory.getFactories();
        factoriesField = ConfigurationFactory.class.getDeclaredField("factories");
        ReflectionUtil.setStaticFieldValue(factoriesField, Collections.singletonList(inactiveFactory));
        ConfigurationFactory.resetConfigurationFactory();
        for (final String property : CONFIGURATION_PROPERTIES) {
            previousProperties.put(property, System.getProperty(property));
            System.clearProperty(property);
        }
    }

    @AfterEach
    void tearDown() {
        ReflectionUtil.setStaticFieldValue(factoriesField, previousFactories);
        ConfigurationFactory.setConfigurationFactory(previousConfigurationFactory);
        for (final String property : CONFIGURATION_PROPERTIES) {
            final String previousValue = previousProperties.get(property);
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }

    @Test
    @UsingStatusListener
    void reportsMatchingResourceWithoutInvokingInactiveFactory(final ListStatusListener listener) {
        try (final LoggerContext context = new LoggerContext("test")) {
            assertInstanceOf(
                    DefaultConfiguration.class,
                    ConfigurationFactory.getInstance().getConfiguration(context, "1", (URI) null));
        }
        assertFactoryWasChecked();
        assertTrue(inactiveFactory.supportedTypeChecks > 0);
        assertThat(listener.findStatusData(Level.ERROR))
                .anySatisfy(statusData -> assertThat(statusData.getMessage().getFormattedMessage())
                        .contains("log4j-test1.xml")
                        .contains(InactiveConfigurationFactory.class.getName()));
    }

    @Test
    void skipsInactiveFactoryForExplicitUri() {
        try (final LoggerContext context = new LoggerContext("test")) {
            assertInstanceOf(
                    DefaultConfiguration.class,
                    ConfigurationFactory.getInstance()
                            .getConfiguration(context, "test", URI.create("file:///configuration.xml")));
        }
        assertFactoryWasChecked();
    }

    @Test
    void skipsInactiveFactoryForConfiguredLocation() throws Exception {
        final URL resource = getClass().getResource("/log4j-test1.xml");
        assertNotNull(resource);
        System.setProperty(
                ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                resource.toURI().toString());
        try (final LoggerContext context = new LoggerContext("test")) {
            assertNull(ConfigurationFactory.getInstance().getConfiguration(context, "test", (URI) null));
        }
        assertFactoryWasChecked();
    }

    @Test
    void checksRequiredVersionBeforeActiveState() throws Exception {
        final URL resource = getClass().getResource("/log4j-test1.xml");
        assertNotNull(resource);
        System.setProperty(
                ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY,
                resource.toURI().toString());
        try (final LoggerContext context = new LoggerContext("test")) {
            assertNull(ConfigurationFactory.getInstance().getConfiguration(context, "test", (URI) null));
        }
        assertTrue(inactiveFactory.versionChecks > 0);
        assertEquals(0, inactiveFactory.activeChecks);
    }

    @Test
    @UsingStatusListener
    void skipsInactiveFactoryForConfigurationSource(final ListStatusListener listener) throws Exception {
        final URL resource = getClass().getResource("/log4j-test1.xml");
        assertNotNull(resource);
        final ConfigurationSource source = ConfigurationSource.fromUri(resource.toURI());
        assertNotNull(source);
        try (final LoggerContext context = new LoggerContext("test")) {
            assertNull(ConfigurationFactory.getInstance().getConfiguration(context, source));
        } finally {
            source.getInputStream().close();
        }
        assertFactoryWasChecked();
        assertThat(listener.findStatusData(Level.ERROR))
                .extracting(statusData -> statusData.getMessage().getFormattedMessage())
                .anySatisfy(message -> assertThat(message)
                        .contains("Cannot determine the ConfigurationFactory to use")
                        .contains("log4j-test1.xml"))
                .noneSatisfy(message -> assertThat(message).contains("input source is null"));
    }

    @Test
    void jsonFactoryUsesOverriddenActiveState() {
        final InactiveJsonConfigurationFactory factory = new InactiveJsonConfigurationFactory();
        assumeTrue(factory.dependenciesAvailable());
        assertThat(factory.getSupportedTypes()).containsExactly(".json", ".jsn");
        assertNull(factory.getConfiguration(null, ConfigurationSource.NULL_SOURCE));
    }

    @Test
    void yamlFactoryUsesOverriddenActiveState() {
        final InactiveYamlConfigurationFactory factory = new InactiveYamlConfigurationFactory();
        assumeTrue(factory.dependenciesAvailable());
        assertThat(factory.getSupportedTypes()).containsExactly(".yml", ".yaml");
        assertNull(factory.getConfiguration(null, ConfigurationSource.NULL_SOURCE));
    }

    private void assertFactoryWasChecked() {
        assertTrue(inactiveFactory.activeChecks > 0);
    }

    private static final class InactiveConfigurationFactory extends ConfigurationFactory {

        private int activeChecks;
        private int supportedTypeChecks;
        private int versionChecks;

        @Override
        protected boolean isActive() {
            activeChecks++;
            return false;
        }

        @Override
        protected String getVersion() {
            versionChecks++;
            return LOG4J2_VERSION;
        }

        @Override
        protected String getTestPrefix() {
            return "log4j-test";
        }

        @Override
        protected String getDefaultPrefix() {
            return "log4j2";
        }

        @Override
        protected String[] getSupportedTypes() {
            supportedTypeChecks++;
            return new String[] {".xml"};
        }

        @Override
        public Configuration getConfiguration(
                final LoggerContext loggerContext, final String name, final URI configLocation) {
            throw unexpectedCall("getConfiguration(LoggerContext, String, URI)");
        }

        @Override
        public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
            throw unexpectedCall("getConfiguration(LoggerContext, ConfigurationSource)");
        }

        private static AssertionError unexpectedCall(final String method) {
            return new AssertionError(method + " should not be called for an inactive factory");
        }
    }

    private static final class InactiveJsonConfigurationFactory extends JsonConfigurationFactory {

        @Override
        protected boolean isActive() {
            return false;
        }

        private boolean dependenciesAvailable() {
            return super.isActive();
        }
    }

    private static final class InactiveYamlConfigurationFactory extends YamlConfigurationFactory {

        @Override
        protected boolean isActive() {
            return false;
        }

        private boolean dependenciesAvailable() {
            return super.isActive();
        }
    }
}
