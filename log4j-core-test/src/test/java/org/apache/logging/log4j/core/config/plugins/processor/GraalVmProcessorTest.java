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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GraalVmProcessorTest {

    private static final String FAKE_PLUGIN = String.join(
            "",
            "  {",
            "    \"name\": \"org.apache.logging.log4j.core.config.plugins.processor.FakePlugin\",",
            "    \"methods\": [",
            "      {",
            "        \"name\": \"<init>\",",
            "        \"parameterTypes\": []",
            "      },",
            "      {",
            "        \"name\": \"newPlugin\",",
            "        \"parameterTypes\": [",
            "          \"int\",",
            "          \"org.apache.logging.log4j.core.Layout\",",
            "          \"org.apache.logging.log4j.core.config.Configuration\",",
            "          \"org.apache.logging.log4j.core.config.Node\",",
            "          \"org.apache.logging.log4j.core.LoggerContext\",",
            "          \"java.lang.String\"",
            "        ]",
            "      }",
            "    ],",
            "    \"fields\": []",
            "  }");
    private static final String FAKE_PLUGIN_BUILDER = String.join(
            "\n",
            "{",
            "    \"name\": \"org.apache.logging.log4j.core.config.plugins.processor.FakePlugin$Builder\",",
            "    \"methods\": [],",
            "    \"fields\": [",
            "      {",
            "        \"name\": \"attribute\"",
            "      },",
            "      {",
            "        \"name\": \"config\"",
            "      },",
            "      {",
            "        \"name\": \"layout\"",
            "      },",
            "      {",
            "        \"name\": \"loggerContext\"",
            "      },",
            "      {",
            "        \"name\": \"node\"",
            "      },",
            "      {",
            "        \"name\": \"value\"",
            "      }",
            "    ]",
            "  }");
    private static final String FAKE_PLUGIN_NESTED = String.join(
            "\n",
            "  {",
            "    \"name\": \"org.apache.logging.log4j.core.config.plugins.processor.FakePlugin$Nested\",",
            "    \"methods\": [",
            "      {",
            "        \"name\": \"<init>\",",
            "        \"parameterTypes\": []",
            "      }",
            "    ],",
            "    \"fields\": []",
            "  }");
    private static final String FAKE_CONSTRAINT_VALIDATOR = String.join(
            "\n",
            "  {",
            "    \"name\": \"org.apache.logging.log4j.core.config.plugins.processor.FakeAnnotations$FakeConstraintValidator\",",
            "    \"methods\": [",
            "      {",
            "        \"name\": \"<init>\",",
            "        \"parameterTypes\": []",
            "      }",
            "    ],",
            "    \"fields\": []",
            "  }");
    private static final String FAKE_PLUGIN_VISITOR = String.join(
            "\n",
            "  {",
            "    \"name\": \"org.apache.logging.log4j.core.config.plugins.processor.FakeAnnotations$FakePluginVisitor\",",
            "    \"methods\": [",
            "      {",
            "        \"name\": \"<init>\",",
            "        \"parameterTypes\": []",
            "      }",
            "    ],",
            "    \"fields\": []",
            "  }");

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
        Assertions.assertThat(reachabilityMetadataUrl).isNotNull();
        reachabilityMetadata =
                IOUtils.toString(Objects.requireNonNull(reachabilityMetadataUrl), StandardCharsets.UTF_8);
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
    void containsSpecificEntries(Class<?> clazz, String expectedJson) {
        assertThatJson(reachabilityMetadata)
                .inPath(filterByName(clazz))
                .isArray()
                .contains(json(expectedJson));
    }

    private String filterByName(Class<?> clazz) {
        return String.format("$[?(@.name == '%s')]", clazz.getName());
    }
}
