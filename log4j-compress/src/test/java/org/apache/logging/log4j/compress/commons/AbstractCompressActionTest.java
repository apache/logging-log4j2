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
package org.apache.logging.log4j.compress.commons;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.jimfs.Jimfs;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamProvider;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;

/**
 * Reusable tests to test compression actions.
 */
public abstract class AbstractCompressActionTest {

    private static final FileSystem FILE_SYSTEM = Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
    protected static final Path ROOT = FILE_SYSTEM.getPath("/");
    private static final Configuration CONFIGURATION = new NullConfiguration();
    protected static final CompressActionFactoryProvider PROVIDER =
            CompressActionFactoryProvider.newInstance(CONFIGURATION);

    private static final CompressorStreamProvider COMPRESSOR_STREAM_PROVIDER = new CompressorStreamFactory();

    public static final String LINE_1 = "Here is line 1. Random text: ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LINE_2 = "Here is line 2. Random text: ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LINE_3 = "Here is line 3. Random text: ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Verifies that compression succeeds using {@code Path} parameters.
     */
    protected static void verifyCompressionUsingPathParams(String algorithm, Map<String, String> compressionOptions)
            throws Exception {
        Path tempDirectory = Files.createTempDirectory(ROOT, "test-compress");
        Path source = tempDirectory.resolve("source");
        Path destination = tempDirectory.resolve("destination");

        writeTestData(source);
        CompressActionFactory factory = PROVIDER.createFactoryForAlgorithm(algorithm);
        assertThat(factory).isNotNull();
        Action action = factory.createCompressAction(source, destination, compressionOptions);
        assertThat(action).isInstanceOf(CommonsCompressAction.class);
        assertThat(action.execute()).as("Compression succeeds.").isTrue();
        verifiedCompressedData(destination, algorithm);
    }

    /**
     * Verifies that compression succeeds using {@code String} parameters.
     * <p>
     *     If @code source} and {@code destination} are not on the default file system, we need to make sure
     *     that {@link java.nio.file.Paths#get(URI)} is called instead of the
     *     {@link java.nio.file.Paths#get(String, String...)} variant.
     * </p>
     */
    protected static void verifyCompressionUsingStringParams(String algorithm, Map<String, String> compressionOptions)
            throws Exception {
        Path tempDirectory = Files.createTempDirectory(ROOT, "test-compress");
        Path source = tempDirectory.resolve("source");
        Path destination = tempDirectory.resolve("destination");

        writeTestData(source);
        CompressActionFactory factory = PROVIDER.createFactoryForAlgorithm(algorithm);
        assertThat(factory).isNotNull();
        Action action = factory.createCompressAction(
                source.toUri().toString(), destination.toUri().toString(), compressionOptions);
        assertThat(action).isInstanceOf(CommonsCompressAction.class);
        assertThat(action.execute()).as("Compression succeeds.").isTrue();
        verifiedCompressedData(destination, algorithm);
    }

    private static void writeTestData(final Path destination) throws IOException {
        try (final BufferedWriter writer = Files.newBufferedWriter(destination, UTF_8)) {
            writer.write(LINE_1);
            writer.write(System.lineSeparator());
            writer.write(LINE_2);
            writer.write(System.lineSeparator());
            writer.write(LINE_3);
        }
    }

    private static void verifiedCompressedData(final Path source, final String algorithm)
            throws IOException, CompressorException {
        try (final InputStream fileInput = Files.newInputStream(source);
                final InputStream uncompressedInput =
                        COMPRESSOR_STREAM_PROVIDER.createCompressorInputStream(algorithm, fileInput, true);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(uncompressedInput, UTF_8))) {
            assertThat(reader.lines()).containsExactly(LINE_1, LINE_2, LINE_3);
        }
    }
}
