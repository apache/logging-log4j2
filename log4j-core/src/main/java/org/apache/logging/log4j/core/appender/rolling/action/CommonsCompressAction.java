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
import java.nio.file.Files;
import java.util.Objects;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Compresses a file using bzip2 compression.
 */
public final class CommonsCompressAction extends AbstractAction {

    private static final int BUF_SIZE = 8192;

    /**
     * Compressor name. One of "gz", "bzip2", "xz", "zst", "pack200" or "deflate".
     */
    private final String name;

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
     * Creates new instance of Bzip2CompressAction.
     *
     * @param name the compressor name. One of "gz", "bzip2", "xz", "zst", "pack200", or "deflate".
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion. Failure to delete does not cause an exception
     *            to be thrown or affect return value.
     */
    public CommonsCompressAction(
            final String name, final File source, final File destination, final boolean deleteSource) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        this.name = name;
        this.source = source;
        this.destination = destination;
        this.deleteSource = deleteSource;
    }

    /**
     * Compresses.
     *
     * @return true if successfully compressed.
     * @throws IOException on IO exception.
     */
    @Override
    public boolean execute() throws IOException {
        return execute(name, source, destination, deleteSource);
    }

    /**
     * Compresses a file.
     *
     * @param name the compressor name, i.e. "gz", "bzip2", "xz", "zstd", "pack200", or "deflate".
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion. Failure to delete does not cause an exception
     *            to be thrown or affect return value.
     *
     * @return true if source file compressed.
     * @throws IOException on IO exception.
     */
    public static boolean execute(
            final String name, final File source, final File destination, final boolean deleteSource)
            throws IOException {
        if (!source.exists()) {
            return false;
        }
        LOGGER.debug("Starting {} compression of {}", name, source.getPath());
        try (final FileInputStream input = new FileInputStream(source);
                final FileOutputStream fileOutput = new FileOutputStream(destination);
                final BufferedOutputStream output = new BufferedOutputStream(
                        new CompressorStreamFactory().createCompressorOutputStream(name, fileOutput))) {
            IOUtils.copy(input, output, BUF_SIZE);
            LOGGER.debug("Finished {} compression of {}", name, source.getPath());
        } catch (final CompressorException e) {
            throw new IOException(e);
        }

        if (deleteSource) {
            try {
                if (Files.deleteIfExists(source.toPath())) {
                    LOGGER.debug("Deleted {}", source.toString());
                } else {
                    LOGGER.warn(
                            "Unable to delete {} after {} compression. File did not exist", source.toString(), name);
                }
            } catch (final Exception ex) {
                LOGGER.warn("Unable to delete {} after {} compression, {}", source.toString(), name, ex.getMessage());
            }
        }

        return true;
    }

    /**
     * Reports exception.
     *
     * @param ex exception.
     */
    @Override
    protected void reportException(final Exception ex) {
        LOGGER.warn("Exception during " + name + " compression of '" + source.toString() + "'.", ex);
    }

    @Override
    public String toString() {
        return CommonsCompressAction.class.getSimpleName() + '[' + source + " to " + destination + ", deleteSource="
                + deleteSource + ']';
    }

    public String getName() {
        return name;
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
