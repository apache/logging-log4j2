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
package org.apache.logging.log4j.core.appender.rolling;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.Objects;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CommonsCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.GzCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.ZipCompressAction;

/**
 *  Enumerates over supported file extensions for compression.
 */
public enum FileExtension {
    ZIP(".zip") {
        @Override
        public Action createCompressAction(
                final String renameTo,
                final String compressedName,
                final boolean deleteSource,
                final int compressionLevel) {
            return new ZipCompressAction(source(renameTo), target(compressedName), deleteSource, compressionLevel);
        }
    },
    GZ(".gz") {
        @Override
        public Action createCompressAction(
                final String renameTo,
                final String compressedName,
                final boolean deleteSource,
                final int compressionLevel) {
            return new GzCompressAction(source(renameTo), target(compressedName), deleteSource, compressionLevel);
        }
    },
    BZIP2(".bz2") {
        @Override
        public Action createCompressAction(
                final String renameTo,
                final String compressedName,
                final boolean deleteSource,
                final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "zst", "pack200", or "deflate".
            return new CommonsCompressAction("bzip2", source(renameTo), target(compressedName), deleteSource);
        }
    },
    DEFLATE(".deflate") {
        @Override
        public Action createCompressAction(
                final String renameTo,
                final String compressedName,
                final boolean deleteSource,
                final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "zst", "pack200", or "deflate".
            return new CommonsCompressAction("deflate", source(renameTo), target(compressedName), deleteSource);
        }
    },
    PACK200(".pack200") {
        @Override
        public Action createCompressAction(
                final String renameTo,
                final String compressedName,
                final boolean deleteSource,
                final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "zst", "pack200", or "deflate".
            return new CommonsCompressAction("pack200", source(renameTo), target(compressedName), deleteSource);
        }
    },
    XZ(".xz") {
        @Override
        public Action createCompressAction(
                final String renameTo,
                final String compressedName,
                final boolean deleteSource,
                final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "zstd", "pack200", or "deflate".
            return new CommonsCompressAction("xz", source(renameTo), target(compressedName), deleteSource);
        }
    },
    ZSTD(".zst") {
        @Override
        public Action createCompressAction(
                final String renameTo,
                final String compressedName,
                final boolean deleteSource,
                final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "zstd", "pack200", or "deflate".
            return new CommonsCompressAction("zstd", source(renameTo), target(compressedName), deleteSource);
        }
    };

    public static FileExtension lookup(final String fileExtension) {
        for (final FileExtension ext : values()) {
            if (ext.isExtensionFor(fileExtension)) {
                return ext;
            }
        }
        return null;
    }

    public static FileExtension lookupForFile(final String fileName) {
        for (final FileExtension ext : values()) {
            if (fileName.endsWith(ext.extension)) {
                return ext;
            }
        }
        return null;
    }

    private final String extension;

    FileExtension(final String extension) {
        Objects.requireNonNull(extension, "extension");
        this.extension = extension;
    }

    public abstract Action createCompressAction(
            String renameTo, String compressedName, boolean deleteSource, int compressionLevel);

    public String getExtension() {
        return extension;
    }

    boolean isExtensionFor(final String s) {
        return s.endsWith(this.extension);
    }

    int length() {
        return extension.length();
    }

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The name of the accessed files is based on a configuration value.")
    File source(final String fileName) {
        return new File(fileName);
    }

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The name of the accessed files is based on a configuration value.")
    File target(final String fileName) {
        return new File(fileName);
    }
}
