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
 * Manages FileWatchers.
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

    public void watchFile(final File file, final FileWatcher watcher) {
        watchers.put(file, new FileMonitor(file.lastModified(), watcher));

    }

    public Map<File, FileWatcher> getWatchers() {
        final Map<File, FileWatcher> map = new HashMap<>();
        for (final Map.Entry<File, FileMonitor> entry : watchers.entrySet()) {
            map.put(entry.getKey(), entry.getValue().fileWatcher);
        }
        return map;
    }

    private class WatchRunnable implements Runnable {

        @Override
        public void run() {
            for (final Map.Entry<File, FileMonitor> entry : watchers.entrySet()) {
                final File file = entry.getKey();
                final FileMonitor fileMonitor = entry.getValue();
                final long lastModfied = file.lastModified();
                if (fileModified(fileMonitor, lastModfied)) {
                    logger.info("File {} was modified on {}, previous modification was {}", file, lastModfied, fileMonitor.lastModified);
                    fileMonitor.lastModified = lastModfied;
                    fileMonitor.fileWatcher.fileModified(file);
                }
            }
        }

        private boolean fileModified(final FileMonitor fileMonitor, final long lastModfied) {
            return lastModfied != fileMonitor.lastModified;
        }
    }

    private class FileMonitor {
        private final FileWatcher fileWatcher;
        private long lastModified;

        public FileMonitor(final long lastModified, final FileWatcher fileWatcher) {
            this.fileWatcher = fileWatcher;
            this.lastModified = lastModified;
        }
    }
}
