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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rolling.helper.Action;
import org.apache.logging.log4j.core.appender.rolling.helper.FileRenameAction;
import org.apache.logging.log4j.core.appender.rolling.helper.GZCompressAction;
import org.apache.logging.log4j.core.appender.rolling.helper.ZipCompressAction;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * When rolling over, <code>FixedWindowRollingPolicy</code> renames files
 * according to a fixed window algorithm as described below.
 * <p/>
 * <p>The <b>ActiveFileName</b> property, which is required, represents the name
 * of the file where current logging output will be written.
 * The <b>FileNamePattern</b>  option represents the file name pattern for the
 * archived (rolled over) log files. If present, the <b>FileNamePattern</b>
 * option must include an integer token, that is the string "%i" somewhere
 * within the pattern.
 * <p/>
 * <p>Let <em>max</em> and <em>min</em> represent the values of respectively
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
 * <p/>
 * <p>Given that this rollover algorithm requires as many file renaming
 * operations as the window size, large window sizes are discouraged. The
 * current implementation will automatically reduce the window size to 12 when
 * larger values are specified by the user.
 */
@Plugin(name = "DefaultRolloverStrategy", type = "Core", printObject = true)
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

    private final StrSubstitutor subst;

    /**
     * Constructs a new instance.
     * @param min The minimum index.
     * @param max The maximum index.
     */
    protected DefaultRolloverStrategy(int min, int max, StrSubstitutor subst) {
        minIndex = min;
        maxIndex = max;
        this.subst = subst;
    }

    /**
     * Perform the rollover.
     * @param manager The RollingFileManager name for current active log file.
     * @return A RolloverDescription.
     * @throws SecurityException if an error occurs.
     */
    public RolloverDescription rollover(RollingFileManager manager) throws SecurityException {
        if (maxIndex >= 0) {
            int purgeStart = minIndex;

            if (!purge(purgeStart, maxIndex, manager)) {
                return null;
            }

            StringBuilder buf = new StringBuilder();
            manager.getProcessor().formatFileName(purgeStart, buf);
            String currentFileName = manager.getFileName();

            String renameTo = subst.replace(buf);
            String compressedName = renameTo;
            Action compressAction = null;

            if (renameTo.endsWith(".gz")) {
                renameTo = renameTo.substring(0, renameTo.length() - 3);
                compressAction = new GZCompressAction(new File(renameTo), new File(compressedName), true);
            } else if (renameTo.endsWith(".zip")) {
                renameTo = renameTo.substring(0, renameTo.length() - 4);
                compressAction = new ZipCompressAction(new File(renameTo), new File(compressedName), true);
            }

            FileRenameAction renameAction =
                new FileRenameAction(new File(currentFileName), new File(renameTo), false);

            return new RolloverDescriptionImpl(currentFileName, false, renameAction, compressAction);
        }

        return null;
    }

    /**
     * Purge and rename old log files in preparation for rollover
     *
     * @param lowIndex  low index
     * @param highIndex high index.  Log file associated with high index will be deleted if needed.
     * @param manager The RollingFileManager
     * @return true if purge was successful and rollover should be attempted.
     */
    private boolean purge(final int lowIndex, final int highIndex, RollingFileManager manager) {
        int suffixLength = 0;

        List<FileRenameAction> renames = new ArrayList<FileRenameAction>();
        StringBuilder buf = new StringBuilder();
        manager.getProcessor().formatFileName(lowIndex, buf);

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
                File toRenameBase =
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
                        return false;
                    }

                    break;
                }

                //
                //   if intermediate index
                //     add a rename action to the list
                buf.setLength(0);
                manager.getProcessor().formatFileName(i + 1, buf);

                String highFilename = subst.replace(buf);
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
            Action action = renames.get(i);

            try {
                if (!action.execute()) {
                    return false;
                }
            } catch (Exception ex) {
                LOGGER.warn("Exception during purge in RollingFileAppender", ex);
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "DefaultRolloverStrategy(min=" + minIndex + ", max=" + maxIndex + ")";
    }

    /**
     * Create the DefaultRolloverStrategy.
     * @param max The maximum number of files to keep.
     * @param min The minimum number of files to keep.
     * @param config The Configuration.
     * @return A DefaultRolloverStrategy.
     */
    @PluginFactory
    public static DefaultRolloverStrategy createStrategy(@PluginAttr("max") String max,
                                                         @PluginAttr("min") String min,
                                                         @PluginConfiguration Configuration config) {

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
        return new DefaultRolloverStrategy(minIndex, maxIndex, config.getSubst());
    }

}
