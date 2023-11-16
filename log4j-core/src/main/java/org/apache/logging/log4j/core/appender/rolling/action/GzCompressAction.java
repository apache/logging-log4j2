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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

/**
 * Compresses a file using GZ compression.
 */
public final class GzCompressAction extends AbstractAction {

    private static final int BUF_SIZE = 8192;

    /**
     * Source file.
     */
    private final File source;

    /**
     * Destination file.
     */
    private final File destination;

    /**
     * If true, attempt to delete file on completion.
     */
    private final boolean deleteSource;

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
     * @param deleteSource if true, attempt to delete file on completion.  Failure to delete
     *                     does not cause an exception to be thrown or affect return value.
     * @param compressionLevel
     *                     Gzip deflater compression level.
     */
    public GzCompressAction(
            final File source, final File destination, final boolean deleteSource, final int compressionLevel) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");

        this.source = source;
        this.destination = destination;
        this.deleteSource = deleteSource;
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
        return execute(source, destination, deleteSource, compressionLevel);
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
        if (source.exists()) {
            try (final FileInputStream fis = new FileInputStream(source);
                    final OutputStream fos = new FileOutputStream(destination);
                    final OutputStream gzipOut =
                            new ConfigurableLevelGZIPOutputStream(fos, BUF_SIZE, compressionLevel);
                    // Reduce native invocations by buffering data into GZIPOutputStream
                    final OutputStream os = new BufferedOutputStream(gzipOut, BUF_SIZE)) {
                final byte[] inbuf = new byte[BUF_SIZE];
                int n;

                while ((n = fis.read(inbuf)) != -1) {
                    os.write(inbuf, 0, n);
                }
            }

            if (deleteSource && !source.delete()) {
                LOGGER.warn("Unable to delete {}.", source);
            }

            return true;
        }

        return false;
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
