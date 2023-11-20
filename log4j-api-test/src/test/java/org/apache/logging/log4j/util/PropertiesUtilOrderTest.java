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
package org.apache.logging.log4j.util;

import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.spi.PropertyComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
@ResourceLock(value = Resources.SYSTEM_PROPERTIES)
public class PropertiesUtilOrderTest {

    public static class NonEnumerablePropertySource implements PropertySource {

        private final Properties props;

        public NonEnumerablePropertySource(final Properties props) {
            this.props = props;
        }

        @Override
        public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
            final CharSequence camelCase = PropertySource.Util.joinAsCamelCase(tokens);
            return camelCase.length() > 0 ? "log4j2." + camelCase : null;
        }

        @Override
        public String getProperty(final String key) {
            return props.getProperty(key);
        }

        @Override
        public boolean containsProperty(final String key) {
            return getProperty(key) != null;
        }

    }

    public static class NullPropertySource implements PropertySource {

        @Override
        public int getPriority() {
            return Integer.MIN_VALUE;
        }

    }

    private final Properties properties = new Properties();

    @BeforeEach
    public void setUp() throws Exception {
        try (final InputStream is = ClassLoader.getSystemResourceAsStream("PropertiesUtilOrderTest.properties")) {
            properties.load(is);
        }
    }

    /*
        Verify that system properties and environment variables override properties provided in files.
     */
    @Test
    public void testOrderOfProperties(final EnvironmentVariables env, final SystemProperties sysProps) {
        env.set("log4j2.*.Configuration.statusLoggerLevel", "ERROR");
        env.set("log4j2.*.StatusLogger.entries", "200");
        env.set("log4j2.my-app.StatusLogger.entries", "500");
        sysProps.set("log4j2.my-app.Configuration.mergeStrategy", "org.apache.CustomMergeStrategy");
        sysProps.set("log4j2.*.Web.isWebApp", "true");
        PropertiesUtil.getProperties().reload();
        final PropertiesUtil util = new PropertiesUtil(properties);
        final PropertiesUtil contextProperties = PropertiesUtil.getContextProperties("my-app", util);
        assertFalse(util.hasProperty(TestProperty.CONFIG_LOCATION));
        assertTrue(contextProperties.hasProperty(TestProperty.CONFIG_LOCATION));
        assertTrue(util.hasProperty(TestProperty.CONFIG_TEST1));
        assertTrue(contextProperties.hasProperty(TestProperty.CONFIG_TEST1));
        // Context environment properties should override system-wide environment definition.
        assertEquals("500", contextProperties.getStringProperty(LoggingSystemProperty.STATUS_MAX_ENTRIES));
        // Context system property should override Context property from file.
        assertEquals("org.apache.CustomMergeStrategy", contextProperties.getStringProperty(TestProperty.CONFIG_MERGE_STRATEGY));
        // Recognize system-wide environment variable.
        assertEquals("200", util.getStringProperty(LoggingSystemProperty.STATUS_MAX_ENTRIES));
        // System-wide system property should be used when no other value is available.
        assertEquals("true", contextProperties.getStringProperty(LoggingSystemProperty.IS_WEBAPP));
        // Context property in file should override system-wide property in file.
        assertEquals("override", contextProperties.getStringProperty(TestProperty.CONFIG_TEST1));
        // System wide property should be used.
        assertEquals("system", util.getStringProperty(TestProperty.CONFIG_TEST1));
        // Context Property should be used.
        assertEquals("Groovy,JavaScript", contextProperties.getStringProperty(TestProperty.SCRIPT_LANGUAGES));
        // No property should be found.
        assertFalse(util.hasProperty(TestProperty.SCRIPT_LANGUAGES));

    }

    public enum TestProperty implements PropertyKey {
        CONFIG_LOCATION(PropertyComponent.CONFIGURATION, "location"),
        CONFIG_MERGE_STRATEGY(PropertyComponent.CONFIGURATION, "mergeStrategy"),
        CONFIG_STATUS_LOGGER_LEVEL(PropertyComponent.CONFIGURATION, "statusLoggerLevel"),
        CONFIG_TEST1(PropertyComponent.CONFIGURATION, "test1"),
        JNDI_JMS(PropertyComponent.JNDI, "enableJMS"),
        LOGGER_CONTEXT_PROP1(PropertyComponent.LOGGER_CONTEXT, "prop1"),
        SCRIPT_LANGUAGES(PropertyComponent.SCRIPT, "enableLanguages");

        private final PropertyComponent component;
        private final String key;

        TestProperty(final PropertyComponent component, final String key) {
            this.component = component;
            this.key = key;
        }


        @Override
        public String getComponent() {
            return component.getName();
        }

        @Override
        public String getName() {
            return key;
        }
    }

}
