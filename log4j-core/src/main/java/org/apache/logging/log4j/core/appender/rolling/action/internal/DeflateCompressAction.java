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
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateParameters;

/** Compresses a file using Deflate compression. */
public final class DeflateCompressAction extends AbstractCompressAction {

    private final int compressionLevel;

    public DeflateCompressAction(
            final File source, final File destination, final boolean deleteSource, final int compressionLevel) {
        super(source, destination, deleteSource);
        this.compressionLevel = compressionLevel;
    }

    @Override
    protected void validateCompressionLevel() {
        if (compressionLevel < Deflater.DEFAULT_COMPRESSION || compressionLevel > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Deflate compression level must be in the range ["
                    + Deflater.DEFAULT_COMPRESSION
                    + ", "
                    + Deflater.BEST_COMPRESSION
                    + "], got: "
                    + compressionLevel);
        }
    }

    @Override
    protected OutputStream createCompressorOutputStream(final OutputStream output) throws IOException {
        final DeflateParameters parameters = new DeflateParameters();
        if (compressionLevel != Deflater.DEFAULT_COMPRESSION) {
            parameters.setCompressionLevel(compressionLevel);
        }
        return new DeflateCompressorOutputStream(output, parameters);
    }
}
