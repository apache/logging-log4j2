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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.FileRenameAction;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Integers;

/**
 * When rolling over, <code>DefaultRolloverStrategy</code> renames files according to an algorithm as described below.
 *
 * <p>
 * The DefaultRolloverStrategy is a combination of a time-based policy and a fixed-window policy. When the file name
 * pattern contains a date format then the rollover time interval will be used to calculate the time to use in the file
 * pattern. When the file pattern contains an integer replacement token one of the counting techniques will be used.
 * </p>
 * <p>
 * When the ascending attribute is set to true (the default) then the counter will be incremented and the current log
 * file will be renamed to include the counter value. If the counter hits the maximum value then the oldest file, which
 * will have the smallest counter, will be deleted, all other files will be renamed to have their counter decremented
 * and then the current file will be renamed to have the maximum counter value. Note that with this counting strategy
 * specifying a large maximum value may entirely avoid renaming files.
 * </p>
 * <p>
 * When the ascending attribute is false, then the "normal" fixed-window strategy will be used.
 * </p>
 * <p>
 * Let <em>max</em> and <em>min</em> represent the values of respectively the <b>MaxIndex</b> and <b>MinIndex</b>
 * options. Let "foo.log" be the value of the <b>ActiveFile</b> option and "foo.%i.log" the value of
 * <b>FileNamePattern</b>. Then, when rolling over, the file <code>foo.<em>max</em>.log</code> will be deleted, the file
 * <code>foo.<em>max-1</em>.log</code> will be renamed as <code>foo.<em>max</em>.log</code>, the file
 * <code>foo.<em>max-2</em>.log</code> renamed as <code>foo.<em>max-1</em>.log</code>, and so on, the file
 * <code>foo.<em>min+1</em>.log</code> renamed as <code>foo.<em>min+2</em>.log</code>. Lastly, the active file
 * <code>foo.log</code> will be renamed as <code>foo.<em>min</em>.log</code> and a new active file name
 * <code>foo.log</code> will be created.
 * </p>
 * <p>
 * Given that this rollover algorithm requires as many file renaming operations as the window size, large window sizes
 * are discouraged.
 * </p>
 */
@Plugin(name = "DefaultRolloverStrategy", category = Core.CATEGORY_NAME, printObject = true)
public class DefaultRolloverStrategy extends AbstractRolloverStrategy {

    private static final int MIN_WINDOW_SIZE = 1;
    private static final int DEFAULT_WINDOW_SIZE = 7;

    /**
     * Creates the DefaultRolloverStrategy.
     *
     * @param max The maximum number of files to keep.
     * @param min The minimum number of files to keep.
     * @param fileIndex If set to "max" (the default), files with a higher index will be newer than files with a smaller
     *            index. If set to "min", file renaming and the counter will follow the Fixed Window strategy.
     * @param compressionLevelStr The compression level, 0 (less) through 9 (more); applies only to ZIP files.
     * @param customActions custom actions to perform asynchronously after rollover
     * @param stopCustomActionsOnError whether to stop executing asynchronous actions if an error occurs
     * @param config The Configuration.
     * @return A DefaultRolloverStrategy.
     */
    @PluginFactory
    public static DefaultRolloverStrategy createStrategy(
            // @formatter:off
            @PluginAttribute("max") final String max,
            @PluginAttribute("min") final String min,
            @PluginAttribute("fileIndex") final String fileIndex,
            @PluginAttribute("compressionLevel") final String compressionLevelStr,
            @PluginElement("Actions") final Action[] customActions,
            @PluginAttribute(value = "stopCustomActionsOnError", defaultBoolean = true)
                    final boolean stopCustomActionsOnError,
            @PluginConfiguration final Configuration config) {
            // @formatter:on
        int minIndex;
        int maxIndex;
        boolean useMax;

        if (fileIndex != null && fileIndex.equalsIgnoreCase("nomax")) {
            minIndex = Integer.MIN_VALUE;
            maxIndex = Integer.MAX_VALUE;
            useMax = false;
        } else {
            useMax = fileIndex == null ? true : fileIndex.equalsIgnoreCase("max");
            minIndex = MIN_WINDOW_SIZE;
            if (min != null) {
                minIndex = Integer.parseInt(min);
                if (minIndex < 1) {
                    LOGGER.error("Minimum window size too small. Limited to " + MIN_WINDOW_SIZE);
                    minIndex = MIN_WINDOW_SIZE;
                }
            }
            maxIndex = DEFAULT_WINDOW_SIZE;
            if (max != null) {
                maxIndex = Integer.parseInt(max);
                if (maxIndex < minIndex) {
                    maxIndex = minIndex < DEFAULT_WINDOW_SIZE ? DEFAULT_WINDOW_SIZE : minIndex;
                    LOGGER.error("Maximum window size must be greater than the minimum windows size. Set to " + maxIndex);
                }
            }
        }
        final int compressionLevel = Integers.parseInt(compressionLevelStr, Deflater.DEFAULT_COMPRESSION);
        return new DefaultRolloverStrategy(minIndex, maxIndex, useMax, compressionLevel, config.getStrSubstitutor(),
                customActions, stopCustomActionsOnError);
    }

