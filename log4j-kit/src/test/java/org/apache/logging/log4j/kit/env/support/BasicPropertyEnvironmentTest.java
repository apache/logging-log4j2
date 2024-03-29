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
package org.apache.logging.log4j.kit.env.support;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.TestPropertyEnvironment;
import org.apache.logging.log4j.kit.logger.TestListLogger;
import org.apache.logging.log4j.spi.StandardLevel;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the property values that are used as properties.
 */
class BasicPropertyEnvironmentTest {

    @Log4jProperty
    record BasicValues(boolean boolAttr, int intAttr, long longAttr) {}

    @Log4jProperty
    record DefaultBasicValues(
            @Log4jProperty(defaultValue = "true") boolean boolAttr,
            @Log4jProperty(defaultValue = "123") int intAttr,
            @Log4jProperty(defaultValue = "123456") long longAttr) {}

    @Log4jProperty(name = "BasicValues")
    record BoxedBasicValues(@Nullable Boolean boolAttr, @Nullable Integer intAttr, @Nullable Long longAttr) {}

    private static final Map<String, String> BASIC_PROPS =
            Map.of("BasicValues.boolAttr", "true", "BasicValues.intAttr", "123", "BasicValues.longAttr", "123456");

    @Test
    void should_support_basic_values() {
        assertMapConvertsTo(Map.of(), new BasicValues(false, 0, 0L));
        assertMapConvertsTo(BASIC_PROPS, new BasicValues(true, 123, 123456));
        // Default values
        assertMapConvertsTo(Map.of(), new DefaultBasicValues(true, 123, 123456));
    }

    @Test
    void should_support_boxed_values() {
        assertMapConvertsTo(Map.of(), new BoxedBasicValues(null, null, null));
        assertMapConvertsTo(BASIC_PROPS, new BoxedBasicValues(true, 123, 123456L));
        // No need to test default values, since properties with a default value should be primitives
    }

    @Log4jProperty
    record ScalarValues(
            @Nullable Charset charsetAttr,
            @Nullable Duration durationAttr,
            @Nullable String stringAttr,
            @Nullable StandardLevel enumAttr,
            @Nullable Level levelAttr,
            @Nullable Path pathAttr,
            @Nullable Locale localeAttr,
            @Nullable TimeZone timeZoneAttr,
            @Nullable ZoneId zoneIdAttr) {}

    @Log4jProperty
    record DefaultScalarValues(
            @Log4jProperty(defaultValue = "UTF-8") Charset charsetAttr,
            @Log4jProperty(defaultValue = "PT8H") Duration durationAttr,
            @Log4jProperty(defaultValue = "Hello child!") String stringAttr,
            @Log4jProperty(defaultValue = "WARN") StandardLevel enumAttr,
            @Log4jProperty(defaultValue = "INFO") Level levelAttr,
            @Log4jProperty(defaultValue = "app.log") Path pathAttr,
            @Log4jProperty(defaultValue = "en-US") Locale localeAttr,
            @Log4jProperty(defaultValue = "Europe/Warsaw") TimeZone timeZoneAttr,
            @Log4jProperty(defaultValue = "UTC+01:00") ZoneId zoneIdAttr) {}

    private static final Map<String, String> SCALAR_PROPS = Map.of(
            "ScalarValues.charsetAttr",
            "UTF-8",
            "ScalarValues.durationAttr",
            "PT8H",
            "ScalarValues.stringAttr",
            "Hello child!",
            "ScalarValues.enumAttr",
            "WARN",
            "ScalarValues.levelAttr",
            "INFO",
            "ScalarValues.pathAttr",
            "app.log",
            "ScalarValues.localeAttr",
            "en-US",
            "ScalarValues.timeZoneAttr",
            "Europe/Warsaw",
            "ScalarValues.zoneIdAttr",
            "UTC+01:00");

    @Test
    void should_support_scalar_values() {
        assertMapConvertsTo(Map.of(), new ScalarValues(null, null, null, null, null, null, null, null, null));
        assertMapConvertsTo(
                SCALAR_PROPS,
                new ScalarValues(
                        UTF_8,
                        Duration.ofHours(8),
                        "Hello child!",
                        StandardLevel.WARN,
                        Level.INFO,
                        Paths.get("app.log"),
                        Locale.forLanguageTag("en-US"),
                        TimeZone.getTimeZone("Europe/Warsaw"),
                        ZoneId.of("UTC+01:00")));
        // Default values
        assertMapConvertsTo(
                SCALAR_PROPS,
                new DefaultScalarValues(
                        UTF_8,
                        Duration.ofHours(8),
                        "Hello child!",
                        StandardLevel.WARN,
                        Level.INFO,
                        Paths.get("app.log"),
                        Locale.forLanguageTag("en-US"),
                        TimeZone.getTimeZone("Europe/Warsaw"),
                        ZoneId.of("UTC+01:00")));
    }

