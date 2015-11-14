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

import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Rollover or scheduled action for deleting old log files that are accepted by the specified PathFilters.
 */
@Plugin(name = "Delete", category = "Core", printObject = true)
public class DeleteAction extends AbstractPathAction {

    /**
     * Creates a new DeleteAction that starts scanning for files to delete from the specified base path.
     * 
     * @param basePath base path from where to start scanning for files to delete.
     * @param followSymbolicLinks whether to follow symbolic links. Default is false.
     * @param maxDepth The maxDepth parameter is the maximum number of levels of directories to visit. A value of 0
     *            means that only the starting file is visited, unless denied by the security manager. A value of
     *            MAX_VALUE may be used to indicate that all levels should be visited.
     * @param pathFilters an array of path filters (if more than one, they all need to accept a path before it is
     *            deleted).
     */
    DeleteAction(final String basePath, final boolean followSymbolicLinks, final int maxDepth,
            final PathFilter[] pathFilters, final StrSubstitutor subst) {
        super(basePath, followSymbolicLinks, maxDepth, pathFilters, subst);
    }

    @Override
    protected FileVisitor<Path> createFileVisitor(final Path visitorBaseDir, final List<PathFilter> visitorFilters) {
        return new DeletingVisitor(visitorBaseDir, visitorFilters);
    }

    /**
     * Create a DeleteAction.
     * 
     * @param basePath base path from where to start scanning for files to delete.
     * @param followLinks whether to follow symbolic links. Default is false.
     * @param maxDepth The maxDepth parameter is the maximum number of levels of directories to visit. A value of 0
     *            means that only the starting file is visited, unless denied by the security manager. A value of
     *            MAX_VALUE may be used to indicate that all levels should be visited.
     * @param pathFilters an array of path filters (if more than one, they all need to accept a path before it is
     *            deleted).
     * @param config The Configuration.
     * @return A DeleteAction.
     */
    @PluginFactory
    public static DeleteAction createDeleteAction(
            // @formatter:off
            @PluginAttribute("basePath") final String basePath, //
            @PluginAttribute(value = "followLinks", defaultBoolean = false) final boolean followLinks,
            @PluginAttribute(value = "maxDepth", defaultInt = 1) final int maxDepth,
            @PluginElement("PathFilters") final PathFilter[] pathFilters,
            @PluginConfiguration final Configuration config) {
            // @formatter:on
        return new DeleteAction(basePath, followLinks, maxDepth, pathFilters, config.getStrSubstitutor());
    }
}
