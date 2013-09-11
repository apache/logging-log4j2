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
import java.util.List;
import java.util.zip.Deflater;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.helper.Action;
import org.apache.logging.log4j.core.appender.rolling.helper.FileRenameAction;
import org.apache.logging.log4j.core.appender.rolling.helper.GZCompressAction;
import org.apache.logging.log4j.core.appender.rolling.helper.ZipCompressAction;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Integers;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * When rolling over, <code>DefaultRolloverStrategy</code> renames files
 * according to an algorithm as described below.
 * 
 * <p>
 * The DefaultRolloverStrategy is a combination of a time-based policy and a fixed-window policy. When
 * the file name pattern contains a date format then the rollover time interval will be used to calculate the
 * time to use in the file pattern. When the file pattern contains an integer replacement token one of the
 * counting techniques will be used.
 * </p>
 * <p>
 * When the ascending attribute is set to true (the default) then the counter will be incremented and the
 * current log file will be renamed to include the counter value. If the counter hits the maximum value then
 * the oldest file, which will have the smallest counter, will be deleted, all other files will be renamed to
 * have their counter decremented and then the current file will be renamed to have the maximum counter value.
 * Note that with this counting strategy specifying a large maximum value may entirely avoid renaming files.
 * </p>
 * <p>
 * When the ascending attribute is false, then the "normal" fixed-window strategy will be used.
 * </p>
 * <p>
 * Let <em>max</em> and <em>min</em> represent the values of respectively
 * the <b>MaxIndex</b> and <b>MinIndex</b> options. Let "foo.log" be the value
 * of the <b>ActiveFile</b> option and "foo.%i.log" the value of
 * <b>FileNamePattern</b>. Then, when rolling over, the file
 * <code>foo.<em>max</em>.log</code> will be deleted, the file
 * <code>foo.<em>max-1</em>.log</code> will be renamed as
 * <code>foo.<em>max</em>.log</code>, the file <code>foo.<em>max-2</em>.log</code>
 * renamed as <code>foo.<em>max-1</em>.log</code>, and so on,
 * the file <code>foo.<em>min+1</em>.log</code> renamed as
 * <code>foo.<em>min+2</em>.log</code>. Lastly, the active file <code>foo.log</code>
 * will be renamed as <code>foo.<em>min</em>.log</code> and a new active file name
 * <code>foo.log</code> will be created.
 * </p>
 * <p>
 * Given that this rollover algorithm requires as many file renaming
 * operations as the window size, large window sizes are discouraged.
 * </p>
 */
@Plugin(name = "DefaultRolloverStrategy", category = "Core", printObject = true)
public class DefaultRolloverStrategy implements RolloverStrategy {
    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private static final int MIN_WINDOW_SIZE = 1;
    private static final int DEFAULT_WINDOW_SIZE = 7;

    /**
     * Index for oldest retained log file.
     */
    private final int maxIndex;

    /**
     * Index for most recent log file.
     */
    private final int minIndex;

    private final boolean useMax;

    private final StrSubstitutor subst;
    
    private final int compressionLevel;

    /**
     * Constructs a new instance.
     * @param minIndex The minimum index.
     * @param maxIndex The maximum index.
     */
    protected DefaultRolloverStrategy(final int minIndex, final int maxIndex, final boolean useMax, final int compressionLevel, final StrSubstitutor subst) {
        this.minIndex = minIndex;
        this.maxIndex = maxIndex;
        this.useMax = useMax;
        this.compressionLevel = compressionLevel;
        this.subst = subst;
    }

