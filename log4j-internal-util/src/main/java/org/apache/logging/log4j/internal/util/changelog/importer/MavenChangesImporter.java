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
package org.apache.logging.log4j.internal.util.changelog.importer;

import org.apache.logging.log4j.internal.util.changelog.ChangelogEntry;
import org.apache.logging.log4j.internal.util.changelog.ChangelogFiles;
import org.apache.logging.log4j.internal.util.changelog.ChangelogRelease;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.apache.logging.log4j.internal.util.StringUtils.isBlank;

public final class MavenChangesImporter {

    private MavenChangesImporter() {}

    public static void main(final String[] mainArgs) {
        final MavenChangesImporterArgs args = MavenChangesImporterArgs.fromMainArgs(mainArgs);
        final MavenChanges mavenChanges = MavenChanges.readFromProjectRootPath(args.projectRootDirectory);
        mavenChanges.releases.forEach(release -> {
            if ("TBD".equals(release.date)) {
                writeUnreleased(args.projectRootDirectory, release);
            } else {
                writeReleased(args.projectRootDirectory, release);
            }
        });
    }

    private static void writeUnreleased(final Path projectRootDirectory, final MavenChanges.Release release) {
        final Path releaseDirectory = ChangelogFiles
                .changelogDirectory(projectRootDirectory)
                .resolve(".unreleased");
        release.actions.forEach(action -> writeAction(releaseDirectory, action));
    }

    private static void writeReleased(final Path projectRootDirectory, final MavenChanges.Release release) {

        // Determine the directory for this particular release.
        final Path releaseDirectory = ChangelogFiles.releaseDirectory(
                projectRootDirectory,
                release.date.replaceAll("-", ""),
                release.version);

        // Write release information.
        final Path releaseFile = ChangelogFiles.releaseXmlFile(releaseDirectory);
        final ChangelogRelease changelogRelease = new ChangelogRelease(release.version, release.date);
        changelogRelease.writeToXmlFile(releaseFile);

        // Write release actions.
        release.actions.forEach(action -> writeAction(releaseDirectory, action));

    }

    private static void writeAction(final Path releaseDirectory, final MavenChanges.Action action) {
        final ChangelogEntry changelogEntry = changelogEntry(action);
        final String changelogEntryFilename = changelogEntryFilename(action);
        final Path changelogEntryFile = releaseDirectory.resolve(changelogEntryFilename);
        changelogEntry.writeToXmlFile(changelogEntryFile);
    }

    private static String changelogEntryFilename(final MavenChanges.Action action) {
        final StringBuilder actionRelativeFileBuilder = new StringBuilder();
        if (action.issue != null) {
            actionRelativeFileBuilder
                    .append(action.issue)
                    .append('_');
        }
        final String sanitizedDescription = action
                .description
                .substring(0, Math.min(action.description.length(), 60))
                .replaceAll("[^A-Za-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("[^A-Za-z0-9]$", "");
        actionRelativeFileBuilder.append(sanitizedDescription);
        actionRelativeFileBuilder.append(".xml");
        return actionRelativeFileBuilder.toString();
    }

    private static ChangelogEntry changelogEntry(final MavenChanges.Action action) {

        // Create the `type`.
        final ChangelogEntry.Type type = changelogType(action.type);

        // Create the `issue`s.
        final List<ChangelogEntry.Issue> issues = new ArrayList<>(1);
        if (action.issue != null) {
            final String issueLink = String.format("https://issues.apache.org/jira/browse/%s", action.issue);
            final ChangelogEntry.Issue issue = new ChangelogEntry.Issue(action.issue, issueLink);
            issues.add(issue);
        }

        // Create the `author`s.
        final List<ChangelogEntry.Author> authors = new ArrayList<>(2);
        for (final String authorId : action.dev.split("\\s*,\\s*")) {
            if (!isBlank(authorId)) {
                authors.add(new ChangelogEntry.Author(authorId, null));
            }
        }
        if (action.dueTo != null) {
            authors.add(new ChangelogEntry.Author(null, action.dueTo));
        }

        // Create the `description`.
        final ChangelogEntry.Description description = new ChangelogEntry.Description("asciidoc", action.description);

        // Create the instance.
        return new ChangelogEntry(type, issues, authors, description);

    }

    /**
     * Maps `maven-changes-plugin` action types to their `Keep a Changelog` equivalents.
     */
    private static ChangelogEntry.Type changelogType(final MavenChanges.Action.Type type) {
        if (MavenChanges.Action.Type.ADD.equals(type)) {
            return ChangelogEntry.Type.ADDED;
        } else if (MavenChanges.Action.Type.FIX.equals(type)) {
            return ChangelogEntry.Type.FIXED;
        } else if (MavenChanges.Action.Type.REMOVE.equals(type)) {
            return ChangelogEntry.Type.REMOVED;
        } else {
            return ChangelogEntry.Type.CHANGED;
        }
    }

}
