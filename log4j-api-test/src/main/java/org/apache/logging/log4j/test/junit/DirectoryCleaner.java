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
package org.apache.logging.log4j.test.junit;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import org.junit.jupiter.api.extension.ExtensionContext;

class DirectoryCleaner extends AbstractFileCleaner {
    @Override
    Collection<Path> getPathsForTest(final ExtensionContext context) {
        final Collection<Path> paths = new HashSet<>();
        final CleanUpDirectories testClassAnnotation =
                context.getRequiredTestClass().getAnnotation(CleanUpDirectories.class);
        if (testClassAnnotation != null) {
            for (final String path : testClassAnnotation.value()) {
                paths.add(Paths.get(path));
            }
        }
        final CleanUpDirectories testMethodAnnotation =
                context.getRequiredTestMethod().getAnnotation(CleanUpDirectories.class);
        if (testMethodAnnotation != null) {
            for (final String path : testMethodAnnotation.value()) {
                paths.add(Paths.get(path));
            }
        }
        return paths;
    }

    @Override
    boolean delete(final Path path) throws IOException {
        return deleteDirectory(path);
    }

    static boolean deleteDirectory(final Path path) throws IOException {
        if (Files.exists(path) && Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return true;
    }
}
