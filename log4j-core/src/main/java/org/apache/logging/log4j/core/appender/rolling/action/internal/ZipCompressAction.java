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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractCompressAction;

/**
 * Compresses a file using Zip compression.
 */
public final class ZipCompressAction extends AbstractCompressAction {

    /**
     * The file being compressed.
     */
    private final String fileName;

    /**
     * Compression level.
     */
    private final int compressionLevel;

    /**
     * Creates new instance of ZipCompressAction.
     *
     * @param source file to compress, may not be null.
     * @param destination compressed file, may not be null.
     * @param compressionLevel
     *                     ZIP deflater compression level
     */
    public ZipCompressAction(final Path source, final Path destination, final int compressionLevel) {
        super(source, destination);
        this.fileName = source.getFileName().toString();
        this.compressionLevel = compressionLevel;
    }

    @Override
    protected OutputStream wrapOutputStream(OutputStream stream) throws IOException {
        final ZipOutputStream zos = new ZipOutputStream(stream);
        zos.setLevel(compressionLevel);
        final ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        return zos;
    }

    @Override
    protected String getAlgorithmName() {
        return "ZIP";
    }
}
