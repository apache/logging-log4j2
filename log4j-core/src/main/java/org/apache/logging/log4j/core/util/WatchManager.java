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
package org.apache.logging.log4j.core.util;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Manages {@link FileWatcher}s.
 * 
 * @see FileWatcher
 * @see ConfigurationScheduler
 */
public class WatchManager extends AbstractLifeCycle {

    private static Logger logger = StatusLogger.getLogger();
    private final ConcurrentMap<File, FileMonitor> watchers = new ConcurrentHashMap<>();
    private int intervalSeconds = 0;
    private ScheduledFuture<?> future;
    private final ConfigurationScheduler scheduler;

    public WatchManager(final ConfigurationScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Resets all file monitors to their current last modified time. If this manager does not watch any file, nothing
     * happens.
     * <p>
     * This allows you to start, stop, reset and start again a manager, without triggering file modified events if the a
     * watched file has changed during the period of time when the manager was stopped.
     * </p>
     * 
     * @since 2.11.0
     */
    public void reset() {
        logger.debug("Resetting {}", this);
        for (final File file : watchers.keySet()) {
            reset(file);
        }
    }

    /**
     * Resets the file monitor for the given file being watched to its current last modified time. If this manager does
     * not watch the given file, nothing happens.
     * <p>
     * This allows you to start, stop, reset and start again a manager, without triggering file modified events if the
     * given watched file has changed during the period of time when the manager was stopped.
     * </p>
     * 
     * @param file
     *            the file for the monitor to reset.
     * @since 2.11.0
     */
    public void reset(final File file) {
        if (file == null) {
            return;
        }
        final FileMonitor fileMonitor = watchers.get(file);
        if (fileMonitor != null) {
            final long lastModifiedMillis = file.lastModified();
            if (lastModifiedMillis != fileMonitor.lastModifiedMillis) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Resetting file monitor for '{}' from {} ({}) to {} ({})", file,
                            millisToString(fileMonitor.lastModifiedMillis), fileMonitor.lastModifiedMillis,
                            millisToString(lastModifiedMillis), lastModifiedMillis);
                }
                fileMonitor.setLastModifiedMillis(lastModifiedMillis);
            }
        }
    }

    public void setIntervalSeconds(final int intervalSeconds) {
        if (!isStarted()) {
            if (this.intervalSeconds > 0 && intervalSeconds == 0) {
                scheduler.decrementScheduledItems();
            } else if (this.intervalSeconds == 0 && intervalSeconds > 0) {
                scheduler.incrementScheduledItems();
            }
            this.intervalSeconds = intervalSeconds;
        }
    }

    /**
     * Gets how often this manager checks for file modifications.
     * 
     * @return how often, in seconds, this manager checks for file modifications.
     */
    public int getIntervalSeconds() {
        return this.intervalSeconds;
    }

    @Override
    public void start() {
        super.start();
        if (intervalSeconds > 0) {
            future = scheduler.scheduleWithFixedDelay(new WatchRunnable(), intervalSeconds, intervalSeconds,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        final boolean stopped = stop(future);
        setStopped();
        return stopped;
    }

    /**
     * Unwatches the given file.
     * 
     * @param file
     *            the file to stop watching.
     * @since 2.11.0
     */
    public void unwatchFile(final File file) {
        logger.debug("Unwatching file '{}'", file);
        watchers.remove(file);
    }

    /**
     * Watches the given file.
     * 
     * @param file
     *            the file to watch.
     * @param watcher
     *            the watcher to notify of file changes.
     */
    public void watchFile(final File file, final FileWatcher watcher) {
        final long lastModified = file.lastModified();
        if (logger.isDebugEnabled()) {
            logger.debug("Watching file '{}' for lastModified {} ({})", file, millisToString(lastModified), lastModified);
        }
        watchers.put(file, new FileMonitor(lastModified, watcher));
    }

    public Map<File, FileWatcher> getWatchers() {
        final Map<File, FileWatcher> map = new HashMap<>(watchers.size());
        for (final Map.Entry<File, FileMonitor> entry : watchers.entrySet()) {
            map.put(entry.getKey(), entry.getValue().fileWatcher);
        }
        return map;
    }

    private String millisToString(final long millis) {
        return new Date(millis).toString();
    }
    
    private final class WatchRunnable implements Runnable {

        // Use a hard class reference here in case a refactoring changes the class name.
        private final String SIMPLE_NAME = WatchRunnable.class.getSimpleName();

        @Override
        public void run() {
            logger.trace("{} run triggered.", SIMPLE_NAME);
            for (final Map.Entry<File, FileMonitor> entry : watchers.entrySet()) {
                final File file = entry.getKey();
                final FileMonitor fileMonitor = entry.getValue();
                final long lastModfied = file.lastModified();
                if (fileModified(fileMonitor, lastModfied)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("File '{}' was modified on {} ({}), previous modification was on {} ({})", file,
                                millisToString(lastModfied), lastModfied, millisToString(fileMonitor.lastModifiedMillis),
                                fileMonitor.lastModifiedMillis);
                    }
                    fileMonitor.lastModifiedMillis = lastModfied;
                    fileMonitor.fileWatcher.fileModified(file);
                }
            }
            logger.trace("{} run ended.", SIMPLE_NAME);
        }

        private boolean fileModified(final FileMonitor fileMonitor, final long lastModifiedMillis) {
            return lastModifiedMillis != fileMonitor.lastModifiedMillis;
        }
    }

    private final class FileMonitor {
        private final FileWatcher fileWatcher;
        private volatile long lastModifiedMillis;

        public FileMonitor(final long lastModifiedMillis, final FileWatcher fileWatcher) {
            this.fileWatcher = fileWatcher;
            this.lastModifiedMillis = lastModifiedMillis;
        }

        private void setLastModifiedMillis(final long lastModifiedMillis) {
            this.lastModifiedMillis = lastModifiedMillis;
        }

        @Override
        public String toString() {
            return "FileMonitor [fileWatcher=" + fileWatcher + ", lastModifiedMillis=" + lastModifiedMillis + "]";
        }

    }

    @Override
    public String toString() {
        return "WatchManager [intervalSeconds=" + intervalSeconds + ", watchers=" + watchers + ", scheduler="
                + scheduler + ", future=" + future + "]";
    }
}
