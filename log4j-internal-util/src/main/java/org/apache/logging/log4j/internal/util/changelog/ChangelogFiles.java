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
package org.apache.logging.log4j.internal.util.changelog;

import org.apache.logging.log4j.internal.util.PomUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public final class ChangelogFiles {

    private ChangelogFiles() {}

    public static Path changelogDirectory(final Path projectRootDirectory) {
        return projectRootDirectory.resolve("src/changelog");
    }

    public static Path unreleasedDirectory(final Path projectRootDirectory, final int versionMajor) {
        final String filename = String.format(".unreleased-%d.x.x", versionMajor);
        return changelogDirectory(projectRootDirectory).resolve(filename);
    }

    public static Set<Integer> unreleasedDirectoryVersionMajors(final Path projectRootDirectory) {
        final Path changelogDirectory = changelogDirectory(projectRootDirectory);
        try {
            return Files
                    .walk(changelogDirectory, 1)
                    .filter(path -> {
                        return !path.equals(projectRootDirectory) &&                        // Skip the directory itself.
                                path.getFileName().toString().startsWith(".unreleased-");   // Only select `.unreleased-*` directories.
                    })
                    .map(path -> {
                        final String filename = path.getFileName().toString();
                        final String versionMajor = filename.replaceFirst("^\\.unreleased-(\\d+)\\.x\\.x", "$1");
                        return Integer.parseInt(versionMajor);
                    })
                    .collect(Collectors.toSet());
        } catch (final IOException error) {
            final String message = String.format("failed walking directory: `%s`", projectRootDirectory);
            throw new UncheckedIOException(message, error);
        }
    }

    public static Path releaseDirectory(
            final Path projectRootDirectory,
            final String releaseDate,
            final String releaseVersion) {
        if (!releaseDate.matches("^\\d{8}$")) {
            final String message = String.format(
                    "release date doesn't match the expected `YYYYmmdd` pattern: `%s`", releaseDate);
            throw new IllegalArgumentException(message);
        }
        if (!releaseVersion.matches(PomUtils.VERSION_PATTERN)) {
            final String message = String.format(
                    "release version doesn't match the expected `%s` pattern: `%s`",
                    PomUtils.VERSION_PATTERN, releaseVersion);
            throw new IllegalArgumentException(message);
        }
        final String releaseDirectoryName = String.format("%s-%s", releaseDate, releaseVersion);
        return changelogDirectory(projectRootDirectory).resolve(releaseDirectoryName);
    }

    public static Path releaseXmlFile(final Path releaseDirectory) {
        return releaseDirectory.resolve(".release.xml");
    }

    public static Path introAsciiDocFile(final Path releaseDirectory) {
        return releaseDirectory.resolve(".intro.adoc");
    }

}
