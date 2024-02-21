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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.logger.TestListLogger;
import org.apache.logging.log4j.spi.StandardLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BasicPropertyEnvironmentTest {

    private static final Map<String, String> TEST_PROPS = Map.of(
            "ComponentProperties.boolAttr",
            "true",
            "ComponentProperties.intAttr",
            "123",
            "ComponentProperties.longAttr",
            "123456",
            "ComponentProperties.charsetAttr",
            "UTF-8",
            "ComponentProperties.durationAttr",
            "PT8H",
            "ComponentProperties.stringAttr",
            "Hello child!",
            "ComponentProperties.classAttr",
            "org.apache.logging.log4j.kit.env.PropertyEnvironment",
            "ComponentProperties.level",
            "INFO",
            "ComponentProperties.subComponent.subProperty",
            "Hello parent!");

    @Test
    void get_property_should_support_records() {
        final TestListLogger logger = new TestListLogger(BasicPropertyEnvironmentTest.class.getName());
        final PropertyEnvironment env = new TestPropertyEnvironment(TEST_PROPS, logger);
        final ComponentProperties expected = new ComponentProperties(
                true,
                123,
                123456L,
                StandardCharsets.UTF_8,
                Duration.ofHours(8),
                "Hello child!",
                PropertyEnvironment.class,
                StandardLevel.INFO,
                new SubComponentProperties("Hello parent!"));
        assertThat(env.getProperty(ComponentProperties.class)).isEqualTo(expected);
        assertThat(logger.getMessages()).isEmpty();
    }

    static Stream<Arguments> get_property_should_check_bounds() {
        return Stream.of(
                Arguments.of(
                        "BoundedClass.className",
                        "java.lang.String",
                        BoundedClass.class,
                        new BoundedClass(null),
                        List.of("Unable to get Class 'java.lang.String' for property 'BoundedClass.className': "
                                + "class does not extend java.lang.Number.")),
                Arguments.of(
                        "BoundedClassParam.className",
                        "java.lang.String",
                        BoundedClassParam.class,
                        new BoundedClassParam(null),
                        List.of("Unable to get Class 'java.lang.String' for property 'BoundedClassParam.className': "
                                + "class does not extend java.lang.Number.")),
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
    void get_property_should_check_bounds(
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
    record ComponentProperties(
            boolean boolAttr,
            int intAttr,
            long longAttr,
            Charset charsetAttr,
            Duration durationAttr,
            String stringAttr,
            Class<?> classAttr,
            StandardLevel level,
            SubComponentProperties subComponent) {}

    record SubComponentProperties(String subProperty) {}

    @Log4jProperty
    record BoundedClass(Class<? extends Number> className) {}

    @Log4jProperty
    record BoundedClassParam<T extends Number>(Class<T> className) {}

    private static class TestPropertyEnvironment extends BasicPropertyEnvironment {

        private final Map<String, String> props;

        public TestPropertyEnvironment(final Map<String, String> props, final Logger logger) {
            super(logger);
            this.props = props;
        }

        @Override
        public String getStringProperty(final String name) {
            return props.get(name);
        }
    }
}
