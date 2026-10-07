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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.logging.log4j.status.StatusLogger;

/** Common file handling for compression actions. */
public final class CompressActionSupport {

    private static final int BUF_SIZE = 8192;

    private CompressActionSupport() {}

    /**
     * Compresses a source file while retaining it.
     *
     * @param source source file
     * @param destination destination file
     * @param compressorOutputStreamFactory factory for the compressor output stream
     * @return {@code true} if the source file was compressed
     * @throws IOException if an I/O error occurs
     */
    public static boolean execute(
            final File source,
            final File destination,
            final CompressorOutputStreamFactory compressorOutputStreamFactory)
            throws IOException {
        return execute(source, destination, false, compressorOutputStreamFactory);
    }

    /**
     * Compresses a source file.
     *
     * @param source source file
     * @param destination destination file
     * @param deleteSource whether to delete the source file after successful compression
     * @param compressorOutputStreamFactory factory for the compressor output stream
     * @return {@code true} if the source file was compressed
     * @throws IOException if an I/O error occurs
     */
    public static boolean execute(
            final File source,
            final File destination,
            final boolean deleteSource,
            final CompressorOutputStreamFactory compressorOutputStreamFactory)
            throws IOException {
        if (!source.exists()) {
            return false;
        }
        try (final FileInputStream input = new FileInputStream(source);
                final FileOutputStream fileOutput = new FileOutputStream(destination);
                final OutputStream compressorOutput = compressorOutputStreamFactory.create(fileOutput);
                final OutputStream output = new BufferedOutputStream(compressorOutput, BUF_SIZE)) {
            final byte[] buffer = new byte[BUF_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
        if (deleteSource && !source.delete()) {
            StatusLogger.getLogger().warn("Unable to delete {}.", source);
        }
        return true;
    }

    @FunctionalInterface
    public interface CompressorOutputStreamFactory {

        /**
         * Creates a compressor output stream.
         *
         * @param output destination output stream
         * @return compressor output stream
         * @throws IOException if an I/O error occurs
         */
        OutputStream create(OutputStream output) throws IOException;
    }
}
