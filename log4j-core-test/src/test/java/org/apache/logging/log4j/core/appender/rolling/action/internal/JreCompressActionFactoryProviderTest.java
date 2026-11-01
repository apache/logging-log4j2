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
package org.apache.logging.log4j.core.appender.rolling.action.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UsingStatusListener
class JreCompressActionFactoryProviderTest {

    private static final String COMPRESSION_LEVEL = CompressActionFactory.COMPRESSION_LEVEL;

    private ListStatusListener statusListener;

    @TempDir
    Path tempDir;

    private CompressActionFactory factory(final String algorithm) {
        final CompressActionFactory factory = CompressActionFactoryProvider.newInstance(new NullConfiguration())
                .createFactoryForAlgorithm(algorithm);
        assertThat(factory).isNotNull();
        return factory;
    }

    /**
     * Compresses into a fresh directory, because ZIP entries contain the name of the source file.
     */
    private long compressedSize(final String algorithm, final Map<String, String> options) throws IOException {
        final Path directory = Files.createTempDirectory(tempDir, "compress");
        final Path source = directory.resolve("source");
        final Path destination = directory.resolve("destination");
        Files.writeString(source, DATA);
        assertThat(factory(algorithm)
                        .createCompressAction(source, destination, options)
                        .execute())
                .isTrue();
        return Files.size(destination);
    }

    private static final String DATA = "Repetitive log line: ABCDEFGHIJKLMNOPQRSTUVWXYZ\n".repeat(2000);

    @ParameterizedTest
    @ValueSource(strings = {"gz", "zip"})
    void configured_level_is_applied(final String algorithm) throws IOException {
        final long stored = compressedSize(algorithm, Map.of(COMPRESSION_LEVEL, "0"));
        final long best = compressedSize(algorithm, Map.of(COMPRESSION_LEVEL, "9"));
        assertThat(best).isLessThan(stored);
        assertThat(statusListener.findStatusData(Level.WARN)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"gz", "zip"})
    void default_level_is_used_without_option(final String algorithm) throws IOException {
        final long unspecified = compressedSize(algorithm, Map.of());
        final long explicitDefault = compressedSize(algorithm, Map.of(COMPRESSION_LEVEL, "-1"));
        assertThat(explicitDefault).isEqualTo(unspecified);
        assertThat(statusListener.findStatusData(Level.WARN)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"gz", "zip"})
    void unsupported_level_falls_back_to_default_and_warns_once(final String algorithm) throws IOException {
        final long unspecified = compressedSize(algorithm, Map.of());
        final CompressActionFactory factory = factory(algorithm);
        for (int i = 0; i < 2; i++) {
            final Path directory = Files.createTempDirectory(tempDir, "compress");
            final Path source = directory.resolve("source");
            final Path destination = directory.resolve("destination");
            Files.writeString(source, DATA);
            assertThat(factory.createCompressAction(source, destination, Map.of(COMPRESSION_LEVEL, "10"))
                            .execute())
                    .isTrue();
            assertThat(Files.size(destination)).isEqualTo(unspecified);
        }
        assertThat(statusListener.findStatusData(Level.WARN)).hasSize(1).allSatisfy(data -> assertThat(
                        data.getMessage().getFormattedMessage())
                .contains("compression level '10'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "2147483648"})
    void non_numeric_level_falls_back_to_default(final String level) throws IOException {
        final long unspecified = compressedSize("gz", Map.of());
        assertThat(compressedSize("gz", Map.of(COMPRESSION_LEVEL, level))).isEqualTo(unspecified);
        assertThat(statusListener.findStatusData(Level.WARN)).hasSize(1);
    }
}
