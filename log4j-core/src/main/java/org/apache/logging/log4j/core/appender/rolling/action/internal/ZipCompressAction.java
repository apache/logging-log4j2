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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compresses a file using Zip compression.
 *
 * @since 2.27.0
 */
public final class ZipCompressAction extends AbstractCompressAction {

    /**
     * Compression level.
     */
    private final int level;

    private static int resolveCompressionLevel(final int level) {
        return CompressActionSupport.resolveCompressionLevel("ZIP", level, -1, 9, -1);
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
        super(source, destination, deleteSource, "zip");
        this.level = level;
    }

    @Override
    protected OutputStream createCompressorOutputStream(final OutputStream output) throws IOException {
        return createCompressorOutputStream(output, source, level);
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
        return CompressActionSupport.execute(
                source, destination, deleteSource, output -> createCompressorOutputStream(output, source, level));
    }

    private static OutputStream createCompressorOutputStream(
            final OutputStream output, final File source, final int level) throws IOException {
        final ZipOutputStream zipOutput = new ZipOutputStream(output);
        try {
            zipOutput.setLevel(resolveCompressionLevel(level));
            zipOutput.putNextEntry(new ZipEntry(source.getName()));
            return zipOutput;
        } catch (final IOException | RuntimeException | Error failure) {
            try {
                zipOutput.close();
            } catch (final IOException closeFailure) {
                if (closeFailure != failure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            throw failure;
        }
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

    public int getLevel() {
        return level;
    }
}
