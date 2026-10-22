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
package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import org.apache.logging.log4j.core.appender.rolling.action.internal.CompressActionSupport;

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

    private static int resolveCompressionLevel(final int compressionLevel) {
        return CompressActionSupport.resolveCompressionLevel(
                "GZIP",
                compressionLevel,
                Deflater.DEFAULT_COMPRESSION,
                Deflater.BEST_COMPRESSION,
                Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Creates a new instance.
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion.
     * @param compressionLevel Gzip deflater compression level.
     */
    public GzCompressAction(
            final File source, final File destination, final boolean deleteSource, final int compressionLevel) {
        super(source, destination, deleteSource);
        this.compressionLevel = compressionLevel;
    }

    /**
     * Prefer the constructor with compression level.
     *
     * @deprecated Prefer {@link GzCompressAction#GzCompressAction(File, File, boolean, int)}.
     */
    @Deprecated
    public GzCompressAction(final File source, final File destination, final boolean deleteSource) {
        this(source, destination, deleteSource, Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Compress.
     *
     * @return true if successfully compressed.
     * @throws IOException on IO exception.
     */
    @Override
    public boolean execute() throws IOException {
        return super.execute();
    }

    /**
     * Creates the GZIP stream using the configured level or the default for unsupported levels.
     *
     * @param output destination output stream
     * @return compressor output stream
     * @throws IOException on IO exception.
     */
    @Override
    protected OutputStream createCompressorOutputStream(final OutputStream output) throws IOException {
        return new ConfigurableLevelGZIPOutputStream(output, BUF_SIZE, resolveCompressionLevel(compressionLevel));
    }

    /**
     * Compress a file.
     *
     * @param source       file to compress, may not be null.
     * @param destination  compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion.  Failure to delete
     *                     does not cause an exception to be thrown or affect return value.
     * @return true if source file compressed.
     * @throws IOException on IO exception.
     * @deprecated In favor of {@link #execute(File, File, boolean, int)}.
     */
    @Deprecated
    public static boolean execute(final File source, final File destination, final boolean deleteSource)
            throws IOException {
        return execute(source, destination, deleteSource, Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Compress a file.
     *
     * @param source       file to compress, may not be null.
     * @param destination  compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion.  Failure to delete
     *                     does not cause an exception to be thrown or affect return value.
     * @param compressionLevel
     *                     Gzip deflater compression level.
     * @return true if source file compressed.
     * @throws IOException on IO exception.
     */
    public static boolean execute(
            final File source, final File destination, final boolean deleteSource, final int compressionLevel)
            throws IOException {
        return CompressActionSupport.execute(
                source,
                destination,
                deleteSource,
                output -> new ConfigurableLevelGZIPOutputStream(
                        output, BUF_SIZE, resolveCompressionLevel(compressionLevel)));
    }

    private static final class ConfigurableLevelGZIPOutputStream extends GZIPOutputStream {

        ConfigurableLevelGZIPOutputStream(final OutputStream out, final int bufSize, final int level)
                throws IOException {
            super(out, bufSize);
            def.setLevel(level);
        }
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
        return GzCompressAction.class.getSimpleName() + '[' + source + " to " + destination + ", deleteSource="
                + deleteSource + ']';
    }

    public File getSource() {
        return source;
    }

    public File getDestination() {
        return destination;
    }

    public boolean isDeleteSource() {
        return deleteSource;
    }
}
