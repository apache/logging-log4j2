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
package org.apache.logging.log4j.core.java9;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that the Java 9 variants of {@code log4j-core} classes are loaded from the {@code META-INF/versions/9}
 * directory of {@code log4j-core.jar}.
 * <p>
 * This test must run against the packaged multi-release JAR, hence it is an integration test.
 * </p>
 */
class Java9VariantsIT {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "org/apache/logging/log4j/core/impl/ExtendedStackTraceElement.class",
                "org/apache/logging/log4j/core/jackson/ExtendedStackTraceElementMixIn.class",
                "org/apache/logging/log4j/core/jackson/Log4jStackTraceElementDeserializer.class",
                "org/apache/logging/log4j/core/jackson/StackTraceElementMixIn.class",
                "org/apache/logging/log4j/core/util/internal/UnsafeUtil.class"
            })
    void java9VariantIsLoadedFromMultiReleaseJar(final String resourceName) {
        final URL resource = LoggerContext.class.getClassLoader().getResource(resourceName);
        assertThat(resource)
                .as("resource `%s`", resourceName)
                .isNotNull()
                .asString()
                .contains("META-INF/versions/9/" + resourceName);
    }
}
