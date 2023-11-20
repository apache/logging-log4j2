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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * FileVisitor that deletes files that are accepted by all PathFilters. Directories are ignored.
 */
public class DeletingVisitor extends SimpleFileVisitor<Path> {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Path basePath;
    private final boolean testMode;
    private final List<? extends PathCondition> pathConditions;

    /**
     * Constructs a new DeletingVisitor.
     *
     * @param basePath used to relativize paths
     * @param pathConditions objects that need to confirm whether a file can be deleted
     * @param testMode if true, files are not deleted but instead a message is printed to the <a
     *            href="http://logging.apache.org/log4j/2.x/manual/configuration.html#StatusMessages">status logger</a>
     *            at INFO level. Users can use this to do a dry run to test if their configuration works as expected.
     */
    public DeletingVisitor(
            final Path basePath, final List<? extends PathCondition> pathConditions, final boolean testMode) {
        this.testMode = testMode;
        this.basePath = Objects.requireNonNull(basePath, "basePath");
        this.pathConditions = Objects.requireNonNull(pathConditions, "pathConditions");
        for (final PathCondition condition : pathConditions) {
            condition.beforeFileTreeWalk();
        }
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        for (final PathCondition pathFilter : pathConditions) {
            final Path relative = basePath.relativize(file);
            if (!pathFilter.accept(basePath, relative, attrs)) {
                LOGGER.trace("Not deleting base={}, relative={}", basePath, relative);
                return FileVisitResult.CONTINUE;
            }
        }
        if (isTestMode()) {
            LOGGER.info("Deleting {} (TEST MODE: file not actually deleted)", file);
        } else {
            delete(file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException ioException) throws IOException {
        // LOG4J2-2677: Appenders may rollover and purge in parallel. SimpleVisitor rethrows exceptions from
        // failed attempts to load file attributes.
        if (ioException instanceof NoSuchFileException) {
            LOGGER.info("File {} could not be accessed, it has likely already been deleted", file, ioException);
            return FileVisitResult.CONTINUE;
        } else {
            return super.visitFileFailed(file, ioException);
        }
    }

    /**
     * Deletes the specified file.
     *
     * @param file the file to delete
     * @throws IOException if a problem occurred deleting the file
     */
    protected void delete(final Path file) throws IOException {
        LOGGER.trace("Deleting {}", file);
        Files.deleteIfExists(file);
    }

    /**
     * Returns {@code true} if files are not deleted even when all conditions accept a path, {@code false} otherwise.
     *
     * @return {@code true} if files are not deleted even when all conditions accept a path, {@code false} otherwise
     */
    public boolean isTestMode() {
        return testMode;
    }
}
