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
import org.apache.logging.log4j.core.appender.rolling.action.AbstractCompressAction;

/**
 * Compresses a file using Zstandard compression.
 * <p>
 * Supports positive compression levels in the range [{@value #MIN_COMPRESSION_LEVEL}, {@link ZstdConstants#ZSTD_CLEVEL_MAX}].
 * Negative (fast-compression) levels are not currently supported; this may change in a future release.
 * </p>
 *
 * @since 2.27.0
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
     * Resolves the configured level, falling back to the Zstd default for unsupported values.
     *
     * @param compressionLevel configured Zstandard compression level
     * @return the level to compress with
     */
    static int resolveCompressionLevel(final int compressionLevel) {
        if (compressionLevel == Deflater.DEFAULT_COMPRESSION || compressionLevel == 0) {
            return ZstdConstants.ZSTD_CLEVEL_DEFAULT;
        }
        return CompressActionSupport.resolveCompressionLevel(
                "Zstd",
                compressionLevel,
                MIN_COMPRESSION_LEVEL,
                ZstdConstants.ZSTD_CLEVEL_MAX,
                ZstdConstants.ZSTD_CLEVEL_DEFAULT);
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
        super(source, destination, deleteSource, "zstd");
        this.compressionLevel = compressionLevel;
    }

    @Override
    protected OutputStream createCompressorOutputStream(final OutputStream output) throws IOException {
        // -1 (Deflater.DEFAULT_COMPRESSION) is the framework-wide sentinel for an unspecified compression level.
        // Zstd uses -1 as a distinct fast-compression level, so resolve it to Zstd's default level here.
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
