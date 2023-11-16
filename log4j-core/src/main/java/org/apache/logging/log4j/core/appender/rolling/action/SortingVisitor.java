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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * FileVisitor that sorts files.
 */
public class SortingVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private final PathSorter sorter;
    private final List<PathWithAttributes> collected = new ArrayList<>();

    /**
     * Constructs a new SortingVisitor.
     *
     * @param sorter Interface implementation which can sort paths.
     */
    public SortingVisitor(final PathSorter sorter) {
        this.sorter = Objects.requireNonNull(sorter, "sorter");
    }

    @Override
    public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) throws IOException {
        collected.add(new PathWithAttributes(path, attrs));
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

    public List<PathWithAttributes> getSortedPaths() {
        Collections.sort(collected, sorter);
        return collected;
    }
}
