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
package org.apache.logging.log4j.core.pattern;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Compare the full output of a renderer with expected golden sample. Therefore, changing the method name or
 * changing the position(line count) where the exception created will cause the test fail.
 */
public class ThrowableFullTest {
    @ParameterizedTest
    @MethodSource("testFull_dataSource")
    void testFull(final ThrowableRenderer<?> renderer, final String expectedFilePath) throws IOException {
        final Throwable throwable = createException("r", 1, 3);
        final String actual = render(renderer, throwable);
        final String expected = getContentFromResource(expectedFilePath);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> testFull_dataSource() {
        return Stream.of(
                Arguments.of(
                        new ThrowableRenderer<>(Collections.emptyList(), Integer.MAX_VALUE), "ThrowableRenderer.txt"),
                Arguments.of(
                        new RootThrowableRenderer(Collections.emptyList(), Integer.MAX_VALUE),
                        "RootThrowableRenderer.txt"),
                Arguments.of(
                        new ExtendedThrowableRenderer(Collections.emptyList(), Integer.MAX_VALUE),
                        "ExtendedThrowableRenderer.txt"));
    }

    private static Throwable createException(final String name, int depth, int maxDepth) {
        Exception r = new Exception(name);
        if (depth < maxDepth) {
            r.initCause(createException(name + "_c", depth + 1, maxDepth));
            r.addSuppressed(createException(name + "_s", depth + 1, maxDepth));
        }
        return r;
    }

    private static String getContentFromResource(String fileName) throws IOException {
        fileName = "throwableRenderer/" + fileName;
        String path = Objects.requireNonNull(
                        ThrowableTest.class.getClassLoader().getResource(fileName))
                .getPath();
        return new String(Files.readAllBytes(FileSystems.getDefault().getPath(path)), StandardCharsets.UTF_8);
    }

    private static String render(final ThrowableRenderer<?> renderer, final Throwable throwable) {
        final StringBuilder stringBuilder = new StringBuilder();
        renderer.renderThrowable(stringBuilder, throwable, System.lineSeparator());
        return stringBuilder.toString();
    }
}
