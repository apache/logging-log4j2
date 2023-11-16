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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Abstract action for processing files that are accepted by the specified PathFilters.
 */
public abstract class AbstractPathAction extends AbstractAction {

    private final String basePathString;
    private final Set<FileVisitOption> options;
    private final int maxDepth;
    private final List<PathCondition> pathConditions;
    private final StrSubstitutor subst;

    /**
     * Creates a new AbstractPathAction that starts scanning for files to process from the specified base path.
     *
     * @param basePath base path from where to start scanning for files to process.
     * @param followSymbolicLinks whether to follow symbolic links. Default is false.
     * @param maxDepth The maxDepth parameter is the maximum number of levels of directories to visit. A value of 0
     *            means that only the starting file is visited, unless denied by the security manager. A value of
     *            MAX_VALUE may be used to indicate that all levels should be visited.
     * @param pathFilters an array of path filters (if more than one, they all need to accept a path before it is
     *            processed).
     */
    protected AbstractPathAction(
            final String basePath,
            final boolean followSymbolicLinks,
            final int maxDepth,
            final PathCondition[] pathFilters,
            final StrSubstitutor subst) {
        this.basePathString = basePath;
        this.options = followSymbolicLinks
                ? EnumSet.of(FileVisitOption.FOLLOW_LINKS)
                : Collections.<FileVisitOption>emptySet();
        this.maxDepth = maxDepth;
        this.pathConditions = Arrays.asList(Arrays.copyOf(pathFilters, pathFilters.length));
        this.subst = subst;
    }

    @Override
    public boolean execute() throws IOException {
        return execute(createFileVisitor(getBasePath(), pathConditions));
    }

    public boolean execute(final FileVisitor<Path> visitor) throws IOException {
        final long start = System.nanoTime();
        LOGGER.debug("Starting {}", this);

        Files.walkFileTree(getBasePath(), options, maxDepth, visitor);

        final double duration = System.nanoTime() - start;
        LOGGER.debug("{} complete in {} seconds", getClass().getSimpleName(), duration / TimeUnit.SECONDS.toNanos(1));

        // TODO return (visitor.success || ignoreProcessingFailure)
        return true; // do not abort rollover even if processing failed
    }

    /**
     * Creates a new {@code FileVisitor<Path>} to pass to the {@link Files#walkFileTree(Path, Set, int, FileVisitor)}
     * method when the {@link #execute()} method is invoked.
     * <p>
     * The visitor is responsible for processing the files it encounters that are accepted by all filters.
     *
     * @param visitorBaseDir base dir from where to start scanning for files to process
     * @param conditions filters that determine if a file should be processed
     * @return a new {@code FileVisitor<Path>}
     */
    protected abstract FileVisitor<Path> createFileVisitor(
            final Path visitorBaseDir, final List<PathCondition> conditions);

    /**
     * Returns the base path from where to start scanning for files to delete. Lookups are resolved, so if the
     * configuration was <code>&lt;Delete basePath="${sys:user.home}/abc" /&gt;</code> then this method returns a path
     * to the "abc" file or directory in the user's home directory.
     *
     * @return the base path (all lookups resolved)
     */
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The name of the accessed files is based on a configuration value.")
    public Path getBasePath() {
        return Paths.get(subst.replace(getBasePathString()));
    }

    /**
     * Returns the base path as it was specified in the configuration. Lookups are not resolved.
     *
     * @return the base path as it was specified in the configuration
     */
    public String getBasePathString() {
        return basePathString;
    }

    public StrSubstitutor getStrSubstitutor() {
        return subst;
    }

    /**
     * Returns whether to follow symbolic links or not.
     *
     * @return the options
     */
    public Set<FileVisitOption> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    /**
     * Returns whether to follow symbolic links or not.
     *
     * @return whether to follow symbolic links or not
     */
    public boolean isFollowSymbolicLinks() {
        return options.contains(FileVisitOption.FOLLOW_LINKS);
    }

    /**
     * Returns the maximum number of directory levels to visit.
     *
     * @return the maxDepth
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * Returns the list of PathCondition objects.
     *
     * @return the pathFilters
     */
    public List<PathCondition> getPathConditions() {
        return Collections.unmodifiableList(pathConditions);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[basePath=" + getBasePath() + ", options=" + options + ", maxDepth="
                + maxDepth + ", conditions=" + pathConditions + "]";
    }
}
