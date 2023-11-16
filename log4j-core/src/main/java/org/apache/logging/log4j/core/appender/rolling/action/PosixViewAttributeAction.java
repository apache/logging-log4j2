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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.util.Strings;

/**
 * File POSIX attribute view action.
 *
 * Allow to define file permissions, user and group for log files on POSIX supported OS.
 */
@Plugin(name = "PosixViewAttribute", category = Core.CATEGORY_NAME, printObject = true)
public class PosixViewAttributeAction extends AbstractPathAction {

    /**
     * File permissions.
     */
    private final Set<PosixFilePermission> filePermissions;

    /**
     * File owner.
     */
    private final String fileOwner;

    /**
     * File group.
     */
    private final String fileGroup;

    private PosixViewAttributeAction(
            final String basePath,
            final boolean followSymbolicLinks,
            final int maxDepth,
            final PathCondition[] pathConditions,
            final StrSubstitutor subst,
            final Set<PosixFilePermission> filePermissions,
            final String fileOwner,
            final String fileGroup) {
        super(basePath, followSymbolicLinks, maxDepth, pathConditions, subst);
        this.filePermissions = filePermissions;
        this.fileOwner = fileOwner;
        this.fileGroup = fileGroup;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for the POSIX view attribute action.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<PosixViewAttributeAction> {

        @PluginConfiguration
        private Configuration configuration;

        private StrSubstitutor subst;

        @PluginBuilderAttribute
        @Required(message = "No base path provided")
        private String basePath;

        @PluginBuilderAttribute
        private boolean followLinks = false;

        @PluginBuilderAttribute
        private int maxDepth = 1;

        @PluginElement("PathConditions")
        private PathCondition[] pathConditions;

        @PluginBuilderAttribute(value = "filePermissions")
        private String filePermissionsString;

        private Set<PosixFilePermission> filePermissions;

        @PluginBuilderAttribute
        private String fileOwner;

        @PluginBuilderAttribute
        private String fileGroup;

        @Override
        @SuppressFBWarnings(
                value = "OVERLY_PERMISSIVE_FILE_PERMISSION",
                justification = "File permissions are specified in a configuration file.")
        public PosixViewAttributeAction build() {
            if (Strings.isEmpty(basePath)) {
                LOGGER.error("Posix file attribute view action not valid because base path is empty.");
                return null;
            }

            if (filePermissions == null
                    && Strings.isEmpty(filePermissionsString)
                    && Strings.isEmpty(fileOwner)
                    && Strings.isEmpty(fileGroup)) {
                LOGGER.error("Posix file attribute view not valid because nor permissions, user or group defined.");
                return null;
            }

            if (!FileUtils.isFilePosixAttributeViewSupported()) {
                LOGGER.warn("Posix file attribute view defined but it is not supported by this files system.");
                return null;
            }

            return new PosixViewAttributeAction(
                    basePath,
                    followLinks,
                    maxDepth,
                    pathConditions,
                    subst != null ? subst : configuration.getStrSubstitutor(),
                    filePermissions != null
                            ? filePermissions
                            : filePermissionsString != null
                                    ? PosixFilePermissions.fromString(filePermissionsString)
                                    : null,
                    fileOwner,
                    fileGroup);
        }

        /**
         * Define required configuration, used to retrieve string substituter.
         *
         * @param configuration {@link AbstractPathAction#getStrSubstitutor()}
         * @return This builder
         */
        public Builder withConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Define string substituter.
         *
         * @param subst {@link AbstractPathAction#getStrSubstitutor()}
         * @return This builder
         */
        public Builder withSubst(final StrSubstitutor subst) {
            this.subst = subst;
            return this;
        }

        /**
         * Define base path to apply condition before execute POSIX file attribute action.
         * @param basePath {@link AbstractPathAction#getBasePath()}
         * @return This builder
         */
        public Builder withBasePath(final String basePath) {
            this.basePath = basePath;
            return this;
        }

        /**
         * True to allow synonyms links during search of eligible files.
         * @param followLinks Follow synonyms links
         * @return This builder
         */
        public Builder withFollowLinks(final boolean followLinks) {
            this.followLinks = followLinks;
            return this;
        }

        /**
         * Define max folder depth to search for eligible files to apply POSIX attribute view.
         * @param maxDepth Max search depth
         * @return This builder
         */
        public Builder withMaxDepth(final int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        /**
         * Define path conditions to filter files in {@link PosixViewAttributeAction#getBasePath()}.
         *
         * @param pathConditions {@link AbstractPathAction#getPathConditions()}
         * @return This builder
         */
        public Builder withPathConditions(final PathCondition[] pathConditions) {
            this.pathConditions = pathConditions;
            return this;
        }

        /**
         * Define file permissions in POSIX format to apply during action execution eligible files.
         *
         * Example:
         * <p>rw-rw-rw
         * <p>r--r--r--
         * @param filePermissionsString Permissions to apply
         * @return This builder
         */
        public Builder withFilePermissionsString(final String filePermissionsString) {
            this.filePermissionsString = filePermissionsString;
            return this;
        }

        /**
         * Define file permissions to apply during action execution eligible files.
         * @param filePermissions Permissions to apply
         * @return This builder
         */
        public Builder withFilePermissions(final Set<PosixFilePermission> filePermissions) {
            this.filePermissions = filePermissions;
            return this;
        }

        /**
         * Define file owner to apply during action execution eligible files.
         * @param fileOwner File owner
         * @return This builder
         */
        public Builder withFileOwner(final String fileOwner) {
            this.fileOwner = fileOwner;
            return this;
        }

        /**
         * Define file group to apply during action execution eligible files.
         * @param fileGroup File group
         * @return This builder
         */
        public Builder withFileGroup(final String fileGroup) {
            this.fileGroup = fileGroup;
            return this;
        }
    }

    @Override
    protected FileVisitor<Path> createFileVisitor(final Path basePath, final List<PathCondition> conditions) {
        return new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                for (final PathCondition pathFilter : conditions) {
                    final Path relative = basePath.relativize(file);
                    if (!pathFilter.accept(basePath, relative, attrs)) {
                        LOGGER.trace("Not defining POSIX attribute base={}, relative={}", basePath, relative);
                        return FileVisitResult.CONTINUE;
                    }
                }
                FileUtils.defineFilePosixAttributeView(file, filePermissions, fileOwner, fileGroup);
                return FileVisitResult.CONTINUE;
            }
        };
    }

    /**
     * Returns POSIX file permissions if defined and the OS supports POSIX file attribute,
     * null otherwise.
     * @return File POSIX permissions
     * @see PosixFileAttributeView
     */
    public Set<PosixFilePermission> getFilePermissions() {
        return filePermissions;
    }

    /**
     * Returns file owner if defined and the OS supports owner file attribute view,
     * null otherwise.
     * @return File owner
     * @see FileOwnerAttributeView
     */
    public String getFileOwner() {
        return fileOwner;
    }

    /**
     * Returns file group if defined and the OS supports POSIX/group file attribute view,
     * null otherwise.
     * @return File group
     * @see PosixFileAttributeView
     */
    public String getFileGroup() {
        return fileGroup;
    }

    @Override
    public String toString() {
        return "PosixViewAttributeAction [filePermissions=" + filePermissions + ", fileOwner="
                + fileOwner + ", fileGroup=" + fileGroup + ", getBasePath()=" + getBasePath()
                + ", getMaxDepth()=" + getMaxDepth() + ", getPathConditions()="
                + getPathConditions() + "]";
    }
}
