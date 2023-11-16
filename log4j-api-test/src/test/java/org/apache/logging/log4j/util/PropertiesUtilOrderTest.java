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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
@ResourceLock(value = Resources.SYSTEM_PROPERTIES)
public class PropertiesUtilOrderTest {

    public static class NonEnumerablePropertySource implements PropertySource {

        private final Properties props;

        public NonEnumerablePropertySource(final Properties props) {
            this.props = props;
        }

        @Override
        public int getPriority() {
            return Integer.MIN_VALUE;
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

    @Test
    public void testNormalizedOverrideLegacy() {
        final PropertiesUtil util = new PropertiesUtil(properties);
        final String legacy = "props.legacy";
        final String normalized = "props.normalized";
        assertEquals(legacy, properties.getProperty("log4j.legacyProperty"));
        assertTrue(util.hasProperty("log4j.legacyProperty"));
        assertEquals(normalized, util.getStringProperty("log4j.legacyProperty"));
        assertEquals(legacy, properties.getProperty("org.apache.logging.log4j.legacyProperty2"));
        assertTrue(util.hasProperty("log4j.legacyProperty2"));
        assertEquals(normalized, util.getStringProperty("org.apache.logging.log4j.legacyProperty2"));
        assertEquals(legacy, properties.getProperty("Log4jLegacyProperty3"));
        assertTrue(util.hasProperty("log4j.legacyProperty3"));
        assertEquals(normalized, util.getStringProperty("Log4jLegacyProperty3"));
        // non-overridden legacy property
        assertTrue(util.hasProperty("log4j.nonOverriddenLegacy"));
        assertEquals(legacy, util.getStringProperty("log4j.nonOverriddenLegacy"));
    }

    @Test
    public void testFallsBackToTokenMatching() {
        final PropertiesUtil util = new PropertiesUtil(properties);
        for (int i = 1; i <= 4; i++) {
            final String key = "log4j2.tokenBasedProperty" + i;
            assertTrue(util.hasProperty(key));
            assertEquals("props.token", util.getStringProperty(key));
        }
        // No fall back (a normalized property is present)
        assertTrue(util.hasProperty("log4j2.normalizedProperty"));
        assertEquals("props.normalized", util.getStringProperty("log4j2.normalizedProperty"));
    }

    @Test
    public void testOrderOfNormalizedProperties(final EnvironmentVariables env, final SystemProperties sysProps) {
        properties.remove("log4j2.normalizedProperty");
        properties.remove("LOG4J_normalized.property");
        final PropertiesUtil util = new PropertiesUtil(properties);
        // Same result for both a legacy property and a normalized property
        assertFalse(util.hasProperty("Log4jNormalizedProperty"));
        assertEquals(null, util.getStringProperty("Log4jNormalizedProperty"));
        assertFalse(util.hasProperty("log4j2.normalizedProperty"));
        assertEquals(null, util.getStringProperty("log4j2.normalizedProperty"));

        properties.setProperty("log4j2.normalizedProperty", "props.normalized");
        util.reload();
        assertTrue(util.hasProperty("Log4jNormalizedProperty"));
        assertEquals("props.normalized", util.getStringProperty("Log4jNormalizedProperty"));
        assertTrue(util.hasProperty("log4j2.normalizedProperty"));
        assertEquals("props.normalized", util.getStringProperty("log4j2.normalizedProperty"));

        env.set("LOG4J_NORMALIZED_PROPERTY", "env");
        util.reload();
        assertTrue(util.hasProperty("Log4jNormalizedProperty"));
        assertEquals("env", util.getStringProperty("Log4jNormalizedProperty"));
        assertTrue(util.hasProperty("log4j2.normalizedProperty"));
        assertEquals("env", util.getStringProperty("log4j2.normalizedProperty"));

        sysProps.set("log4j2.normalizedProperty", "sysProps");
        util.reload();
        assertTrue(util.hasProperty("Log4jNormalizedProperty"));
        assertEquals("sysProps", util.getStringProperty("Log4jNormalizedProperty"));
        assertTrue(util.hasProperty("log4j2.normalizedProperty"));
        assertEquals("sysProps", util.getStringProperty("log4j2.normalizedProperty"));
    }

    @Test
    public void testLegacySystemPropertyHasHigherPriorityThanEnv(
            final EnvironmentVariables env, final SystemProperties sysProps) {
        env.set("LOG4J_CONFIGURATION_FILE", "env");
        final PropertiesUtil util = new PropertiesUtil(properties);

        assertTrue(util.hasProperty("log4j.configurationFile"));
        assertEquals("env", util.getStringProperty("log4j.configurationFile"));

        sysProps.set("log4j.configurationFile", "legacy");
        util.reload();
        assertTrue(util.hasProperty("log4j.configurationFile"));
        assertEquals("legacy", util.getStringProperty("log4j.configurationFile"));

        sysProps.set("log4j2.configurationFile", "new");
        util.reload();
        assertTrue(util.hasProperty("log4j.configurationFile"));
        assertEquals("new", util.getStringProperty("log4j.configurationFile"));
    }

    @Test
    public void testHighPriorityNonEnumerableSource(final SystemProperties sysProps) {
        // In both datasources
        assertNotNull(properties.getProperty("log4j2.normalizedProperty"));
        assertNotNull(properties.getProperty("log4j.onlyLegacy"));
        sysProps.set("log4j2.normalizedProperty", "sysProps.normalized");
        sysProps.set("log4j.onlyLegacy", "sysProps.legazy");
        // Only system properties
        assertNull(properties.getProperty("log4j2.normalizedPropertySysProps"));
        assertNull(properties.getProperty("log4j.onlyLegacySysProps"));
        sysProps.set("log4j2.normalizedPropertySysProps", "sysProps.normalized");
        sysProps.set("log4j.onlyLegacySysProps", "sysProps.legacy");
        // Only the non enumerable source
        assertNotNull(properties.getProperty("log4j2.normalizedPropertyProps"));
        assertNotNull(properties.getProperty("log4j.onlyLegacyProps"));

        final PropertiesUtil util = new PropertiesUtil(new NonEnumerablePropertySource(properties));
        assertTrue(util.hasProperty("log4j2.normalizedProperty"));
        assertEquals("props.normalized", util.getStringProperty("log4j2.normalizedProperty"));
        assertTrue(util.hasProperty("log4j.onlyLegacy"));
        assertEquals("props.legacy", util.getStringProperty("log4j.onlyLegacy"));
        assertTrue(util.hasProperty("log4j2.normalizedPropertySysProps"));
        assertEquals("sysProps.normalized", util.getStringProperty("log4j2.normalizedPropertySysProps"));
        assertTrue(util.hasProperty("log4j.onlyLegacySysProps"));
        assertEquals("sysProps.legacy", util.getStringProperty("log4j.onlyLegacySysProps"));
        assertTrue(util.hasProperty("log4j2.normalizedPropertyProps"));
        assertEquals("props.normalized", util.getStringProperty("log4j2.normalizedPropertyProps"));
        assertTrue(util.hasProperty("log4j.onlyLegacyProps"));
        assertEquals("props.legacy", util.getStringProperty("log4j.onlyLegacyProps"));
    }

    /**
     * Checks the for missing null checks. The {@link NullPropertySource} returns
     * {@code null} in almost every call.
     *
     * @param sysProps
     */
    @Test
    public void testNullChecks(final SystemProperties sysProps) {
        sysProps.set("log4j2.someProperty", "sysProps");
        sysProps.set("Log4jLegacyProperty", "sysProps");
        final PropertiesUtil util = new PropertiesUtil(new NullPropertySource());
        assertTrue(util.hasProperty("log4j2.someProperty"));
        assertEquals("sysProps", util.getStringProperty("log4j2.someProperty"));
        assertTrue(util.hasProperty("Log4jLegacyProperty"));
        assertEquals("sysProps", util.getStringProperty("Log4jLegacyProperty"));
        assertTrue(util.hasProperty("log4j.legacyProperty"));
        assertEquals("sysProps", util.getStringProperty("log4j.legacyProperty"));
        assertFalse(util.hasProperty("log4j2.nonExistentProperty"));
        assertNull(util.getStringProperty("log4j2.nonExistentProperty"));
    }
}
