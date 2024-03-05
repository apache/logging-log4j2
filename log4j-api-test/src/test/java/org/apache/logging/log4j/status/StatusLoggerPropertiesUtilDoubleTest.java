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

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
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

    @Test
    void properties_file_in_class_path_should_be_read() {
        final String propertiesFileName = StatusLoggerPropertiesUtilDoubleTest.class.getSimpleName() + ".properties";
        final Properties actualProperties = StatusLogger.PropertiesUtilsDouble.readPropertiesFile(propertiesFileName);
        assertThat(actualProperties).containsExactlyEntriesOf(singletonMap("foo", "bar"));
    }
}