    /**
     * Index for oldest retained log file.
     */
    private final int maxIndex;

    /**
     * Index for most recent log file.
     */
    private final int minIndex;
    private final boolean useMax;
    private final int compressionLevel;
    private final List<Action> customActions;
    private final boolean stopCustomActionsOnError;

    /**
     * Constructs a new instance.
     *
     * @param minIndex The minimum index.
     * @param maxIndex The maximum index.
     * @param customActions custom actions to perform asynchronously after rollover
     * @param stopCustomActionsOnError whether to stop executing asynchronous actions if an error occurs
     */
    protected DefaultRolloverStrategy(final int minIndex, final int maxIndex, final boolean useMax,
            final int compressionLevel, final StrSubstitutor strSubstitutor, final Action[] customActions,
            final boolean stopCustomActionsOnError) {
        super(strSubstitutor);
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
        this.useMax = useMax;
        this.compressionLevel = compressionLevel;
        this.stopCustomActionsOnError = stopCustomActionsOnError;
        this.customActions = customActions == null ? Collections.<Action> emptyList() : Arrays.asList(customActions);
    }

    public int getCompressionLevel() {
        return this.compressionLevel;
    }

    public List<Action> getCustomActions() {
        return customActions;
    }

    public int getMaxIndex() {
        return this.maxIndex;
    }

    public int getMinIndex() {
        return this.minIndex;
    }

    public boolean isStopCustomActionsOnError() {
        return stopCustomActionsOnError;
    }

    public boolean isUseMax() {
        return useMax;
    }

    private int purge(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        return useMax ? purgeAscending(lowIndex, highIndex, manager) : purgeDescending(lowIndex, highIndex, manager);
    }

    /**
     * Purges and renames old log files in preparation for rollover. The oldest file will have the smallest index, the
     * newest the highest.
     *
     * @param lowIndex low index. Log file associated with low index will be deleted if needed.
     * @param highIndex high index.
     * @param manager The RollingFileManager
     * @return true if purge was successful and rollover should be attempted.
     */
    private int purgeAscending(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        final SortedMap<Integer, Path> eligibleFiles = getEligibleFiles(manager);
        final int maxFiles = highIndex - lowIndex + 1;

        boolean renameFiles = false;
        while (eligibleFiles.size() >= maxFiles) {
            try {
                LOGGER.debug("Eligible files: {}", eligibleFiles);
                Integer key = eligibleFiles.firstKey();
                LOGGER.debug("Deleting {}", eligibleFiles.get(key).toFile().getAbsolutePath());
                Files.delete(eligibleFiles.get(key));
                eligibleFiles.remove(key);
                renameFiles = true;
            } catch (IOException ioe) {
                LOGGER.error("Unable to delete {}, {}", eligibleFiles.firstKey(), ioe.getMessage(), ioe);
                break;
            }
        }
        final StringBuilder buf = new StringBuilder();
        if (renameFiles) {
            for (Map.Entry<Integer, Path> entry : eligibleFiles.entrySet()) {
                buf.setLength(0);
                // LOG4J2-531: directory scan & rollover must use same format
                manager.getPatternProcessor().formatFileName(strSubstitutor, buf, entry.getKey() - 1);
                String currentName = entry.getValue().toFile().getName();
                String renameTo = buf.toString();
                int suffixLength = suffixLength(renameTo);
                if (suffixLength > 0 && suffixLength(currentName) == 0) {
                   renameTo = renameTo.substring(0, renameTo.length() - suffixLength);
                }
                Action action = new FileRenameAction(entry.getValue().toFile(), new File(renameTo), true);
                try {
                    LOGGER.debug("DefaultRolloverStrategy.purgeAscending executing {}", action);
                    if (!action.execute()) {
                        return -1;
                    }
                } catch (final Exception ex) {
                    LOGGER.warn("Exception during purge in RollingFileAppender", ex);
                    return -1;
                }
            }
        }

        return eligibleFiles.size() > 0 ?
                (eligibleFiles.lastKey() < highIndex ? eligibleFiles.lastKey() + 1 : highIndex) : lowIndex;
    }

