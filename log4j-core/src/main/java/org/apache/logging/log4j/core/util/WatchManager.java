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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages FileWatchers.
 */
public class WatchManager extends AbstractLifeCycle {

    private static final long serialVersionUID = 8998356999926962686L;
    private static Logger logger = StatusLogger.getLogger();
    private final ConcurrentMap<File, FileMonitor> watchers = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService;
    private int intervalSeconds = 0;
    private ScheduledFuture<?> future;

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public int getIntervalSeconds() {
        return this.intervalSeconds;
    }

    @Override
    public void start() {
        super.start();
        if (intervalSeconds > 0) {
            future = executorService.scheduleWithFixedDelay(new WatchWorker(), intervalSeconds, intervalSeconds,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() {
        future.cancel(true);
        super.stop();
    }

    public void watchFile(File file, FileWatcher watcher) {
        watchers.put(file, new FileMonitor(file.lastModified(), watcher));

    }

    private class WatchWorker implements Runnable {

        @Override
        public void run() {
            for (Map.Entry<File, FileMonitor> entry : watchers.entrySet()) {
                File file = entry.getKey();
                FileMonitor fileMonitor = entry.getValue();
                long lastModfied = file.lastModified();
                if (lastModfied > fileMonitor.lastModified) {
                    logger.info("File {} was modified", file.toString());
                    fileMonitor.lastModified = lastModfied;
                    fileMonitor.fileWatcher.fileModified(file);
                }
            }
        }
    }

    private class FileMonitor {
        private final FileWatcher fileWatcher;
        private long lastModified;

        public FileMonitor(long lastModified, FileWatcher fileWatcher) {
            this.fileWatcher = fileWatcher;
            this.lastModified = lastModified;
        }
    }
}
