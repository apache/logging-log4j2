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
package org.apache.logging.log4j.compress.commons;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamProvider;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractCompressAction;

/**
 * Compresses a file using bzip2 compression.
 */
final class CommonsCompressAction extends AbstractCompressAction {

    private final CompressorStreamProvider provider;

    /**
     * Compressor name. One of "gz", "bzip2", "xz", "pack200" or "deflate".
     */
    private final String name;

    /**
     * Creates new instance of Bzip2CompressAction.
     *
     * @param name The compressor name. One of "gz", "bzip2", "xz", "pack200", or "deflate".
     * @param source The file to compress, may not be null.
     * @param destination The compressed file, may not be null.
     */
    CommonsCompressAction(
            final CompressorStreamProvider provider, final String name, final Path source, final Path destination) {
        super(source, destination);
        this.provider = Objects.requireNonNull(provider);
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    protected OutputStream wrapOutputStream(OutputStream stream) throws IOException {
        try {
            return new BufferedOutputStream(provider.createCompressorOutputStream(name, stream), BUF_SIZE);
        } catch (final CompressorException error) {
            final String message = String.format("failed to wrap the output stream with the `%s` compressor", name);
            throw new IOException(message, error);
        }
    }

    @Override
    protected String getAlgorithmName() {
        return name;
    }
}