    /**
     * Purges and renames old log files in preparation for rollover. The newest file will have the smallest index, the
     * oldest will have the highest.
     *
     * @param lowIndex low index
     * @param highIndex high index. Log file associated with high index will be deleted if needed.
     * @param manager The RollingFileManager
     * @return true if purge was successful and rollover should be attempted.
     */
    private int purgeDescending(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        // Retrieve the files in descending order, so the highest key will be first.
        final SortedMap<Integer, Path> eligibleFiles = getEligibleFiles(manager, false);
        final int maxFiles = highIndex - lowIndex + 1;

        while (eligibleFiles.size() >= maxFiles) {
            try {
                Integer key = eligibleFiles.firstKey();
                Files.delete(eligibleFiles.get(key));
                eligibleFiles.remove(key);
            } catch (IOException ioe) {
                LOGGER.error("Unable to delete {}, {}", eligibleFiles.firstKey(), ioe.getMessage(), ioe);
                break;
            }
        }
        final StringBuilder buf = new StringBuilder();
        for (Map.Entry<Integer, Path> entry : eligibleFiles.entrySet()) {
            buf.setLength(0);
            // LOG4J2-531: directory scan & rollover must use same format
            manager.getPatternProcessor().formatFileName(strSubstitutor, buf, entry.getKey() + 1);
            String currentName = entry.getValue().toFile().getName();
            String renameTo = buf.toString();
            int suffixLength = suffixLength(renameTo);
            if (suffixLength > 0 && suffixLength(currentName) == 0) {
                renameTo = renameTo.substring(0, renameTo.length() - suffixLength);
            }
            Action action = new FileRenameAction(entry.getValue().toFile(), new File(renameTo), true);
            try {
                LOGGER.debug("DefaultRolloverStrategy.purgeDescending executing {}", action);
                if (!action.execute()) {
                    return -1;
                }
            } catch (final Exception ex) {
                LOGGER.warn("Exception during purge in RollingFileAppender", ex);
                return -1;
            }
        }

        return lowIndex;
    }

    /**
     * Performs the rollover.
     *
     * @param manager The RollingFileManager name for current active log file.
     * @return A RolloverDescription.
     * @throws SecurityException if an error occurs.
     */
    @Override
    public RolloverDescription rollover(final RollingFileManager manager) throws SecurityException {
        int fileIndex;
        if (minIndex == Integer.MIN_VALUE) {
            final SortedMap<Integer, Path> eligibleFiles = getEligibleFiles(manager);
            fileIndex = eligibleFiles.size() > 0 ? eligibleFiles.lastKey() + 1 : 1;
        } else {
            if (maxIndex < 0) {
                return null;
            }
            final long startNanos = System.nanoTime();
            fileIndex = purge(minIndex, maxIndex, manager);
            if (fileIndex < 0) {
                return null;
            }
            if (LOGGER.isTraceEnabled()) {
                final double durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                LOGGER.trace("DefaultRolloverStrategy.purge() took {} milliseconds", durationMillis);
            }
        }
        final StringBuilder buf = new StringBuilder(255);
        manager.getPatternProcessor().formatFileName(strSubstitutor, buf, fileIndex);
        final String currentFileName = manager.getFileName();

        String renameTo = buf.toString();
        final String compressedName = renameTo;
        Action compressAction = null;

        FileExtension fileExtension = manager.getFileExtension();
        if (fileExtension != null) {
            renameTo = renameTo.substring(0, renameTo.length() - fileExtension.length());
            compressAction = fileExtension.createCompressAction(renameTo, compressedName,
                    true, compressionLevel);
        }

        if (currentFileName.equals(renameTo)) {
            LOGGER.warn("Attempt to rename file {} to itself will be ignored", currentFileName);
            return new RolloverDescriptionImpl(currentFileName, false, null, null);
        }

        final FileRenameAction renameAction = new FileRenameAction(new File(currentFileName), new File(renameTo),
                    manager.isRenameEmptyFiles());

        final Action asyncAction = merge(compressAction, customActions, stopCustomActionsOnError);
        return new RolloverDescriptionImpl(currentFileName, false, renameAction, asyncAction);
    }

    @Override
    public String toString() {
        return "DefaultRolloverStrategy(min=" + minIndex + ", max=" + maxIndex + ", useMax=" + useMax + ")";
    }

}
