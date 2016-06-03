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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.CommonsCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.CompositeAction;
import org.apache.logging.log4j.core.appender.rolling.action.FileRenameAction;
import org.apache.logging.log4j.core.appender.rolling.action.GzCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.ZipCompressAction;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;

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
@Plugin(name = "DefaultRolloverStrategy", category = "Core", printObject = true)
public class DefaultRolloverStrategy implements RolloverStrategy {

    /**
     * Enumerates over supported file extensions.
     * <p>
     * Package-protected for unit tests.
     */
    static enum FileExtensions {
        ZIP(".zip") {
            @Override
            Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                    final int compressionLevel) {
                return new ZipCompressAction(source(renameTo), target(compressedName), deleteSource, compressionLevel);
            }
        },
        GZ(".gz") {
            @Override
            Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                    final int compressionLevel) {
                return new GzCompressAction(source(renameTo), target(compressedName), deleteSource);
            }
        },
        BZIP2(".bz2") {
            @Override
            Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                    final int compressionLevel) {
                // One of "gz", "bzip2", "xz", "pack200", or "deflate".
                return new CommonsCompressAction("bzip2", source(renameTo), target(compressedName), deleteSource);
            }
        },
        DEFLATE(".deflate") {
            @Override
            Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                    final int compressionLevel) {
                // One of "gz", "bzip2", "xz", "pack200", or "deflate".
                return new CommonsCompressAction("deflate", source(renameTo), target(compressedName), deleteSource);
            }
        },
        PACK200(".pack200") {
            @Override
            Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                    final int compressionLevel) {
                // One of "gz", "bzip2", "xz", "pack200", or "deflate".
                return new CommonsCompressAction("pack200", source(renameTo), target(compressedName), deleteSource);
            }
        },
        XZ(".xz") {
            @Override
            Action createCompressAction(final String renameTo, final String compressedName, final boolean deleteSource,
                    final int compressionLevel) {
                // One of "gz", "bzip2", "xz", "pack200", or "deflate".
                return new CommonsCompressAction("xz", source(renameTo), target(compressedName), deleteSource);
            }
        };

        static FileExtensions lookup(final String fileExtension) {
            for (final FileExtensions ext : values()) {
                if (ext.isExtensionFor(fileExtension)) {
                    return ext;
                }
            }
            return null;
        }

        private final String extension;

        private FileExtensions(final String extension) {
            Objects.requireNonNull(extension, "extension");
            this.extension = extension;
        }

        abstract Action createCompressAction(String renameTo, String compressedName, boolean deleteSource,
                int compressionLevel);

        String getExtension() {
            return extension;
        }

        boolean isExtensionFor(final String s) {
            return s.endsWith(this.extension);
        }

        int length() {
            return extension.length();
        }

        File source(final String fileName) {
            return new File(fileName);
        }

        File target(final String fileName) {
            return new File(fileName);
        }
    };

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private static final int MIN_WINDOW_SIZE = 1;
    private static final int DEFAULT_WINDOW_SIZE = 7;

    /**
     * Create the DefaultRolloverStrategy.
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
        final boolean useMax = fileIndex == null ? true : fileIndex.equalsIgnoreCase("max");
        int minIndex = MIN_WINDOW_SIZE;
        if (min != null) {
            minIndex = Integer.parseInt(min);
            if (minIndex < 1) {
                LOGGER.error("Minimum window size too small. Limited to " + MIN_WINDOW_SIZE);
                minIndex = MIN_WINDOW_SIZE;
            }
        }
        int maxIndex = DEFAULT_WINDOW_SIZE;
        if (max != null) {
            maxIndex = Integer.parseInt(max);
            if (maxIndex < minIndex) {
                maxIndex = minIndex < DEFAULT_WINDOW_SIZE ? DEFAULT_WINDOW_SIZE : minIndex;
                LOGGER.error("Maximum window size must be greater than the minimum windows size. Set to " + maxIndex);
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
    private final StrSubstitutor strSubstitutor;
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
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
        this.useMax = useMax;
        this.compressionLevel = compressionLevel;
        this.strSubstitutor = strSubstitutor;
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

    public StrSubstitutor getStrSubstitutor() {
        return strSubstitutor;
    }

    public boolean isStopCustomActionsOnError() {
        return stopCustomActionsOnError;
    }

    public boolean isUseMax() {
        return useMax;
    }

    private Action merge(final Action compressAction, final List<Action> custom, final boolean stopOnError) {
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

    private int purge(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        return useMax ? purgeAscending(lowIndex, highIndex, manager) : purgeDescending(lowIndex, highIndex, manager);
    }

    /**
     * Purge and rename old log files in preparation for rollover. The oldest file will have the smallest index, the
     * newest the highest.
     *
     * @param lowIndex low index
     * @param highIndex high index. Log file associated with high index will be deleted if needed.
     * @param manager The RollingFileManager
     * @return true if purge was successful and rollover should be attempted.
     */
    private int purgeAscending(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        final List<FileRenameAction> renames = new ArrayList<>();
        final StringBuilder buf = new StringBuilder();

        // LOG4J2-531: directory scan & rollover must use same format
        manager.getPatternProcessor().formatFileName(strSubstitutor, buf, highIndex);
        String highFilename = strSubstitutor.replace(buf);
        final int suffixLength = suffixLength(highFilename);
        int curMaxIndex = 0;

        for (int i = highIndex; i >= lowIndex; i--) {
            File toRename = new File(highFilename);
            if (i == highIndex && toRename.exists()) {
                curMaxIndex = highIndex;
            } else if (curMaxIndex == 0 && toRename.exists()) {
                curMaxIndex = i + 1;
                break;
            }

            boolean isBase = false;

            if (suffixLength > 0) {
                final File toRenameBase = new File(highFilename.substring(0, highFilename.length() - suffixLength));

                if (toRename.exists()) {
                    if (toRenameBase.exists()) {
                        LOGGER.debug("DefaultRolloverStrategy.purgeAscending deleting {} base of {}.", //
                                toRenameBase, toRename);
                        toRenameBase.delete();
                    }
                } else {
                    toRename = toRenameBase;
                    isBase = true;
                }
            }

            if (toRename.exists()) {
                //
                // if at lower index and then all slots full
                // attempt to delete last file
                // if that fails then abandon purge
                if (i == lowIndex) {
                    LOGGER.debug("DefaultRolloverStrategy.purgeAscending deleting {} at low index {}: all slots full.",
                            toRename, i);
                    if (!toRename.delete()) {
                        return -1;
                    }

                    break;
                }

                //
                // if intermediate index
                // add a rename action to the list
                buf.setLength(0);
                // LOG4J2-531: directory scan & rollover must use same format
                manager.getPatternProcessor().formatFileName(strSubstitutor, buf, i - 1);

                final String lowFilename = strSubstitutor.replace(buf);
                String renameTo = lowFilename;

                if (isBase) {
                    renameTo = lowFilename.substring(0, lowFilename.length() - suffixLength);
                }

                renames.add(new FileRenameAction(toRename, new File(renameTo), true));
                highFilename = lowFilename;
            } else {
                buf.setLength(0);
                // LOG4J2-531: directory scan & rollover must use same format
                manager.getPatternProcessor().formatFileName(strSubstitutor, buf, i - 1);

                highFilename = strSubstitutor.replace(buf);
            }
        }
        if (curMaxIndex == 0) {
            curMaxIndex = lowIndex;
        }

        //
        // work renames backwards
        //
        for (int i = renames.size() - 1; i >= 0; i--) {
            final Action action = renames.get(i);
            try {
                LOGGER.debug("DefaultRolloverStrategy.purgeAscending executing {} of {}: {}", //
                        i, renames.size(), action);
                if (!action.execute()) {
                    return -1;
                }
            } catch (final Exception ex) {
                LOGGER.warn("Exception during purge in RollingFileAppender", ex);
                return -1;
            }
        }
        return curMaxIndex;
    }

    /**
     * Purge and rename old log files in preparation for rollover. The newest file will have the smallest index, the
     * oldest will have the highest.
     *
     * @param lowIndex low index
     * @param highIndex high index. Log file associated with high index will be deleted if needed.
     * @param manager The RollingFileManager
     * @return true if purge was successful and rollover should be attempted.
     */
    private int purgeDescending(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        final List<FileRenameAction> renames = new ArrayList<>();
        final StringBuilder buf = new StringBuilder();

        // LOG4J2-531: directory scan & rollover must use same format
        manager.getPatternProcessor().formatFileName(strSubstitutor, buf, lowIndex);

        String lowFilename = strSubstitutor.replace(buf);
        final int suffixLength = suffixLength(lowFilename);

        for (int i = lowIndex; i <= highIndex; i++) {
            File toRename = new File(lowFilename);
            boolean isBase = false;

            if (suffixLength > 0) {
                final File toRenameBase = new File(lowFilename.substring(0, lowFilename.length() - suffixLength));

                if (toRename.exists()) {
                    if (toRenameBase.exists()) {
                        LOGGER.debug("DefaultRolloverStrategy.purgeDescending deleting {} base of {}.", //
                                toRenameBase, toRename);
                        toRenameBase.delete();
                    }
                } else {
                    toRename = toRenameBase;
                    isBase = true;
                }
            }

            if (toRename.exists()) {
                //
                // if at upper index then
                // attempt to delete last file
                // if that fails then abandon purge
                if (i == highIndex) {
                    LOGGER.debug(
                            "DefaultRolloverStrategy.purgeDescending deleting {} at high index {}: all slots full.", //
                            toRename, i);
                    if (!toRename.delete()) {
                        return -1;
                    }

                    break;
                }

                //
                // if intermediate index
                // add a rename action to the list
                buf.setLength(0);
                // LOG4J2-531: directory scan & rollover must use same format
                manager.getPatternProcessor().formatFileName(strSubstitutor, buf, i + 1);

                final String highFilename = strSubstitutor.replace(buf);
                String renameTo = highFilename;

                if (isBase) {
                    renameTo = highFilename.substring(0, highFilename.length() - suffixLength);
                }

                renames.add(new FileRenameAction(toRename, new File(renameTo), true));
                lowFilename = highFilename;
            } else {
                break;
            }
        }

        //
        // work renames backwards
        //
        for (int i = renames.size() - 1; i >= 0; i--) {
            final Action action = renames.get(i);
            try {
                LOGGER.debug("DefaultRolloverStrategy.purgeDescending executing {} of {}: {}", //
                        i, renames.size(), action);
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
     * Perform the rollover.
     * 
     * @param manager The RollingFileManager name for current active log file.
     * @return A RolloverDescription.
     * @throws SecurityException if an error occurs.
     */
    @Override
    public RolloverDescription rollover(final RollingFileManager manager) throws SecurityException {
        if (maxIndex < 0) {
            return null;
        }
        final long startNanos = System.nanoTime();
        final int fileIndex = purge(minIndex, maxIndex, manager);
        if (fileIndex < 0) {
            return null;
        }
        if (LOGGER.isTraceEnabled()) {
            final double durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            LOGGER.trace("DefaultRolloverStrategy.purge() took {} milliseconds", durationMillis);
        }
        final StringBuilder buf = new StringBuilder(255);
        manager.getPatternProcessor().formatFileName(strSubstitutor, buf, fileIndex);
        final String currentFileName = manager.getFileName();

        String renameTo = buf.toString();
        final String compressedName = renameTo;
        Action compressAction = null;

        for (final FileExtensions ext : FileExtensions.values()) { // LOG4J2-1077 support other compression formats
            if (ext.isExtensionFor(renameTo)) {
                renameTo = renameTo.substring(0, renameTo.length() - ext.length()); // LOG4J2-1135 omit extension!
                compressAction = ext.createCompressAction(renameTo, compressedName, true, compressionLevel);
                break;
            }
        }

        final FileRenameAction renameAction = new FileRenameAction(new File(currentFileName), new File(renameTo), false);

        final Action asyncAction = merge(compressAction, customActions, stopCustomActionsOnError);
        return new RolloverDescriptionImpl(currentFileName, false, renameAction, asyncAction);
    }

    private int suffixLength(final String lowFilename) {
        for (final FileExtensions extension : FileExtensions.values()) {
            if (extension.isExtensionFor(lowFilename)) {
                return extension.length();
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "DefaultRolloverStrategy(min=" + minIndex + ", max=" + maxIndex + ')';
    }

}
