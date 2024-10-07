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

import static org.apache.commons.compress.compressors.CompressorStreamFactory.BZIP2;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.DEFLATE;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.LZ4_BLOCK;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.LZ4_FRAMED;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.LZMA;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.SNAPPY_FRAMED;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.XZ;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactory;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test the compression using the algorithms available without additional dependencies.
 */
@UsingStatusListener
class CommonsCompressActionTest extends AbstractCompressActionTest {

    private ListStatusListener statusListener;

    @Test
    void testConstructorDisallowsNullSource() {
        CompressActionFactory bzip2 = PROVIDER.createFactoryForAlgorithm("bzip2");
        assertThat(bzip2).isNotNull();
        assertThrows(NullPointerException.class, () -> bzip2.createCompressAction(null, ROOT.resolve("any"), Map.of()));
        assertThrows(NullPointerException.class, () -> bzip2.createCompressAction(null, "any", Map.of()));
    }

    @Test
    public void testConstructorDisallowsNullDestination() {
        CompressActionFactory bzip2 = PROVIDER.createFactoryForAlgorithm("bzip2");
        assertThat(bzip2).isNotNull();
        assertThrows(NullPointerException.class, () -> bzip2.createCompressAction(ROOT.resolve("any"), null, Map.of()));
        assertThrows(NullPointerException.class, () -> bzip2.createCompressAction("any", null, Map.of()));
    }

    @Test
    void testExecuteReturnsFalseIfSourceDoesNotExist() throws IOException {
        Path tempDirectory = Files.createTempDirectory(ROOT, "test");
        CompressActionFactory bzip2 = PROVIDER.createFactoryForAlgorithm("bzip2");
        assertThat(bzip2).isNotNull();
        final Action action = bzip2.createCompressAction(
                tempDirectory.resolve("source"), tempDirectory.resolve("destination"), Map.of());

        assertThat(action).isNotNull();
        assertThat(action.execute()).as("Compress non-existent file").isEqualTo(false);
    }

    static Stream<String> algorithms() {
        return Stream.of(
                GZIP, BZIP2, DEFLATE, SNAPPY_FRAMED, LZ4_BLOCK, LZ4_FRAMED, CustomCompressorStreamProvider.ALGORITHM);
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void verify_compression_using_Path_params(final String algorithm) throws Exception {
        verifyCompressionUsingPathParams(algorithm, Map.of());
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void verify_compression_using_String_params(final String algorithm) throws Exception {
        verifyCompressionUsingStringParams(algorithm, Map.of());
    }

    static Stream<String> when_dependency_missing_factory_returns_null_and_warns() {
        return Stream.of(XZ, LZMA, ZSTANDARD);
    }

    @ParameterizedTest
    @MethodSource
    @UsingStatusListener
    void when_dependency_missing_factory_returns_null_and_warns(final String algorithm) throws Exception {
        CompressActionFactory factory = PROVIDER.createFactoryForAlgorithm(algorithm);
        assertThat(factory)
                .as("Compression action factory for disabled algorithm.")
                .isNull();
        // We warn the user that the algorithm is not available
        assertThat(statusListener.findStatusData(Level.WARN)).anySatisfy(data -> {
            assertThat(data.getMessage().getFormattedMessage())
                    .contains("missing dependency", "https://commons.apache.org/proper/commons-compress/index.html");
        });
    }
}
