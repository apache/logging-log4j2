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
package org.apache.logging.log4j.core.test.junit;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A JUnit test rule to automatically delete folders recursively before
 * (optional) and after (optional) a test is run.
 * <p>
 * This class should not perform logging using Log4j to avoid accidentally
 * loading or re-loading Log4j configurations.
 * </p>
 */
public class CleanFolders extends AbstractExternalFileCleaner {

    public static final class DeleteAllFileVisitor extends SimpleFileVisitor<Path> {

        private final PrintStream printStream;

        public DeleteAllFileVisitor(final PrintStream logger) {
            this.printStream = logger;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            printf("%s Deleting directory %s\n", CLEANER_MARKER, dir);
            final boolean deleted = Files.deleteIfExists(dir);
            printf("%s Deleted directory %s: %s\n", CLEANER_MARKER, dir, deleted);
            return FileVisitResult.CONTINUE;
        }

        protected void printf(final String format, final Object... args) {
            if (printStream != null) {
                printStream.printf(format, args);
            }
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            printf("%s Deleting file %s with %s\n", CLEANER_MARKER, file, attrs);
            final boolean deleted = Files.deleteIfExists(file);
            printf("%s Deleted file %s: %s\n", CLEANER_MARKER, file, deleted);
            return FileVisitResult.CONTINUE;
        }
    }

    private static final int MAX_TRIES = 10;

    public CleanFolders(final boolean before, final boolean after, final int maxTries, final File... files) {
        super(before, after, maxTries, null, files);
    }

    public CleanFolders(final boolean before, final boolean after, final int maxTries, final Path... paths) {
        super(before, after, maxTries, null, paths);
    }

    public CleanFolders(final boolean before, final boolean after, final int maxTries, final String... fileNames) {
        super(before, after, maxTries, null, fileNames);
    }

    public CleanFolders(final File... folders) {
        super(true, true, MAX_TRIES, null, folders);
    }

    public CleanFolders(final Path... paths) {
        super(true, true, MAX_TRIES, null, paths);
    }

    public CleanFolders(final PrintStream logger, final File... folders) {
        super(true, true, MAX_TRIES, logger, folders);
    }

    public CleanFolders(final String... folderNames) {
        super(true, true, MAX_TRIES, null, folderNames);
    }

    @Override
    protected boolean clean(final Path path, final int tryIndex) throws IOException {
        cleanFolder(path, tryIndex);
        return true;
    }

    private void cleanFolder(final Path folder, final int tryIndex) throws IOException {
        if (Files.exists(folder) && Files.isDirectory(folder)) {
            Files.walkFileTree(folder, new DeleteAllFileVisitor(getPrintStream()));
        }
    }
}
