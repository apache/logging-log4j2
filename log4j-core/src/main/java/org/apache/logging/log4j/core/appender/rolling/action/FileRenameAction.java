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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * File rename action.
 */
public class FileRenameAction extends AbstractAction {

    /**
     * Source.
     */
    private final File source;

    /**
     * Destination.
     */
    private final File destination;

    /**
     * If true, rename empty files, otherwise delete empty files.
     */
    private final boolean renameEmptyFiles;

    /**
     * Creates an FileRenameAction.
     *
     * @param src current file name.
     * @param dst new file name.
     * @param renameEmptyFiles if true, rename file even if empty, otherwise delete empty files.
     */
    public FileRenameAction(final File src, final File dst, final boolean renameEmptyFiles) {
        source = src;
        destination = dst;
        this.renameEmptyFiles = renameEmptyFiles;
    }

    /**
     * Rename file.
     *
     * @return true if successfully renamed.
     */
    @Override
    public boolean execute() {
        return execute(source, destination, renameEmptyFiles);
    }

    /**
     * Gets the destination.
     *
     * @return the destination.
     */
    public File getDestination() {
        return this.destination;
    }

    /**
     * Gets the source.
     *
     * @return the source.
     */
    public File getSource() {
        return this.source;
    }

    /**
     * Whether to rename empty files. If true, rename empty files, otherwise delete empty files.
     *
     * @return Whether to rename empty files.
     */
    public boolean isRenameEmptyFiles() {
        return renameEmptyFiles;
    }

    /**
     * Rename file.
     *
     * @param source current file name.
     * @param destination new file name.
     * @param renameEmptyFiles if true, rename file even if empty, otherwise delete empty files.
     * @return true if successfully renamed.
     */
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The name of the accessed files is based on a configuration value.")
    public static boolean execute(final File source, final File destination, final boolean renameEmptyFiles) {
        if (renameEmptyFiles || (source.length() > 0)) {
            final File parent = destination.getParentFile();
            if ((parent != null) && !parent.exists()) {
                // LOG4J2-679: ignore mkdirs() result: in multithreaded scenarios,
                // if one thread succeeds the other thread returns false
                // even though directories have been created. Check if dir exists instead.
                parent.mkdirs();
                if (!parent.exists()) {
                    LOGGER.error("Unable to create directory {}", parent.getAbsolutePath());
                    return false;
                }
            }
            try {
                try {
                    return moveFile(Paths.get(source.getAbsolutePath()), Paths.get(destination.getAbsolutePath()));
                } catch (final IOException exMove) {
                    LOGGER.debug(
                            "Unable to move file {} to {}: {} {} - will try to copy and delete",
                            source.getAbsolutePath(),
                            destination.getAbsolutePath(),
                            exMove.getClass().getName(),
                            exMove.getMessage());
                    boolean result = source.renameTo(destination);
                    if (!result) {
                        try {
                            Files.copy(
                                    Paths.get(source.getAbsolutePath()),
                                    Paths.get(destination.getAbsolutePath()),
                                    StandardCopyOption.REPLACE_EXISTING);
                            try {
                                Files.delete(Paths.get(source.getAbsolutePath()));
                                result = true;
                                LOGGER.trace(
                                        "Renamed file {} to {} using copy and delete",
                                        source.getAbsolutePath(),
                                        destination.getAbsolutePath());
                            } catch (final IOException exDelete) {
                                LOGGER.error(
                                        "Unable to delete file {}: {} {}",
                                        source.getAbsolutePath(),
                                        exDelete.getClass().getName(),
                                        exDelete.getMessage());
                                try {
                                    result = true;
                                    new PrintWriter(source.getAbsolutePath()).close();
                                    LOGGER.trace(
                                            "Renamed file {} to {} with copy and truncation",
                                            source.getAbsolutePath(),
                                            destination.getAbsolutePath());
                                } catch (final IOException exOwerwrite) {
                                    LOGGER.error(
                                            "Unable to overwrite file {}: {} {}",
                                            source.getAbsolutePath(),
                                            exOwerwrite.getClass().getName(),
                                            exOwerwrite.getMessage());
                                }
                            }
                        } catch (final IOException exCopy) {
                            LOGGER.error(
                                    "Unable to copy file {} to {}: {} {}",
                                    source.getAbsolutePath(),
                                    destination.getAbsolutePath(),
                                    exCopy.getClass().getName(),
                                    exCopy.getMessage());
                        }
                    } else {
                        LOGGER.trace(
                                "Renamed file {} to {} with source.renameTo",
                                source.getAbsolutePath(),
                                destination.getAbsolutePath());
                    }
                    return result;
                }
            } catch (final RuntimeException ex) {
                LOGGER.error(
                        "Unable to rename file {} to {}: {} {}",
                        source.getAbsolutePath(),
                        destination.getAbsolutePath(),
                        ex.getClass().getName(),
                        ex.getMessage());
            }
        } else {
            try {
                return source.delete();
            } catch (final Exception exDelete) {
                LOGGER.error(
                        "Unable to delete empty file {}: {} {}",
                        source.getAbsolutePath(),
                        exDelete.getClass().getName(),
                        exDelete.getMessage());
            }
        }

        return false;
    }

    private static boolean moveFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.trace(
                    "Renamed file {} to {} with Files.move",
                    source.toFile().getAbsolutePath(),
                    target.toFile().getAbsolutePath());
            return true;
        } catch (final AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.trace(
                    "Renamed file {} to {} with Files.move",
                    source.toFile().getAbsolutePath(),
                    target.toFile().getAbsolutePath());
            return true;
        }
    }

    @Override
    public String toString() {
        return FileRenameAction.class.getSimpleName() + '[' + source + " to " + destination + ", renameEmptyFiles="
                + renameEmptyFiles + ']';
    }
}
