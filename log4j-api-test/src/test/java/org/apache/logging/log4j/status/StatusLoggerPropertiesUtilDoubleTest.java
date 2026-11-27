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
package org.apache.logging.log4j.status;

import static java.util.Collections.singletonMap;
import static org.apache.logging.log4j.status.StatusLogger.PropertiesUtilsDouble.readAllAvailableProperties;
import static org.apache.logging.log4j.status.StatusLogger.PropertiesUtilsDouble.readProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.org.webcompere.systemstubs.SystemStubs.restoreSystemProperties;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

import java.security.Permission;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyPermission;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StatusLoggerPropertiesUtilDoubleTest {

    private static final String[] MATCHING_PROPERTY_NAMES = new String[] {
        // System properties for version range `[, 2.10)`
        "log4j2.StatusLogger.DateFormat",
        // System properties for version range `[2.10, 3)`
        "log4j2.statusLoggerDateFormat",
        // System properties for version range `[3,)`
        "log4j2.StatusLogger.dateFormat",
        // Environment variables
        "LOG4J_STATUS_LOGGER_DATE_FORMAT"
    };

    private static final String[] NOT_MATCHING_PROPERTY_NAMES =
            new String[] {"log4j2.StatusLogger$DateFormat", "log4j2.StàtusLögger.DateFormat"};

    private static final class TestCase {

        private final boolean matching;

        private final String propertyName;

        private final String userProvidedPropertyName;

        private TestCase(final boolean matching, final String propertyName, final String userProvidedPropertyName) {
            this.matching = matching;
            this.propertyName = propertyName;
            this.userProvidedPropertyName = userProvidedPropertyName;
        }

        @Override
        public String toString() {
            return String.format("`%s` %s `%s`", propertyName, matching ? "==" : "!=", userProvidedPropertyName);
        }
    }

    static Stream<TestCase> testCases() {
        return Stream.concat(
                testCases(true, MATCHING_PROPERTY_NAMES, MATCHING_PROPERTY_NAMES),
                testCases(false, MATCHING_PROPERTY_NAMES, NOT_MATCHING_PROPERTY_NAMES));
    }

    private static Stream<TestCase> testCases(
            final boolean matching, final String[] propertyNames, final String[] userProvidedPropertyNames) {
        return Arrays.stream(propertyNames).flatMap(propertyName -> Arrays.stream(userProvidedPropertyNames)
                .map(userProvidedPropertyName -> new TestCase(matching, propertyName, userProvidedPropertyName)));
    }

    @ParameterizedTest
    @MethodSource("testCases")
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ_WRITE)
    void system_properties_should_work(final TestCase testCase) throws Exception {
        restoreSystemProperties(() -> {
            final String expectedValue = "foo";
            System.setProperty(testCase.propertyName, expectedValue);
            verifyProperty(testCase, expectedValue);
        });
    }

    @ParameterizedTest
    @MethodSource("testCases")
    @ResourceLock(value = Resources.GLOBAL, mode = ResourceAccessMode.READ_WRITE)
    void environment_variables_should_work(final TestCase testCase) throws Exception {
        final String expectedValue = "bar";
        withEnvironmentVariable(testCase.propertyName, expectedValue).execute(() -> {
            verifyProperty(testCase, expectedValue);
        });
    }

    private static void verifyProperty(final TestCase testCase, final String expectedValue) {
        final Map<String, Object> normalizedProperties = readAllAvailableProperties();
        final String actualValue = readProperty(normalizedProperties, testCase.userProvidedPropertyName);
        if (testCase.matching) {
            assertThat(actualValue).describedAs("" + testCase).isEqualTo(expectedValue);
        } else {
            assertThat(actualValue).describedAs("" + testCase).isNull();
        }
    }

    static Stream<String> systemPropertyNames() {
        return Stream.of(
                "log4j2.debug",
                "log4j2.status.entries",
                "log4j2.statusEntries",
                "log4j2.StatusLogger.level",
                "log4j2.statusLoggerLevel",
                "log4j2.StatusLogger.dateFormat",
                "log4j2.StatusLogger.DateFormat",
                "log4j2.statusLoggerDateFormat",
                "log4j2.StatusLogger.dateFormatZone",
                "log4j2.statusLoggerDateFormatZone");
    }

    static Stream<String> environmentVariableNames() {
        return Stream.of(
                "LOG4J_DEBUG",
                "LOG4J_STATUS_ENTRIES",
                "LOG4J_STATUS_LOGGER_LEVEL",
                "LOG4J_STATUS_LOGGER_DATE_FORMAT",
                "LOG4J_STATUS_LOGGER_DATE_FORMAT_ZONE");
    }

    @ParameterizedTest
    @MethodSource("systemPropertyNames")
    @ResourceLock(value = Resources.GLOBAL, mode = ResourceAccessMode.READ_WRITE)
    void system_properties_should_be_read_one_by_one_if_bulk_access_is_denied(final String propertyName)
            throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty(propertyName, "foo");
            final Map<String, Object> normalizedProperties =
                    readAllAvailablePropertiesDenying(new PropertyPermission("*", "read,write"));
            assertThat(readProperty(normalizedProperties, propertyName)).isEqualTo("foo");
        });
    }

    @Test
    @ResourceLock(value = Resources.GLOBAL, mode = ResourceAccessMode.READ_WRITE)
    void system_property_should_be_skipped_if_access_is_denied() throws Exception {
        restoreSystemProperties(() -> {
            System.setProperty("log4j2.debug", "true");
            final Map<String, Object> normalizedProperties = readAllAvailablePropertiesDenying(
                    new PropertyPermission("*", "read,write"), new PropertyPermission("log4j2.debug", "read"));
            assertThat(readProperty(normalizedProperties, "log4j2.debug")).isNull();
        });
    }

    @ParameterizedTest
    @MethodSource("environmentVariableNames")
    @ResourceLock(value = Resources.GLOBAL, mode = ResourceAccessMode.READ_WRITE)
    void environment_variables_should_be_read_one_by_one_if_bulk_access_is_denied(final String variableName)
            throws Exception {
        withEnvironmentVariable(variableName, "bar").execute(() -> {
            final Map<String, Object> normalizedProperties =
                    readAllAvailablePropertiesDenying(new RuntimePermission("getenv.*"));
            assertThat(readProperty(normalizedProperties, variableName)).isEqualTo("bar");
        });
    }

    @Test
    @ResourceLock(value = Resources.GLOBAL, mode = ResourceAccessMode.READ_WRITE)
    void environment_variable_should_be_skipped_if_access_is_denied() throws Exception {
        withEnvironmentVariable("LOG4J_DEBUG", "true").execute(() -> {
            final Map<String, Object> normalizedProperties = readAllAvailablePropertiesDenying(
                    new RuntimePermission("getenv.*"), new RuntimePermission("getenv.LOG4J_DEBUG"));
            assertThat(readProperty(normalizedProperties, "LOG4J_DEBUG")).isNull();
        });
    }

    private static Map<String, Object> readAllAvailablePropertiesDenying(final Permission... deniedPermissions) {
        final SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission(final Permission permission) {
                if (Arrays.asList(deniedPermissions).contains(permission)) {
                    throw new SecurityException("denied: " + permission);
                }
            }
        };
        final SecurityManager previousSecurityManager = System.getSecurityManager();
        try {
            System.setSecurityManager(securityManager);
        } catch (final UnsupportedOperationException error) {
            Assumptions.abort("The JVM does not allow setting a security manager");
        }
        try {
            return readAllAvailableProperties();
        } finally {
            System.setSecurityManager(previousSecurityManager);
        }
    }

    @Test
    void properties_file_in_class_path_should_be_read() {
        final String propertiesFileName = StatusLoggerPropertiesUtilDoubleTest.class.getSimpleName() + ".properties";
        final Properties actualProperties = StatusLogger.PropertiesUtilsDouble.readPropertiesFile(propertiesFileName);
        assertThat(actualProperties).containsExactlyEntriesOf(singletonMap("foo", "bar"));
    }
}
