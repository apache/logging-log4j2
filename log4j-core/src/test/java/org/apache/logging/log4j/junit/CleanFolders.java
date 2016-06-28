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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.ExternalResource;

/**
 * A JUnit test rule to automatically delete folders before and after a test is run.
 */
public class CleanFolders extends ExternalResource {
    private static final int MAX_TRIES = 10;
    private final List<File> folders;

    public CleanFolders(final File... files) {
        this.folders = Arrays.asList(files);
    }

    public CleanFolders(final String... fileNames) {
        this.folders = new ArrayList<>(fileNames.length);
        for (final String fileName : fileNames) {
            this.folders.add(new File(fileName));
        }
    }

    @Override
    protected void before() {
        this.clean();
    }

    @Override
    protected void after() {
        this.clean();
    }

    private void clean() {
        for (final File folder : folders) {
            for (int i = 0; i < MAX_TRIES; i++) {
                final Path targetPath = folder.toPath();
                if (Files.exists(targetPath)) {
                    String fileName = null;
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(targetPath)) {
                        for (final Path path : directoryStream) {
                            fileName = path.toFile().getName();
                            Files.deleteIfExists(path);
                        }
                        Files.deleteIfExists(targetPath);
                    } catch (final IOException e) {
                        throw new IllegalStateException(fileName, e);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CleanFolders [");
        builder.append(folders);
        builder.append("]");
        return builder.toString();
    }
}
