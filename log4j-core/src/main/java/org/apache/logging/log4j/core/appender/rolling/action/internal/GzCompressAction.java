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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractCompressAction;

/**
 * Compresses a file using GZ compression.
 */
public final class GzCompressAction extends AbstractCompressAction {

    /**
     * GZIP compression level to use.
     *
     * @see Deflater#setLevel(int)
     */
    private final int compressionLevel;

    /**
     * Create new instance of GzCompressAction.
     *
     * @param source       file to compress, may not be null.
     * @param destination  compressed file, may not be null.
     * @param compressionLevel
     *                     Gzip deflater compression level.
     */
    public GzCompressAction(final Path source, final Path destination, final int compressionLevel) {
        super(source, destination);
        this.compressionLevel = compressionLevel;
    }

    @Override
    protected OutputStream wrapOutputStream(OutputStream stream) throws IOException {
        final OutputStream gzipOut = new ConfigurableLevelGZIPOutputStream(stream, BUF_SIZE, compressionLevel);
        // Reduce native invocations by buffering data into GZIPOutputStream
        return new BufferedOutputStream(gzipOut, BUF_SIZE);
    }

    @Override
    protected String getAlgorithmName() {
        return "GZ";
    }

    private static final class ConfigurableLevelGZIPOutputStream extends GZIPOutputStream {

        ConfigurableLevelGZIPOutputStream(final OutputStream out, final int bufSize, final int level)
                throws IOException {
            super(out, bufSize);
            def.setLevel(level);
        }
    }
}
