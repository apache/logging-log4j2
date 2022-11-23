/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.internal.util.changelog.releaser;

import org.apache.logging.log4j.internal.util.AsciiDocUtils;
import org.apache.logging.log4j.internal.util.XmlReader;
import org.apache.logging.log4j.internal.util.changelog.ChangelogFiles;
import org.apache.logging.log4j.internal.util.changelog.ChangelogRelease;
import org.w3c.dom.Element;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static org.apache.logging.log4j.internal.util.StringUtils.isBlank;
import static org.apache.logging.log4j.internal.util.StringUtils.trimNullable;
import static org.apache.logging.log4j.internal.util.changelog.ChangelogFiles.releaseDirectory;

public final class ChangelogReleaser {

    private ChangelogReleaser() {}

    public static void main(final String[] mainArgs) throws Exception {

        // Read arguments.
        final ChangelogReleaserArgs args = ChangelogReleaserArgs.fromMainArgs(mainArgs);

        // Read the release date and version.
        final String releaseDate = BASIC_ISO_DATE.format(LocalDate.now());
        final String releaseVersion = readReleaseVersion(args.projectRootDirectory);
        System.out.format(
                "using `%s` and `%s` for release date and version, respectively%n",
                releaseDate, releaseVersion);

        // Move unreleased directory to a release directory.
        final Path releaseDirectory = releaseDirectory(args.projectRootDirectory, releaseDate, releaseVersion);
        final Path unreleasedDirectory = ChangelogFiles.unreleasedDirectory(args.projectRootDirectory);
        if (!Files.exists(unreleasedDirectory)) {
            final String message = String.format(
                    "`%s` does not exist! A release without any changelogs don't make sense!",
                    unreleasedDirectory);
            throw new IllegalStateException(message);
        }
        System.out.format("moving `%s` to `%s`%n", unreleasedDirectory, releaseDirectory);
        Files.move(unreleasedDirectory, releaseDirectory);
        Files.createDirectories(unreleasedDirectory);

        // Write the release information.
        final Path releaseXmlFile = ChangelogFiles.releaseXmlFile(releaseDirectory);
        System.out.format("writing release information to `%s`%n", releaseXmlFile);
        final ChangelogRelease changelogRelease = new ChangelogRelease(releaseVersion, releaseDate);
        changelogRelease.writeToXmlFile(releaseXmlFile);

        // Write the release introduction.
        final Path introAsciiDocFile = ChangelogFiles.introAsciiDocFile(releaseDirectory);
        Files.write(introAsciiDocFile, AsciiDocUtils.LICENSE_COMMENT_BLOCK.getBytes(StandardCharsets.UTF_8));
        System.out.format("created intro file at `%s`%n", introAsciiDocFile);

    }

    private static String readReleaseVersion(final Path projectRootDirectory) {

        // Read the root `project` element.
        final Path rootPomFile = projectRootDirectory.resolve("pom.xml");
        final Element projectElement = XmlReader.readXmlFileRootElement(rootPomFile, "project");

        // Read the `version` element.
        final Element versionElement = XmlReader.childElementMatchingName(projectElement, "version");
        final String version = trimNullable(versionElement.getTextContent());
        if (isBlank(version)) {
            throw XmlReader.failureAtXmlNode(versionElement, "blank `version`: %s", version);
        }
        return version;

    }

}
