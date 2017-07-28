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
package org.apache.logging.log4j.junit;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.Logger;

/**
 * A JUnit test rule to automatically delete folders recursively before (optional) and after (optional) a test is run.
 */
public class CleanFolders extends AbstractExternalFileCleaner {

    public static final class DeleteAllFileVisitor extends SimpleFileVisitor<Path> {

        private final Logger logger;

        public DeleteAllFileVisitor(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            if (logger != null) {
                logger.debug(CLEANER_MARKER, "Deleting directory {}", dir);
            }
            final boolean deleted = Files.deleteIfExists(dir);
            if (logger != null) {
                logger.debug(CLEANER_MARKER, "Deleted directory {}: {}", dir, deleted);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            if (logger != null) {
                logger.debug(CLEANER_MARKER, "Deleting file {} with {}", file, attrs);
            }
            final boolean deleted = Files.deleteIfExists(file);
            if (logger != null) {
                logger.debug(CLEANER_MARKER, "Deleted file {}: {}", file, deleted);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private static final int MAX_TRIES = 10;

    public CleanFolders(final boolean before, final boolean after, final int maxTries, final File... files) {
        super(before, after, maxTries, null, files);
    }

    public CleanFolders(final boolean before, final boolean after, final int maxTries, final String... fileNames) {
        super(before, after, maxTries, null, fileNames);
    }

    public CleanFolders(final File... folders) {
        super(true, true, MAX_TRIES, null, folders);
    }

    public CleanFolders(final Logger logger, final File... folders) {
        super(true, true, MAX_TRIES, logger, folders);
    }

    public CleanFolders(final Path... paths) {
        super(true, true, MAX_TRIES, null, paths);
    }

    public CleanFolders(final String... folderNames) {
        super(true, true, MAX_TRIES, null, folderNames);
    }

    private void cleanFolder(final Path folder, final int tryIndex) throws IOException {
        if (Files.exists(folder) && Files.isDirectory(folder)) {
            Files.walkFileTree(folder, new DeleteAllFileVisitor(getLogger()));
        }
    }

    @Override
    protected boolean clean(final Path path, final int tryIndex) throws IOException {
        cleanFolder(path, tryIndex);
        return true;
    }
}
