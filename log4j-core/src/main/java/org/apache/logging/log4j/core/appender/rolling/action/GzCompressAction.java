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
import java.util.zip.Deflater;

/**
 * Compresses a file using GZ compression.
 *
 * @deprecated Since 2.27.0 this class only delegates to an internal implementation and is no longer used by Log4j
 *             itself. Configure compression through the {@code compressionLevel} attribute of a rollover strategy
 *             instead.
 */
@Deprecated
public final class GzCompressAction extends AbstractAction {

    private final org.apache.logging.log4j.core.appender.rolling.action.internal.GzCompressAction delegate;

    /**
     * Creates a new instance.
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param deleteSource if true, attempt to delete file on completion.
     * @param compressionLevel Gzip deflater compression level.
     */
    public GzCompressAction(
            final File source, final File destination, final boolean deleteSource, final int compressionLevel) {
        this.delegate = new org.apache.logging.log4j.core.appender.rolling.action.internal.GzCompressAction(
                source, destination, deleteSource, compressionLevel);
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
        return delegate.execute();
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
        return org.apache.logging.log4j.core.appender.rolling.action.internal.GzCompressAction.execute(
                source, destination, deleteSource, compressionLevel);
    }

    /**
     * Capture exception.
     *
     * @param ex exception.
     */
    @Override
    protected void reportException(final Exception ex) {
        LOGGER.warn("Exception during compression of '" + delegate.getSource() + "'.", ex);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public File getSource() {
        return delegate.getSource();
    }

    public File getDestination() {
        return delegate.getDestination();
    }

    public boolean isDeleteSource() {
        return delegate.isDeleteSource();
    }
}
