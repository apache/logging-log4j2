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
package org.apache.logging.log4j.core.appender.rolling;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that the {@code compressionLevel} attribute of the rollover strategies reaches the compressor, when it is
 * read from a configuration file.
 */
@UsingStatusListener // Disables status logger unless an error occurs
@ResourceLock("compression.level")
class RolloverStrategyCompressionLevelConfigTest {

    private static final String PACKAGE = "org/apache/logging/log4j/core/appender/rolling/";

    @TempDir
    Path tempDir;

    /**
     * The level is a property, which also checks that the attribute supports substitution.
     */
    private long compressedSize(final String configuration, final String level) throws IOException {
        final Path directory = Files.createDirectories(tempDir.resolve("level" + level));
        System.setProperty("compression.dir", directory.toString());
        System.setProperty("compression.level", level);
        try {
            final LoggerContext context = Configurator.initialize("CompressionLevel", PACKAGE + configuration);
            final Logger logger = context.getLogger(getClass());
            for (int i = 0; i < 500; i++) {
                logger.debug("This is test message number {} with some filler text that repeats: ABCDEFGHIJKL", i);
            }
            assertThat(context.stop(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            System.clearProperty("compression.dir");
            System.clearProperty("compression.level");
        }
        try (Stream<Path> files = Files.list(directory)) {
            final List<Path> archives =
                    files.filter(file -> file.toString().endsWith(".gz")).toList();
            assertThat(archives).as("Archives").hasSizeGreaterThan(3);
            long size = 0;
            for (final Path archive : archives) {
                size += Files.size(archive);
                try (InputStream input = new GZIPInputStream(Files.newInputStream(archive))) {
                    assertThat(input.readAllBytes())
                            .as("Content of %s", archive)
                            .isNotEmpty();
                }
            }
            return size;
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"RolloverStrategyCompressionLevelDefault.xml", "RolloverStrategyCompressionLevelDirectWrite.xml"
            })
    void configured_level_is_used_by_the_compressor(final String configuration) throws IOException {
        final long stored = compressedSize(configuration, "0");
        final long best = compressedSize(configuration, "9");
        assertThat(best).as("Size with level 9 compared to level 0").isLessThan(stored);
    }
}
