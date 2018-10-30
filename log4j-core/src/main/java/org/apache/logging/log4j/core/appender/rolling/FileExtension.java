/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

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
        Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                                    final int compressionLevel) {
            return new ZipCompressAction(source(renameTo), target(compressedName), deleteSource, compressionLevel);
        }
    },
    GZ(".gz") {
        @Override
        Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                                    final int compressionLevel) {
            return new GzCompressAction(source(renameTo), target(compressedName), deleteSource);
        }
    },
    BZIP2(".bz2") {
        @Override
        Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                                    final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "pack200", or "deflate".
            return new CommonsCompressAction("bzip2", source(renameTo), target(compressedName), deleteSource);
        }
    },
    DEFLATE(".deflate") {
        @Override
        Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                                    final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "pack200", or "deflate".
            return new CommonsCompressAction("deflate", source(renameTo), target(compressedName), deleteSource);
        }
    },
    PACK200(".pack200") {
        @Override
        Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                                    final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "pack200", or "deflate".
            return new CommonsCompressAction("pack200", source(renameTo), target(compressedName), deleteSource);
        }
    },
    XZ(".xz") {
        @Override
        Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                                    final int compressionLevel) {
            // One of "gz", "bzip2", "xz", "pack200", or "deflate".
            return new CommonsCompressAction("xz", source(renameTo), target(compressedName), deleteSource);
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

    private FileExtension(final String extension) {
        Objects.requireNonNull(extension, "extension");
        this.extension = extension;
    }

    abstract Action createCompressAction(String renameTo, String compressedName, boolean deleteSource,
                                         int compressionLevel);

    String getExtension() {
        return extension;
    }

    boolean isExtensionFor(final String s) {
        return s.endsWith(this.extension);
    }

    int length() {
        return extension.length();
    }

    File source(final String fileName) {
        return new File(fileName);
    }

    File target(final String fileName) {
        return new File(fileName);
    } 
}
