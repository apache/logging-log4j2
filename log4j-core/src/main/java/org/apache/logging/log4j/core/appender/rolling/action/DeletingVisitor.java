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

package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
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
    private final List<? extends PathCondition> pathConditions;

    /**
     * Constructs a new DeletingVisitor.
     * 
     * @param basePath used to relativize paths
     * @param pathConditions objects that need to confirm whether a file can be deleted
     */
    public DeletingVisitor(final Path basePath, final List<? extends PathCondition> pathConditions) {
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
        delete(file);
        return FileVisitResult.CONTINUE;
    }

    /**
     * Deletes the specified file.
     * 
     * @param file the file to delete
     * @throws IOException if a problem occurred deleting the file
     */
    protected void delete(final Path file) throws IOException {
        LOGGER.trace("Deleting {}", file);
        Files.delete(file);
    }
}
