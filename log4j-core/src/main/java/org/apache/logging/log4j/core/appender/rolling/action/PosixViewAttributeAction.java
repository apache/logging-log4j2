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
package org.apache.logging.log4j.core.appender.rolling.action;

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
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.FileUtils;

/**
 * File posix attribute action.
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

    /**
     * Constructor of the posix view attribute action.
     *
     * @param basePath {@link AbstractPathAction#getBasePath()}
     * @param followSymbolicLinks {@link AbstractPathAction#isFollowSymbolicLinks()}
     * @param pathConditions {@link AbstractPathAction#getPathConditions()}
     * @param subst {@link AbstractPathAction#getStrSubstitutor()}
     * @param filePermissions Permissions to apply
     * @param fileOwner File owner
     * @param fileGroup File group
     * @param followSymbolicLinks 
     */
    public PosixViewAttributeAction(final String basePath, final boolean followSymbolicLinks,
            final int maxDepth, final PathCondition[] pathConditions, final StrSubstitutor subst,
            final Set<PosixFilePermission> filePermissions,
            final String fileOwner, final String fileGroup) {
        super(basePath, followSymbolicLinks, maxDepth, pathConditions, subst);
        this.filePermissions = filePermissions;
        this.fileOwner = fileOwner;
        this.fileGroup = fileGroup;
    }

    /**
     * Creates a PosixViewAttributeAction action that defined posix attribute view on a file.
     * 
     * @param basePath {@link AbstractPathAction#getBasePath()}
     * @param followSymbolicLinks {@link AbstractPathAction#isFollowSymbolicLinks()}
     * @param pathConditions {@link AbstractPathAction#getPathConditions()}
     * @param subst {@link AbstractPathAction#getStrSubstitutor()}
     * @param filePermissions File permissions
     * @param fileOwner File owner
     * @param fileGroup File group
     * @return PosixViewAttribute action
     */
    @PluginFactory
    public static PosixViewAttributeAction createNameCondition(
            // @formatter:off
            @PluginAttribute("basePath") final String basePath, //
            @PluginAttribute(value = "followLinks") final boolean followLinks,
            @PluginAttribute(value = "maxDepth", defaultInt = 1) final int maxDepth,
            @PluginElement("PathConditions") final PathCondition[] pathConditions,
            @PluginAttribute("filePermissions") final String filePermissions,
            @PluginAttribute("fileOwner") final String fileOwner,
            @PluginAttribute("fileGroup") final String fileGroup,
            @PluginConfiguration final Configuration config) {
            // @formatter:on
        return new PosixViewAttributeAction(basePath, followLinks, maxDepth,
                    pathConditions, config.getStrSubstitutor(),
                    filePermissions != null ? PosixFilePermissions.fromString(filePermissions) : null,
                    fileOwner,
                    fileGroup);
    }

    @Override
    protected FileVisitor<Path> createFileVisitor(final Path basePath,
            final List<PathCondition> conditions) {
        return new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                for (final PathCondition pathFilter : conditions) {
                    final Path relative = basePath.relativize(file);
                    if (!pathFilter.accept(basePath, relative, attrs)) {
                        LOGGER.trace("Not defining posix attribute base={}, relative={}", basePath, relative);
                        return FileVisitResult.CONTINUE;
                    }
                }
                FileUtils.defineFilePosixAttributeView(file, filePermissions, fileOwner, fileGroup);
                return FileVisitResult.CONTINUE;
            }
        };
    }

    /**
     * Returns posix file permissions if defined and the OS supports posix file attribute,
     * null otherwise.
     * @return File posix permissions
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
     * Returns file group if defined and the OS supports posix/group file attribute view,
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
