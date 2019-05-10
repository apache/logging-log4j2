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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.config.ConfigurationFileWatcher;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Manages {@link FileWatcher}s.
 *
 * @see FileWatcher
 * @see ConfigurationScheduler
 */
public class WatchManager extends AbstractLifeCycle {

    private static Logger logger = StatusLogger.getLogger();
    private final ConcurrentMap<Source, ConfigurationMonitor> watchers = new ConcurrentHashMap<>();
    private int intervalSeconds = 0;
    private ScheduledFuture<?> future;
    private final ConfigurationScheduler scheduler;
    private final List<WatchEventService> eventServiceList;
    private final UUID id = UuidUtil.getTimeBasedUuid();

    public WatchManager(final ConfigurationScheduler scheduler) {
        this.scheduler = scheduler;
        eventServiceList = getEventServices();
    }

    public UUID getId() {
        return this.id;
    }

    public boolean hasEventListeners() {
        return eventServiceList.size() > 0;
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
        for (final Source source : watchers.keySet()) {
            reset(source);
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
     * @param file the file for the monitor to reset.
     * @since 2.11.0
     */
    public void reset(final File file) {
        if (file == null) {
            return;
        }
        Source source = new Source(file);
        reset(source);
    }

    /**
     * Resets the configuration monitor for the given file being watched to its current last modified time. If this
     * manager does not watch the given configuration, nothing happens.
     * <p>
     * This allows you to start, stop, reset and start again a manager, without triggering file modified events if the
     * given watched configuration has changed during the period of time when the manager was stopped.
     * </p>
     *
     * @param source the Source for the monitor to reset.
     * @since 2.12.0
     */
    public void reset(final Source source) {
        if (source == null) {
            return;
        }
        final ConfigurationMonitor monitor = watchers.get(source);
        if (monitor != null) {
            Watcher watcher = monitor.getWatcher();
            if (watcher.isModified()) {
                final long lastModifiedMillis = watcher.getLastModified();
                if (logger.isDebugEnabled()) {
                    logger.debug("Resetting file monitor for '{}' from {} ({}) to {} ({})", source.getLocation(),
                        millisToString(monitor.lastModifiedMillis), monitor.lastModifiedMillis,
                        millisToString(lastModifiedMillis), lastModifiedMillis);
                }
                monitor.setLastModifiedMillis(lastModifiedMillis);
            }
        }
    }

    public void setIntervalSeconds(final int intervalSeconds) {
        if (!isStarted()) {
            if (this.intervalSeconds > 0 && intervalSeconds == 0) {
                scheduler.decrementScheduledItems();
            } else {
                if (this.intervalSeconds == 0 && intervalSeconds > 0) {
                    scheduler.incrementScheduledItems();
                }
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
            future = scheduler
                .scheduleWithFixedDelay(new WatchRunnable(), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }
        for (WatchEventService service : eventServiceList) {
            service.subscribe(this);
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        for (WatchEventService service : eventServiceList) {
            service.unsubscribe(this);
        }
        final boolean stopped = stop(future);
        setStopped();
        return stopped;
    }

    /**
     * Unwatches the given file.
     *
     * @param file the file to stop watching.
     * @since 2.11.0
     */
    public void unwatchFile(final File file) {
        Source source = new Source(file);
        unwatch(source);
    }

    /**
     * Unwatches the given file.
     *
     * @param source the Source to stop watching.
     *               the file to stop watching.
     * @since 2.12.0
     */
    public void unwatch(final Source source) {
        logger.debug("Unwatching configuration {}", source);
        watchers.remove(source);
    }

    public void checkFiles() {
        new WatchRunnable().run();
    }

    /**
     * Watches the given file.
     *
     * @param file        the file to watch.
     * @param fileWatcher the watcher to notify of file changes.
     */
    public void watchFile(final File file, final FileWatcher fileWatcher) {
        Watcher watcher;
        if (fileWatcher instanceof Watcher) {
            watcher = (Watcher) fileWatcher;
        } else {
            watcher = new WrappedFileWatcher(fileWatcher);
        }
        Source source = new Source(file);
        watch(source, watcher);
    }

    /**
     * Watches the given file.
     *
     * @param source  the source to watch.
     * @param watcher the watcher to notify of file changes.
     */
    public void watch(final Source source, final Watcher watcher) {
        watcher.watching(source);
        final long lastModified = watcher.getLastModified();
        if (logger.isDebugEnabled()) {
            logger.debug("Watching configuration '{}' for lastModified {} ({})", source, millisToString(lastModified),
                lastModified);
        }
        watchers.put(source, new ConfigurationMonitor(lastModified, watcher));
    }

    /**
     * Returns a Map of the file watchers.
     *
     * @return A Map of the file watchers.
     * @deprecated use getConfigurationWatchers.
     */
    public Map<File, FileWatcher> getWatchers() {
        final Map<File, FileWatcher> map = new HashMap<>(watchers.size());
        for (Map.Entry<Source, ConfigurationMonitor> entry : watchers.entrySet()) {
            if (entry.getValue().getWatcher() instanceof ConfigurationFileWatcher) {
                map.put(entry.getKey().getFile(), (FileWatcher) entry.getValue().getWatcher());
            } else {
                map.put(entry.getKey().getFile(), new WrappedFileWatcher((FileWatcher) entry.getValue().getWatcher()));
            }
        }
        return map;
    }

    /**
     * Return the ConfigurationWaatchers.
     *
     * @return the ConfigurationWatchers.
     * @since 2.11.2
     */
    public Map<Source, Watcher> getConfigurationWatchers() {
        final Map<Source, Watcher> map = new HashMap<>(watchers.size());
        for (final Map.Entry<Source, ConfigurationMonitor> entry : watchers.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getWatcher());
        }
        return map;
    }

    private String millisToString(final long millis) {
        return new Date(millis).toString();
    }

    private List<WatchEventService> getEventServices() {
        List<WatchEventService> list = new ArrayList<>();
        for (final ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            try {
                final ServiceLoader<WatchEventService> serviceLoader = ServiceLoader
                    .load(WatchEventService.class, classLoader);
                for (final WatchEventService service : serviceLoader) {
                    list.add(service);
                }
            } catch (final Throwable ex) {
                LOGGER.debug("Unable to retrieve WatchEventService from ClassLoader {}", classLoader, ex);
            }
        }
        return list;
    }

    private final class WatchRunnable implements Runnable {

        // Use a hard class reference here in case a refactoring changes the class name.
        private final String SIMPLE_NAME = WatchRunnable.class.getSimpleName();

        @Override
        public void run() {
            logger.trace("{} run triggered.", SIMPLE_NAME);
            for (final Map.Entry<Source, ConfigurationMonitor> entry : watchers.entrySet()) {
                final Source source = entry.getKey();
                final ConfigurationMonitor monitor = entry.getValue();
                if (monitor.getWatcher().isModified()) {
                    final long lastModified = monitor.getWatcher().getLastModified();
                    if (logger.isInfoEnabled()) {
                        logger.info("Source '{}' was modified on {} ({}), previous modification was on {} ({})", source,
                            millisToString(lastModified), lastModified, millisToString(monitor.lastModifiedMillis),
                            monitor.lastModifiedMillis);
                    }
                    monitor.lastModifiedMillis = lastModified;
                    monitor.getWatcher().modified();
                }
            }
            logger.trace("{} run ended.", SIMPLE_NAME);
        }
    }

    private final class ConfigurationMonitor {
        private final Watcher watcher;
        private volatile long lastModifiedMillis;

        public Watcher getWatcher() {
            return watcher;
        }

        public ConfigurationMonitor(final long lastModifiedMillis, final Watcher watcher) {
            this.watcher = watcher;
            this.lastModifiedMillis = lastModifiedMillis;
        }

        private void setLastModifiedMillis(final long lastModifiedMillis) {
            this.lastModifiedMillis = lastModifiedMillis;
        }

        @Override
        public String toString() {
            return "ConfigurationMonitor [watcher=" + watcher + ", lastModifiedMillis=" + lastModifiedMillis + "]";
        }

    }

    @Override
    public String toString() {
        return "WatchManager [intervalSeconds=" + intervalSeconds + ", watchers=" + watchers + ", scheduler="
            + scheduler + ", future=" + future + "]";
    }
}
