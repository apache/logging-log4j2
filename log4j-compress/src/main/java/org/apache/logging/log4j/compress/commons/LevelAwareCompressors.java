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

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.IntConsumer;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateParameters;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.jspecify.annotations.Nullable;

/**
 * Creates compressor streams that use the native compression level of the algorithm.
 * <p>
 *     The algorithms that appear in {@link #SUPPORT} are the ones for which Commons Compress can configure a level.
 *     The streams are created with the Commons Compress classes of the algorithm, not with the
 *     {@link org.apache.commons.compress.compressors.CompressorStreamProvider} of the factory. A custom provider that
 *     replaces one of these algorithms is therefore not used, when a compression level is configured.
 * </p>
 */
final class LevelAwareCompressors {

    @FunctionalInterface
    private interface Wrapper {
        OutputStream wrap(OutputStream stream, int level) throws IOException;
    }

    /**
     * @param validator Throws an {@link IllegalArgumentException} for levels the algorithm does not support.
     * @param wrapper Creates the compressor stream for a level accepted by the validator.
     */
    private record Support(IntConsumer validator, Wrapper wrapper) {}

    /*
     * The references to classes of optional dependencies (zstd-jni) are in lambdas, so that they are only
     * resolved if the algorithm is used.
     */
    private static final Map<String, Support> SUPPORT = Map.of(
            CompressorStreamFactory.GZIP,
            new Support(
                    LevelAwareCompressors::gzipParameters,
                    (stream, level) -> new GzipCompressorOutputStream(stream, gzipParameters(level))),
            CompressorStreamFactory.BZIP2,
            new Support(
                    level -> {
                        if (level < BZip2CompressorOutputStream.MIN_BLOCKSIZE
                                || level > BZip2CompressorOutputStream.MAX_BLOCKSIZE) {
                            throw new IllegalArgumentException("Unsupported bzip2 block size: " + level);
                        }
                    },
                    BZip2CompressorOutputStream::new),
            CompressorStreamFactory.DEFLATE,
            new Support(
                    LevelAwareCompressors::deflateParameters,
                    (stream, level) -> new DeflateCompressorOutputStream(stream, deflateParameters(level))),
            CompressorStreamFactory.ZSTANDARD,
            // Zstandard adjusts levels outside of its range
            new Support(level -> {}, (stream, level) -> Zstd.wrap(stream, level)));

    private LevelAwareCompressors() {}

    private static @Nullable Support find(final String algorithm) {
        return SUPPORT.get(toRootLowerCase(algorithm));
    }

    /**
     * Checks if the compression level can be configured for the algorithm.
     */
    static boolean isSupported(final String algorithm) {
        return find(algorithm) != null;
    }

    /**
     * Checks that the level is supported by the algorithm, without creating a compressor.
     *
     * @throws IllegalArgumentException if the level is not supported by the algorithm.
     */
    static void validate(final String algorithm, final int level) {
        requireSupport(algorithm).validator().accept(level);
    }

    /**
     * Wraps a stream in a compressor stream with the given level.
     *
     * @param level A level accepted by {@link #validate(String, int)}.
     */
    static OutputStream wrap(final String algorithm, final OutputStream stream, final int level) throws IOException {
        return requireSupport(algorithm).wrapper().wrap(stream, level);
    }

    private static Support requireSupport(final String algorithm) {
        final Support support = find(algorithm);
        if (support == null) {
            throw new IllegalArgumentException("Compression levels are not supported for " + algorithm);
        }
        return support;
    }

    private static GzipParameters gzipParameters(final int level) {
        final GzipParameters parameters = new GzipParameters();
        parameters.setCompressionLevel(level);
        return parameters;
    }

    private static DeflateParameters deflateParameters(final int level) {
        final DeflateParameters parameters = new DeflateParameters();
        parameters.setCompressionLevel(level);
        return parameters;
    }

    /**
     * Isolates the references to the optional {@code zstd-jni} dependency.
     */
    private static final class Zstd {

        static OutputStream wrap(final OutputStream stream, final int level) throws IOException {
            return new org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream(stream, level);
        }
    }
}
