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
package org.apache.logging.log4j.layout.template.json.resolver;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.asMap;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.usingSerializedLogEventAccessor;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.writeJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PatternResolverPropertySubstitutionTest {

    private static final String DEFAULT_PROPERTY_NAME = "DEFAULT";

    private static final String FALLBACK_PROPERTY_NAME = "FALLBACK";

    private static final class TestCase {

        private final Map<String, String> properties;

        private final LogEvent logEvent;

        private final String expectedOutput;

        private TestCase(final Map<String, String> properties, final LogEvent logEvent, final String expectedOutput) {
            this.properties = properties;
            this.logEvent = logEvent;
            this.expectedOutput = expectedOutput;
        }
    }

    static Stream<TestCase> testCases() {
        return Stream.of(
                new TestCase(
                        Collections.singletonMap(DEFAULT_PROPERTY_NAME, "%m"),
                        Log4jLogEvent.newBuilder()
                                .setMessage(new SimpleMessage("foo"))
                                .build(),
                        "foo"),
                new TestCase(
                        Collections.singletonMap(FALLBACK_PROPERTY_NAME, "%p"),
                        Log4jLogEvent.newBuilder().setLevel(Level.DEBUG).build(),
                        "DEBUG"));
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void test_property_substitution(final TestCase testCase) {

        // Create the event template
        final String pattern = String.format("${%s:-${%s}}", DEFAULT_PROPERTY_NAME, FALLBACK_PROPERTY_NAME);
        final String eventTemplate = writeJson(asMap("output", asMap("$resolver", "pattern", "pattern", pattern)));

        // Create configuration with properties
        final DefaultConfigurationBuilder<BuiltConfiguration> configBuilder = new DefaultConfigurationBuilder<>();
        testCase.properties.forEach(configBuilder::addProperty);
        final Configuration config = configBuilder.build();

        // Create the layout
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(config)
                .setEventTemplate(eventTemplate)
                .build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, testCase.logEvent, accessor -> assertThat(accessor.getString("output"))
                .isEqualTo(testCase.expectedOutput));
    }
}
