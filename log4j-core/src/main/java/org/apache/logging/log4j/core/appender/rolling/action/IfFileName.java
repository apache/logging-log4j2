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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PathCondition that accepts files for deletion if their relative path matches either a glob pattern or a regular
 * expression. If both a regular expression and a glob pattern are specified the glob pattern is used and the regular
 * expression is ignored.
 * <p>
 * The regular expression is a pattern as defined by the {@link Pattern} class. A glob is a simplified pattern expression
 * described in {@link FileSystem#getPathMatcher(String)}.
 */
@Plugin(name = "IfFileName", category = "Core", printObject = true)
public final class IfFileName implements PathCondition {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final PathMatcher pathMatcher;
    private final String syntaxAndPattern;

    /**
     * Constructs a FileNameFilter filter. If both a regular expression and a glob pattern are specified the glob
     * pattern is used and the regular expression is ignored.
     * 
     * @param glob the baseDir-relative path pattern of the files to delete (may contain '*' and '?' wildcarts)
     * @param regex the regular expression that matches the baseDir-relative path of the file(s) to delete
     */
    private IfFileName(final String glob, final String regex) {
        if (regex == null && glob == null) {
            throw new IllegalArgumentException("Specify either a path glob or a regular expression. "
                    + "Both cannot be null.");
        }
        syntaxAndPattern = createSyntaxAndPatternString(glob, regex);
        pathMatcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
    }

    static String createSyntaxAndPatternString(final String glob, final String regex) {
        if (glob != null) {
            return glob.startsWith("glob:") ? glob : "glob:" + glob;
        }
        return regex.startsWith("regex:") ? regex : "regex:" + regex;
    }

    /**
     * Returns the baseDir-relative path pattern of the files to delete. The returned string takes the form
     * {@code syntax:pattern} where syntax is one of "glob" or "regex" and the pattern is either a {@linkplain Pattern
     * regular expression} or a simplified pattern expression described under "glob" in
     * {@link FileSystem#getPathMatcher(String)}.
     * 
     * @return relative path of the file(s) to delete (may contain regular expression or wildcarts)
     */
    public String getSyntaxAndPattern() {
        return syntaxAndPattern;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#accept(java.nio.file.Path, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public boolean accept(final Path basePath, final Path relativePath, final BasicFileAttributes attrs) {
        final boolean result = pathMatcher.matches(relativePath);

        final String match = result ? " " : " not ";
        LOGGER.trace("IfFileName: '{}' does{}match relative path '{}'", syntaxAndPattern, match, relativePath);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#beforeFileTreeWalk()
     */
    @Override
    public void beforeFileTreeWalk() {
    }

    /**
     * Creates a IfFileName condition that returns true if either the specified
     * {@linkplain FileSystem#getPathMatcher(String) glob pattern} or the regular expression matches the relative path.
     * If both a regular expression and a glob pattern are specified the glob pattern is used and the regular expression
     * is ignored.
     * 
     * @param glob the baseDir-relative path pattern of the files to delete (may contain '*' and '?' wildcarts)
     * @param regex the regular expression that matches the baseDir-relative path of the file(s) to delete
     * @return A IfFileName condition.
     * @see FileSystem#getPathMatcher(String)
     */
    @PluginFactory
    public static IfFileName createNameCondition( //
            @PluginAttribute("glob") final String glob, //
            @PluginAttribute("regex") final String regex) {
        return new IfFileName(glob, regex);
    }

    @Override
    public String toString() {
        return "IfFileName(" + syntaxAndPattern + ")";
    }
}
