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
