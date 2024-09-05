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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract class AbstractCompressAction extends AbstractAction {

    protected static final int BUF_SIZE = 8192;

    /**
     * Source file.
     */
    private final Path source;

    /**
     * Destination file.
     */
    private final Path destination;

    public AbstractCompressAction(final Path source, final Path destination) {
        this.source = Objects.requireNonNull(source, "source");
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    /**
     * The name of this compression algorithm.
     * <p>
     *     When applicable, it should correspond to the name used by Apache Commons Compress.
     * </p>
     */
    protected abstract String getAlgorithmName();

    /**
     * Wraps an output stream into a compressing output stream.
     *
     * @param stream The stream to wrap.
     * @return A compressing output stream.
     */
    protected abstract OutputStream wrapOutputStream(final OutputStream stream) throws IOException;

    @Override
    public boolean execute() throws IOException {
        if (Files.exists(source)) {
            LOGGER.debug("Starting {} compression from {} to {}.", getAlgorithmName(), source, destination);
            try (final OutputStream output = wrapOutputStream(Files.newOutputStream(destination))) {
                Files.copy(source, output);
            }
            LOGGER.debug("Finished {} compression from {} to {}.", getAlgorithmName(), source, destination);
            try {
                Files.delete(source);
                LOGGER.debug("File {} deleted successfully.", source);
            } catch (final IOException ioe) {
                LOGGER.warn("Unable to delete {}.", source, ioe);
            }
            return true;
        }
        return false;
    }

    /**
     * Capture exception.
     *
     * @param ex exception.
     */
    @Override
    protected void reportException(final Exception ex) {
        LOGGER.warn("Exception during compression of '{}'.", source, ex);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + source + " to " + destination + ']';
    }
}