    /**
     * Perform the rollover.
     * @param manager The RollingFileManager name for current active log file.
     * @return A RolloverDescription.
     * @throws SecurityException if an error occurs.
     */
    @Override
    public RolloverDescription rollover(final RollingFileManager manager) throws SecurityException {
        if (maxIndex >= 0) {
            int fileIndex;

            if ((fileIndex = purge(minIndex, maxIndex, manager)) < 0) {
                return null;
            }

            final StringBuilder buf = new StringBuilder();
            manager.getPatternProcessor().formatFileName(subst, buf, fileIndex);
            final String currentFileName = manager.getFileName();

            String renameTo = buf.toString();
            final String compressedName = renameTo;
            Action compressAction = null;

            if (renameTo.endsWith(".gz")) {
                renameTo = renameTo.substring(0, renameTo.length() - 3);
                compressAction = new GZCompressAction(new File(renameTo), new File(compressedName), true);
            } else if (renameTo.endsWith(".zip")) {
                renameTo = renameTo.substring(0, renameTo.length() - 4);
                compressAction = new ZipCompressAction(new File(renameTo), new File(compressedName), true, 
                        compressionLevel);
            }

            final FileRenameAction renameAction =
                new FileRenameAction(new File(currentFileName), new File(renameTo), false);

            return new RolloverDescriptionImpl(currentFileName, false, renameAction, compressAction);
        }

        return null;
    }

    private int purge(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        return useMax ? purgeAscending(lowIndex, highIndex, manager) :
            purgeDescending(lowIndex, highIndex, manager);
    }

