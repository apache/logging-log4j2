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
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdConstants;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;

/**
 * Compresses a file using Zstandard compression.
 * <p>
 * Supports Zstandard compression levels, including negative fast-compression levels.
 * </p>
 *
 * @since 2.27.0
 */
public final class ZstdCompressAction extends AbstractCompressAction {

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
    public static int resolveCompressionLevel(final int compressionLevel) {
        if (compressionLevel == CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL || compressionLevel == 0) {
            return 0;
        }
        if (!ZstdUtils.isZstdCompressionAvailable()) {
            return compressionLevel;
        }
        return CompressActionSupport.resolveCompressionLevel(
                "Zstd", compressionLevel, ZstdConstants.ZSTD_CLEVEL_MIN, ZstdConstants.ZSTD_CLEVEL_MAX, 0);
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
        if (!ZstdUtils.isZstdCompressionAvailable()) {
            throw new IOException("Zstandard compression is not available: zstd-jni is missing.");
        }
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
