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
package org.apache.logging.log4j.core.appender.rolling;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CompositeAction;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.NotANumber;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public abstract class AbstractRolloverStrategy implements RolloverStrategy {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    public static final Pattern PATTERN_COUNTER = Pattern.compile(".*%(?<ZEROPAD>0)?(?<PADDING>\\d+)?i.*");

    protected final StrSubstitutor strSubstitutor;

    protected AbstractRolloverStrategy(final StrSubstitutor strSubstitutor) {
        this.strSubstitutor = strSubstitutor;
    }

    public StrSubstitutor getStrSubstitutor() {
        return strSubstitutor;
    }

    protected Action merge(final Action compressAction, final List<Action> custom, final boolean stopOnError) {
        if (custom.isEmpty()) {
            return compressAction;
        }
        if (compressAction == null) {
            return new CompositeAction(custom, stopOnError);
        }
        final List<Action> all = new ArrayList<>();
        all.add(compressAction);
        all.addAll(custom);
        return new CompositeAction(all, stopOnError);
    }

    protected int suffixLength(final String lowFilename) {
        for (final FileExtension extension : FileExtension.values()) {
            if (extension.isExtensionFor(lowFilename)) {
                return extension.length();
            }
        }
        return 0;
    }

    protected SortedMap<Integer, Path> getEligibleFiles(final RollingFileManager manager) {
        return getEligibleFiles(manager, true);
    }

    protected SortedMap<Integer, Path> getEligibleFiles(final RollingFileManager manager, final boolean isAscending) {
        final StringBuilder buf = new StringBuilder();
        final String pattern = manager.getPatternProcessor().getPattern();
        manager.getPatternProcessor().formatFileName(strSubstitutor, buf, NotANumber.NAN);
        final String fileName = manager.isDirectWrite() ? "" : manager.getFileName();
        return getEligibleFiles(fileName, buf.toString(), pattern, isAscending);
    }

    protected SortedMap<Integer, Path> getEligibleFiles(final String path, final String pattern) {
        return getEligibleFiles("", path, pattern, true);
    }

    @Deprecated
    protected SortedMap<Integer, Path> getEligibleFiles(
            final String path, final String logfilePattern, final boolean isAscending) {
        return getEligibleFiles("", path, logfilePattern, isAscending);
    }

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The file path should be specified in the configuration file.")
    protected SortedMap<Integer, Path> getEligibleFiles(
            final String currentFile, final String path, final String logfilePattern, final boolean isAscending) {
        final TreeMap<Integer, Path> eligibleFiles = new TreeMap<>();
        final File file = new File(path);
        File parent = file.getParentFile();
        if (parent == null) {
            parent = new File(".");
        } else {
            parent.mkdirs();
        }
        if (!PATTERN_COUNTER.matcher(logfilePattern).find()) {
            return eligibleFiles;
        }
        final Path dir = parent.toPath();
        String fileName = file.getName();
        final int suffixLength = suffixLength(fileName);
        // use Pattern.quote to treat all initial parts of the fileName as literal
        // this fixes issues with filenames containing 'magic' regex characters
        if (suffixLength > 0) {
            fileName = Pattern.quote(fileName.substring(0, fileName.length() - suffixLength)) + ".*";
        } else {
            fileName = Pattern.quote(fileName);
        }
        // since we insert a pattern inside a regex escaped string,
        // surround it with quote characters so that (\d) is treated as a pattern and not a literal
        final String filePattern = fileName.replaceFirst("0*\\u0000", "\\\\E(0?\\\\d+)\\\\Q");
        final Pattern pattern = Pattern.compile(filePattern);
        final Path current = currentFile.length() > 0 ? new File(currentFile).toPath() : null;
        LOGGER.debug("Current file: {}", currentFile);

        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (final Path entry : stream) {
                final Matcher matcher = pattern.matcher(entry.toFile().getName());
                if (matcher.matches() && !entry.equals(current)) {
                    try {
                        final Integer index = Integers.parseInt(matcher.group(1));
                        eligibleFiles.put(index, entry);
                    } catch (NumberFormatException ex) {
                        LOGGER.debug(
                                "Ignoring file {} which matches pattern but the index is invalid.",
                                entry.toFile().getName());
                    }
                }
            }
        } catch (final IOException ioe) {
            throw new LoggingException("Error reading folder " + dir + " " + ioe.getMessage(), ioe);
        }
        return isAscending ? eligibleFiles : eligibleFiles.descendingMap();
    }
}