    @Log4jProperty
    record ArrayValues(char @Nullable [] password) {}

    private static final Map<String, String> ARRAY_PROPS = Map.of("ArrayValues.password", "changeit");

    @Test
    void should_support_arrays_of_scalars() {
        final TestListLogger logger = new TestListLogger(BasicPropertyEnvironmentTest.class.getName());
        // Missing properties
        PropertyEnvironment env = new TestPropertyEnvironment(Map.of(), logger);
        ArrayValues actual = env.getProperty(ArrayValues.class);
        assertThat(actual.password()).isNull();
        // With properties
        env = new TestPropertyEnvironment(ARRAY_PROPS, logger);
        actual = env.getProperty(ArrayValues.class);
        assertThat(actual.password()).containsExactly("changeit".toCharArray());
        // Check for warnings
        assertThat(logger.getMessages()).isEmpty();
    }

    @Log4jProperty
    record Component(@Nullable String type, SubComponent subComponent) {}

    // Subcomponents shouldn't be annotated.
    record SubComponent(@Nullable String type) {}

    private static final Map<String, String> COMPONENT_PROPS =
            Map.of("Component.type", "COMPONENT", "Component.subComponent.type", "SUBCOMPONENT");

    @Test
    void should_support_nested_records() {
        assertMapConvertsTo(Map.of(), new Component(null, new SubComponent(null)));
        assertMapConvertsTo(COMPONENT_PROPS, new Component("COMPONENT", new SubComponent("SUBCOMPONENT")));
    }

    @Log4jProperty
    record BoundedClass(Class<? extends Number> className) {}

    @Log4jProperty
    record BoundedClassParam<T extends Number>(Class<T> className) {}

    static Stream<Arguments> should_support_classes_with_bounds() {
        return Stream.of(
                Arguments.of(
                        "BoundedClass.className",
                        "java.lang.String",
                        BoundedClass.class,
                        new BoundedClass(null),
                        List.of("Invalid Class value 'java.lang.String': class does not extend java.lang.Number.")),
                Arguments.of(
                        "BoundedClassParam.className",
                        "java.lang.String",
                        BoundedClassParam.class,
                        new BoundedClassParam(null),
                        List.of("Invalid Class value 'java.lang.String': class does not extend java.lang.Number.")),
                Arguments.of(
                        "BoundedClass.className",
                        "java.lang.Integer",
                        BoundedClass.class,
                        new BoundedClass(Integer.class),
                        Collections.emptyList()),
                Arguments.of(
                        "BoundedClassParam.className",
                        "java.lang.Integer",
                        BoundedClassParam.class,
                        new BoundedClassParam(Integer.class),
                        Collections.emptyList()));
    }

    @ParameterizedTest
    @MethodSource
    void should_support_classes_with_bounds(
            final String key,
            final String value,
            final Class<?> clazz,
            final Object expected,
            final Iterable<? extends String> expectedMessages) {
        final TestListLogger logger = new TestListLogger(BasicPropertyEnvironmentTest.class.getName());
        final PropertyEnvironment env = new TestPropertyEnvironment(Map.of(key, value), logger);
        assertThat(env.getProperty(clazz)).isEqualTo(expected);
        Assertions.<String>assertThat(logger.getMessages()).containsExactlyElementsOf(expectedMessages);
    }

    @Log4jProperty
    record SystemValues(
            @Log4jProperty(defaultValue = "system") Charset charsetAttr,
            @Log4jProperty(defaultValue = "system") Locale localeAttr,
            @Log4jProperty(defaultValue = "system") TimeZone timeZoneAttr,
            @Log4jProperty(defaultValue = "system") ZoneId zoneIdAttr) {}

    @Test
    void should_support_system_defaults() {
        final TestListLogger logger = new TestListLogger(BasicPropertyEnvironmentTest.class.getName());
        final PropertyEnvironment env = new TestPropertyEnvironment(Map.of(), logger);
        assertThat(env.getProperty(SystemValues.class))
                .isEqualTo(new SystemValues(
                        Charset.defaultCharset(), Locale.getDefault(), TimeZone.getDefault(), ZoneId.systemDefault()));
        // Check for warnings
        assertThat(logger.getMessages()).isEmpty();
    }

    private void assertMapConvertsTo(final Map<String, String> map, final Object expected) {
        final TestListLogger logger = new TestListLogger(BasicPropertyEnvironmentTest.class.getName());
        final PropertyEnvironment env = new TestPropertyEnvironment(map, logger);
        final Object actual = env.getProperty(expected.getClass());
        assertThat(actual).isEqualTo(expected);
        assertThat(logger.getMessages()).isEmpty();
    }
}
