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
package org.apache.logging.log4j.core.config.plugins.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GraalVmProcessorTest {

    private static final Object FAKE_PLUGIN = asMap(
            "name",
            FakePlugin.class.getName(),
            "methods",
            asList(
                    asMap("name", "<init>", "parameterTypes", emptyList()),
                    asMap(
                            "name",
                            "newPlugin",
                            "parameterTypes",
                            asList(
                                    "int",
                                    "org.apache.logging.log4j.core.Layout",
                                    "org.apache.logging.log4j.core.config.Configuration",
                                    "org.apache.logging.log4j.core.config.Node",
                                    "org.apache.logging.log4j.core.LoggerContext",
                                    "java.lang.String"))),
            "fields",
            emptyList());
    private static final Object FAKE_PLUGIN_BUILDER = asMap(
            "name",
            FakePlugin.Builder.class.getName(),
            "methods",
            emptyList(),
            "fields",
            asList(
                    asMap("name", "attribute"),
                    asMap("name", "attributeWithoutPublicSetterButWithSuppressAnnotation"),
                    asMap("name", "config"),
                    asMap("name", "layout"),
                    asMap("name", "loggerContext"),
                    asMap("name", "node"),
                    asMap("name", "value")));
    private static final Object FAKE_PLUGIN_NESTED = onlyNoArgsConstructor(FakePlugin.Nested.class);
    private static final Object FAKE_CONSTRAINT_VALIDATOR =
            onlyNoArgsConstructor(FakeAnnotations.FakeConstraintValidator.class);
    private static final Object FAKE_PLUGIN_VISITOR = onlyNoArgsConstructor(FakeAnnotations.FakePluginVisitor.class);

    /**
     * Generates a metadata element with just a single no-arg constructor.
     *
     * @param clazz The name of the metadata element.
     * @return A GraalVM metadata element.
     */
    private static Object onlyNoArgsConstructor(Class<?> clazz) {
        return asMap(
                "name",
                clazz.getName(),
                "methods",
                singletonList(asMap("name", "<init>", "parameterTypes", emptyList())),
                "fields",
                emptyList());
    }

    private static Map<String, ?> asMap(Object... pairs) {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("odd number of arguments: " + pairs.length);
        }
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static String reachabilityMetadata;

    @BeforeAll
    static void setup() throws IOException {
        // There are two descriptors, choose the one in `test-classes`
        URL reachabilityMetadataUrl = null;
        for (URL url : Collections.list(GraalVmProcessor.class
                .getClassLoader()
                .getResources("META-INF/native-image/org.apache.logging.log4j/log4j-core-test/reflect-config.json"))) {
            if (url.getPath().contains("test-classes")) {
                reachabilityMetadataUrl = url;
                break;
            }
        }
        assertThat(reachabilityMetadataUrl).isNotNull();
        reachabilityMetadata = IOUtils.toString(reachabilityMetadataUrl, StandardCharsets.UTF_8);
    }

    static Stream<Arguments> containsSpecificEntries() {
        return Stream.of(
                Arguments.of(FakePlugin.class, FAKE_PLUGIN),
                Arguments.of(FakePlugin.Builder.class, FAKE_PLUGIN_BUILDER),
                Arguments.of(FakePlugin.Nested.class, FAKE_PLUGIN_NESTED),
                Arguments.of(FakeAnnotations.FakeConstraintValidator.class, FAKE_CONSTRAINT_VALIDATOR),
                Arguments.of(FakeAnnotations.FakePluginVisitor.class, FAKE_PLUGIN_VISITOR));
    }

    @ParameterizedTest
    @MethodSource
    void containsSpecificEntries(Class<?> clazz, Object expectedJson) {
        assertThatJson(reachabilityMetadata)
                .inPath(filterByName(clazz))
                .isArray()
                .contains(json(expectedJson));
    }

    private String filterByName(Class<?> clazz) {
        return String.format("$[?(@.name == '%s')]", clazz.getName());
    }
}
