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
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

/**
 * A JUnit test rule to automatically delete folders recursively before (optional) and after (optional) a test is run.
 */
public class CleanFolders extends AbstractExternalFileCleaner {
    private static final int MAX_TRIES = 10;

    public CleanFolders(final boolean before, final boolean after, final int maxTries, final File... files) {
        super(before, after, maxTries, files);
    }

    public CleanFolders(final boolean before, final boolean after, final int maxTries, final String... fileNames) {
        super(before, after, maxTries, fileNames);
    }

    public CleanFolders(final File... folders) {
        super(true, true, MAX_TRIES, folders);
    }

    public CleanFolders(final String... folderNames) {
        super(true, true, MAX_TRIES, folderNames);
    }

    @Override
    protected void clean() {
        Map<Path, IOException> failures = new HashMap<>();
        // Clean and gather failures
        for (final File folder : getFiles()) {
            if (folder.exists()) {
                final Path path = folder.toPath();
                for (int i = 0; i < getMaxTries(); i++) {
                    try {
                        cleanFolder(path);
                        if (failures.containsKey(path)) {
                            failures.remove(path);
                        }
                        // break from MAX_TRIES and goes to the next folder
                        break;
                    } catch (final IOException e) {
                        // We will try again.
                        failures.put(path, e);
                    }
                }
            }
        }
        // Fail on failures
        if (failures.size() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<Path, IOException> failure : failures.entrySet()) {
                failure.getValue().printStackTrace();
                if (!first) {
                    sb.append(", ");
                }
                sb.append(failure.getKey()).append(" failed with ").append(failure.getValue());
                first = false;
            }
            Assert.fail(sb.toString());
        }
    }

    private void cleanFolder(final Path folder) throws IOException {
        if (Files.exists(folder) && Files.isDirectory(folder)) {
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
