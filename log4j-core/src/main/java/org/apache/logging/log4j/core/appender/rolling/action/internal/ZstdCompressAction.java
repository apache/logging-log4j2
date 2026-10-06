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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdConstants;

/**
 * Compresses a file using Zstandard compression.
 * <p>
 * Supports positive compression levels in the range [{@value #MIN_COMPRESSION_LEVEL}, {@link ZstdConstants#ZSTD_CLEVEL_MAX}].
 * Negative (fast-compression) levels are not currently supported; this may change in a future release.
 * </p>
 */
public final class ZstdCompressAction extends AbstractCompressAction {

    /**
     * Minimum supported Zstd compression level. Negative (fast-compression) levels are intentionally
     * out of scope for now; see the discussion at
     * https://github.com/apache/logging-log4j2/discussions/2950.
     */
    static final int MIN_COMPRESSION_LEVEL = 1;

    /**
     * Zstandard compression level to use.
     *
     * @see ZstdCompressorOutputStream.Builder#setLevel(int)
     */
    private final int compressionLevel;

    /**
     * Validates that the compression level is a positive integer in the range [{@value #MIN_COMPRESSION_LEVEL}, {@link ZstdConstants#ZSTD_CLEVEL_MAX}].
     *
     * @param compressionLevel Zstandard compression level
     * @return the compression level if valid
     * @throws IllegalArgumentException if compressionLevel is not in the range [{@value #MIN_COMPRESSION_LEVEL}, {@link ZstdConstants#ZSTD_CLEVEL_MAX}]
     */
    private static int checkCompressionLevel(final int compressionLevel) {
        final int minCompressionLevel = MIN_COMPRESSION_LEVEL;
        final int maxCompressionLevel = ZstdConstants.ZSTD_CLEVEL_MAX;

        if (compressionLevel < minCompressionLevel || compressionLevel > maxCompressionLevel) {
            if (compressionLevel < 0) {
                throw new IllegalArgumentException(
                        "Negative Zstd fast-compression levels are not yet supported by Log4j2 (got: "
                                + compressionLevel
                                + "). Only the standard range ["
                                + minCompressionLevel
                                + ", "
                                + maxCompressionLevel
                                + "] is currently supported.");
            }
            throw new IllegalArgumentException("Zstd compression level must be in the range ["
                    + minCompressionLevel
                    + ", "
                    + maxCompressionLevel
                    + "], got: "
                    + compressionLevel);
        }
        return compressionLevel;
    }

    /**
     * Resolves the configured level: -1 is the rollover framework's "unspecified" sentinel, and 0 is the level
     * Zstd itself reads as "use the default level".
     *
     * @param compressionLevel configured Zstandard compression level
     * @return the level to compress with
     */
    static int resolveCompressionLevel(final int compressionLevel) {
        return compressionLevel == Deflater.DEFAULT_COMPRESSION || compressionLevel == 0
                ? ZstdConstants.ZSTD_CLEVEL_DEFAULT
                : compressionLevel;
    }

    /**
     * Creates a new instance.
     *
     * @param source           file to compress, may not be null.
     * @param destination      compressed file, may not be null.
     * @param deleteSource     if true, attempt to delete file on completion.
     * @param compressionLevel Zstandard compression level.
     */
    public ZstdCompressAction(
            final File source, final File destination, final boolean deleteSource, final int compressionLevel) {
        super(source, destination, deleteSource);
        this.compressionLevel = compressionLevel;
    }

    /** Validates the resolved Zstandard compression level. */
    @Override
    protected void validateCompressionLevel() {
        checkCompressionLevel(resolveCompressionLevel(compressionLevel));
    }

    @Override
    protected OutputStream createCompressorOutputStream(final OutputStream output) throws IOException {
        // -1 (Deflater.DEFAULT_COMPRESSION) is the framework-wide sentinel for an unspecified compression level.
        // Zstd uses -1 as a distinct fast-compression level, so map the sentinel to Zstd's default level here.
        final int level = resolveCompressionLevel(compressionLevel);
        return ZstdCompressorOutputStream.builder()
                .setOutputStream(output)
                .setLevel(level)
                .get();
    }

    /**
     * Capture exception.
     *
     * @param ex exception.
     */
    @Override
    protected void reportException(final Exception ex) {
        LOGGER.warn("Exception during compression of '" + source.toString() + "'.", ex);
    }

    @Override
    public String toString() {
        return ZstdCompressAction.class.getSimpleName() + '[' + source + " to " + destination + ", deleteSource="
                + deleteSource + ']';
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }
}
