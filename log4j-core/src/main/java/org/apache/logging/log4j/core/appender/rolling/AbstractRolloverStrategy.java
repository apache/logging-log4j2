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
package org.apache.logging.log4j.core.appender.rolling;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CommonsCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.CompositeAction;
import org.apache.logging.log4j.core.appender.rolling.action.GzCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.ZipCompressAction;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.NotANumber;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public abstract class AbstractRolloverStrategy implements RolloverStrategy {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

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

    protected SortedMap<Integer, Path> getEligibleFiles(final RollingFileManager manager,
                                                        final boolean isAscending) {
        final StringBuilder buf = new StringBuilder();
        String pattern = manager.getPatternProcessor().getPattern();
        manager.getPatternProcessor().formatFileName(strSubstitutor, buf, NotANumber.NAN);
        return getEligibleFiles(buf.toString(), pattern, isAscending);
    }

    protected SortedMap<Integer, Path> getEligibleFiles(String path, String pattern) {
        return getEligibleFiles(path, pattern, true);
    }

    protected SortedMap<Integer, Path> getEligibleFiles(String path, String logfilePattern, boolean isAscending) {
        TreeMap<Integer, Path> eligibleFiles = new TreeMap<>();
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent == null) {
            parent = new File(".");
        } else {
            parent.mkdirs();
        }
        if (!logfilePattern.contains("%i")) {
            return eligibleFiles;
        }
        Path dir = parent.toPath();
        String fileName = file.getName();
        int suffixLength = suffixLength(fileName);
        if (suffixLength > 0) {
            fileName = fileName.substring(0, fileName.length() - suffixLength) + ".*";
        }
        String filePattern = fileName.replace(NotANumber.VALUE, "(\\d+)");
        Pattern pattern = Pattern.compile(filePattern);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry: stream) {
                Matcher matcher = pattern.matcher(entry.toFile().getName());
                if (matcher.matches()) {
                    Integer index = Integer.parseInt(matcher.group(1));
                    eligibleFiles.put(index, entry);
                }
            }
        } catch (IOException ioe) {
            throw new LoggingException("Error reading folder " + dir + " " + ioe.getMessage(), ioe);
        }
        return isAscending? eligibleFiles : eligibleFiles.descendingMap();
    }
}
