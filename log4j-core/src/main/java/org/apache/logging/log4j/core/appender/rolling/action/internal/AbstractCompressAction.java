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
import java.util.Objects;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;

/**
 * Base class for actions that compress a file and optionally delete the source after successful compression.
 *
 * @since 2.27.0
 */
public abstract class AbstractCompressAction extends AbstractAction {

    /** Buffer size used when copying source data to the compressor. */
    protected static final int BUF_SIZE = 8192;

    /** Source file. */
    protected final File source;
    /** Destination file. */
    protected final File destination;
    /** If true, attempt to delete the source file after successful compression. */
    protected final boolean deleteSource;

    private final String compressionName;

    /**
     * Creates a compression action.
     *
     * @param source file to compress, must not be null
     * @param destination compressed file, must not be null
     * @param deleteSource whether to delete the source after successful compression
     * @param compressionName format name for debug messages, must not be null
     */
    protected AbstractCompressAction(
            final File source, final File destination, final boolean deleteSource, final String compressionName) {
        this.source = Objects.requireNonNull(source, "source");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.deleteSource = deleteSource;
        this.compressionName = Objects.requireNonNull(compressionName, "compressionName");
    }

    @Override
    public boolean execute() throws IOException {
        LOGGER.debug("Starting {} compression of {}", compressionName, source.getPath());
        final boolean compressed =
                CompressActionSupport.execute(source, destination, deleteSource, this::createCompressorOutputStream);
        if (compressed) {
            LOGGER.debug("Finished {} compression of {}", compressionName, source.getPath());
        }
        return compressed;
    }

    /**
     * Creates the compressor stream during execution, after the source and destination have been opened.
     * The action closes the returned stream; implementations must clean up resources if creation fails.
     *
     * @param output destination stream
     * @return compressor stream
     * @throws IOException if the compressor cannot be created
     */
    protected abstract OutputStream createCompressorOutputStream(OutputStream output) throws IOException;

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
