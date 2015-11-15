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

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PathFilter that accepts files for deletion if their relative path matches either a path pattern or a regular
 * expression.
 * <p>
 * If both a regular expression and a path pattern are specified the path pattern is used and the regular expression is
 * ignored.
 * <p>
 * The path pattern may contain '?' and '*' wildcarts.
 */
@Plugin(name = "FileNameFilter", category = "Core", printObject = true)
public final class IfFileName implements PathCondition {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final Pattern regex;
    private final String pathPattern;

    /**
     * Constructs a FileNameFilter filter. If both a regular expression and a path pattern are specified the path
     * pattern is used and the regular expression is ignored.
     * 
     * @param path the baseDir-relative path pattern of the files to delete (may contain '*' and '?' wildcarts)
     * @param regex the regular expression that matches the baseDir-relative path of the file(s) to delete
     */
    private IfFileName(final String path, final String regex) {
        if (regex == null && path == null) {
            throw new IllegalArgumentException("Specify either a path or a regular expression. Both cannot be null.");
        }
        this.regex = regex != null ? Pattern.compile(regex) : null;
        this.pathPattern = path;
    }

    /**
     * Returns the compiled regular expression that matches the baseDir-relative path of the file(s) to delete, or
     * {@code null} if no regular expression was specified.
     * 
     * @return the compiled regular expression, or {@code null}
     */
    public Pattern getRegex() {
        return regex;
    }

    /**
     * Returns the baseDir-relative path pattern of the files to delete, or {@code null} if not specified. This path
     * pattern may contain '*' and '?' wildcarts.
     * 
     * @return relative path of the file(s) to delete (may contain '*' and '?' wildcarts)
     */
    public String getPathPattern() {
        return pathPattern;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathFilter#accept(java.nio.file.Path,
     * java.nio.file.Path)
     */
    @Override
    public boolean accept(final Path basePath, final Path relativePath, final BasicFileAttributes attrs) {
        if (pathPattern != null) {
            final boolean result = isMatch(relativePath.toString(), pathPattern);
            final String match = result ? "" : "not ";
            LOGGER.trace("FileNameFilter: pathPattern '{}' does {}match relative path '{}'", pathPattern, match,
                    relativePath);
            return result;
        } else {
            final Matcher matcher = regex.matcher(relativePath.toString());
            final boolean result = matcher.matches();
            final String match = result ? "" : "not ";
            LOGGER.trace("FileNameFilter: regex '{}' does {}match relative path '{}'", regex.pattern(), match,
                    relativePath);
            return result;
        }
    }

    // package protected for unit tests
    static boolean isMatch(final String text, final String pattern) {
        int i = 0;
        int j = 0;
        int starIndex = -1;
        int iIndex = -1;

        while (i < text.length()) {
            if (j < pattern.length() && (pattern.charAt(j) == '?' || pattern.charAt(j) == text.charAt(i))) {
                ++i;
                ++j;
            } else if (j < pattern.length() && pattern.charAt(j) == '*') {
                starIndex = j;
                iIndex = i;
                j++;
            } else if (starIndex != -1) {
                j = starIndex + 1;
                i = iIndex + 1;
                iIndex++;
            } else {
                return false;
            }
        }

        while (j < pattern.length() && pattern.charAt(j) == '*') {
            ++j;
        }
        return j == pattern.length();
    }

    /**
     * Creates a FileNameFilter filter. If both a regular expression and a path pattern are specified the path pattern
     * is used and the regular expression is ignored.
     * 
     * @param path the baseDir-relative path pattern of the files to delete (may contain '*' and '?' wildcarts)
     * @param regex the regular expression that matches the baseDir-relative path of the file(s) to delete
     * @return A FileNameFilter filter.
     */
    @PluginFactory
    public static IfFileName createNameFilter( //
            @PluginAttribute("path") final String path, //
            @PluginAttribute("regex") final String regex) {
        return new IfFileName(path, regex);
    }

    @Override
    public String toString() {
        final String pattern = regex == null ? "null" : regex.pattern();
        return "FileNameFilter(regex=" + pattern + ", path=" + pathPattern + ")";
    }
}
