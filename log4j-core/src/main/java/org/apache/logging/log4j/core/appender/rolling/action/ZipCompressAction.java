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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compresses a file using Zip compression.
 */
public final class ZipCompressAction extends AbstractAction {

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
     * If true, attempts to delete file on completion.
     */
    private final boolean deleteSource;

    /**
     * Compression level.
     */
    private final int level;

    /**
     * Maximum delay in seconds before compression.
     */
    private final int maxDelaySeconds;

    /**
     * Validates that the compression level is in the valid range [-1, 9].
     *
     * @param level the compression level to validate
     * @return the level if valid
     * @throws IllegalArgumentException if level is not in the range [-1, 9]
     */
    private static int checkLevel(final int level) {
        if (level < java.util.zip.Deflater.DEFAULT_COMPRESSION || level > 9) {
            throw new IllegalArgumentException("Compression level must be in the range [-1, 9], got: " + level);
        }
        return level;
    }

    /**
     * Creates new instance of GzCompressAction.
     *
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion. Failure to delete does not cause an exception
     *            to be thrown or affect return value.
     * @param level the compression level
     * @since2.26.0
     * @param maxDelaySeconds maximum delay in seconds before compression.
     */
    public ZipCompressAction(
            final File source,
            final File destination,
            final boolean deleteSource,
            final int level,
            final int maxDelaySeconds) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");

        this.source = source;
        this.destination = destination;
        this.deleteSource = deleteSource;
        this.level = checkLevel(level);
        this.maxDelaySeconds = maxDelaySeconds;
    }

    /**
     * Creates new instance.
     *
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion. Failure to delete does not cause an exception
     *            to be thrown or affect return value.
     * @param level the compression level
     */
    public ZipCompressAction(final File source, final File destination, final boolean deleteSource, final int level) {
        this(source, destination, deleteSource, level, 0);
    }

    /**
     * Compresses.
     *
     * @return true if successfully compressed.
     * @throws IOException on IO exception.
     */
    @Override
    public boolean execute() throws IOException {
        if (maxDelaySeconds > 0) {
            int delay = java.util.concurrent.ThreadLocalRandom.current().nextInt(maxDelaySeconds + 1);
            if (delay > 0) {
                try {
                    Thread.sleep(delay * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return execute(source, destination, deleteSource, level);
    }

    /**
     * Compresses a file.
     *
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion. Failure to delete does not cause an exception
     *            to be thrown or affect return value.
     * @param level the compression level
     * @return true if source file compressed.
     * @throws IOException on IO exception.
     */
    public static boolean execute(
            final File source, final File destination, final boolean deleteSource, final int level) throws IOException {
        if (source.exists()) {
            try (final FileInputStream fis = new FileInputStream(source);
                    final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destination))) {
                zos.setLevel(level);

                final ZipEntry zipEntry = new ZipEntry(source.getName());
                zos.putNextEntry(zipEntry);

                final byte[] inbuf = new byte[BUF_SIZE];
                int n;

                while ((n = fis.read(inbuf)) != -1) {
                    zos.write(inbuf, 0, n);
                }
            }

            if (deleteSource && !source.delete()) {
                LOGGER.warn("Unable to delete " + source.toString() + '.');
            }

            return true;
        }

        return false;
    }

    /**
     * Captures exception.
     *
     * @param ex exception.
     */
    @Override
    protected void reportException(final Exception ex) {
        LOGGER.warn("Exception during compression of '" + source.toString() + "'.", ex);
    }

    @Override
    public String toString() {
        return ZipCompressAction.class.getSimpleName() + '[' + source + " to " + destination + ", level=" + level
                + ", deleteSource=" + deleteSource + ']';
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

    public int getLevel() {
        return level;
    }
}