    /**
     * Purge and rename old log files in preparation for rollover. The newest file will have the smallest index, the
     * oldest will have the highest.
     *
     * @param lowIndex  low index
     * @param highIndex high index.  Log file associated with high index will be deleted if needed.
     * @param manager The RollingFileManager
     * @return true if purge was successful and rollover should be attempted.
     */
    private int purgeDescending(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        int suffixLength = 0;

        final List<FileRenameAction> renames = new ArrayList<FileRenameAction>();
        final StringBuilder buf = new StringBuilder();
        manager.getPatternProcessor().formatFileName(buf, lowIndex);

        String lowFilename = subst.replace(buf);

        if (lowFilename.endsWith(".gz")) {
            suffixLength = 3;
        } else if (lowFilename.endsWith(".zip")) {
            suffixLength = 4;
        }

        for (int i = lowIndex; i <= highIndex; i++) {
            File toRename = new File(lowFilename);
            boolean isBase = false;

            if (suffixLength > 0) {
                final File toRenameBase =
                    new File(lowFilename.substring(0, lowFilename.length() - suffixLength));

                if (toRename.exists()) {
                    if (toRenameBase.exists()) {
                        toRenameBase.delete();
                    }
                } else {
                    toRename = toRenameBase;
                    isBase = true;
                }
            }

            if (toRename.exists()) {
                //
                //    if at upper index then
                //        attempt to delete last file
                //        if that fails then abandon purge
                if (i == highIndex) {
                    if (!toRename.delete()) {
                        return -1;
                    }

                    break;
                }

                //
                //   if intermediate index
                //     add a rename action to the list
                buf.setLength(0);
                manager.getPatternProcessor().formatFileName(buf, i + 1);

                final String highFilename = subst.replace(buf);
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
        //   work renames backwards
        //
        for (int i = renames.size() - 1; i >= 0; i--) {
            final Action action = renames.get(i);

            try {
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
     * Purge and rename old log files in preparation for rollover. The oldest file will have the smallest index,
     * the newest the highest.
     *
     * @param lowIndex  low index
     * @param highIndex high index.  Log file associated with high index will be deleted if needed.
     * @param manager The RollingFileManager
     * @return true if purge was successful and rollover should be attempted.
     */
    private int purgeAscending(final int lowIndex, final int highIndex, final RollingFileManager manager) {
        int suffixLength = 0;

        final List<FileRenameAction> renames = new ArrayList<FileRenameAction>();
        final StringBuilder buf = new StringBuilder();
        manager.getPatternProcessor().formatFileName(buf, highIndex);

        String highFilename = subst.replace(buf);

        if (highFilename.endsWith(".gz")) {
            suffixLength = 3;
        } else if (highFilename.endsWith(".zip")) {
            suffixLength = 4;
        }

        int maxIndex = 0;

        for (int i = highIndex; i >= lowIndex; i--) {
            File toRename = new File(highFilename);
            if (i == highIndex && toRename.exists()) {
                maxIndex = highIndex;
            } else if (maxIndex == 0 && toRename.exists()) {
                maxIndex = i + 1;
                break;
            }

            boolean isBase = false;

            if (suffixLength > 0) {
                final File toRenameBase =
                    new File(highFilename.substring(0, highFilename.length() - suffixLength));

                if (toRename.exists()) {
                    if (toRenameBase.exists()) {
                        toRenameBase.delete();
                    }
                } else {
                    toRename = toRenameBase;
                    isBase = true;
                }
            }

            if (toRename.exists()) {
                //
                //    if at lower index and then all slots full
                //        attempt to delete last file
                //        if that fails then abandon purge
                if (i == lowIndex) {
                    if (!toRename.delete()) {
                        return -1;
                    }

                    break;
                }

                //
                //   if intermediate index
                //     add a rename action to the list
                buf.setLength(0);
                manager.getPatternProcessor().formatFileName(buf, i - 1);

                final String lowFilename = subst.replace(buf);
                String renameTo = lowFilename;

                if (isBase) {
                    renameTo = lowFilename.substring(0, lowFilename.length() - suffixLength);
                }

                renames.add(new FileRenameAction(toRename, new File(renameTo), true));
                highFilename = lowFilename;
            } else {
                buf.setLength(0);
                manager.getPatternProcessor().formatFileName(buf, i - 1);

                highFilename = subst.replace(buf);
            }
        }
        if (maxIndex == 0) {
            maxIndex = lowIndex;
        }

        //
        //   work renames backwards
        //
        for (int i = renames.size() - 1; i >= 0; i--) {
            final Action action = renames.get(i);

            try {
                if (!action.execute()) {
                    return -1;
                }
            } catch (final Exception ex) {
                LOGGER.warn("Exception during purge in RollingFileAppender", ex);
                return -1;
            }
        }
        return maxIndex;
    }

    @Override
    public String toString() {
        return "DefaultRolloverStrategy(min=" + minIndex + ", max=" + maxIndex + ")";
    }

    /**
     * Create the DefaultRolloverStrategy.
     * @param max The maximum number of files to keep.
     * @param min The minimum number of files to keep.
     * @param fileIndex If set to "max" (the default), files with a higher index will be newer than files with a
     * smaller index. If set to "min", file renaming and the counter will follow the Fixed Window strategy.
     * @param compressionLevelStr The compression level, 0 (less) through 9 (more); applies only to ZIP files.
     * @param config The Configuration.
     * @return A DefaultRolloverStrategy.
     */
    @PluginFactory
    public static DefaultRolloverStrategy createStrategy(
            @PluginAttribute("max") final String max,
            @PluginAttribute("min") final String min,
            @PluginAttribute("fileIndex") final String fileIndex,
            @PluginAttribute("compressionLevel") final String compressionLevelStr,
            @PluginConfiguration final Configuration config) {
        final boolean useMax = fileIndex == null ? true : fileIndex.equalsIgnoreCase("max");
        int minIndex;
        if (min != null) {
            minIndex = Integer.parseInt(min);
            if (minIndex < 1) {
                LOGGER.error("Minimum window size too small. Limited to " + MIN_WINDOW_SIZE);
                minIndex = MIN_WINDOW_SIZE;
            }
        } else {
            minIndex = MIN_WINDOW_SIZE;
        }
        int maxIndex;
        if (max != null) {
            maxIndex = Integer.parseInt(max);
            if (maxIndex < minIndex) {
                maxIndex = minIndex < DEFAULT_WINDOW_SIZE ? DEFAULT_WINDOW_SIZE : minIndex;
                LOGGER.error("Maximum window size must be greater than the minimum windows size. Set to " + maxIndex);
            }
        } else {
            maxIndex = DEFAULT_WINDOW_SIZE;
        }
        final int compressionLevel = Integers.parseInt(compressionLevelStr, Deflater.DEFAULT_COMPRESSION);
        return new DefaultRolloverStrategy(minIndex, maxIndex, useMax, compressionLevel, config.getStrSubstitutor());
    }

}
