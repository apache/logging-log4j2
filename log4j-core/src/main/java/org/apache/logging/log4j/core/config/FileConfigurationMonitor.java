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
package org.apache.logging.log4j.core.config;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Configuration monitor that periodically checks the timestamp of the configuration file and calls the
 * ConfigurationListeners when an update occurs.
 */
public class FileConfigurationMonitor implements ConfigurationMonitor {

    private static final int MASK = 0x0f;

    static final int MIN_INTERVAL = 5;

    private final File file;

    private volatile long lastModified;

    private final List<ConfigurationListener> listeners;

    private final long intervalNano;

    private volatile long nextCheck;

    private final AtomicInteger counter = new AtomicInteger(0);

    private static final Lock LOCK = new ReentrantLock();

    private final Reconfigurable reconfigurable;

    /**
     * Constructor.
     * @param reconfigurable The Configuration that can be reconfigured.
     * @param file The File to monitor.
     * @param listeners The List of ConfigurationListeners to notify upon a change.
     * @param intervalSeconds The monitor interval in seconds. The minimum interval is 5 seconds.
     */
    public FileConfigurationMonitor(final Reconfigurable reconfigurable, final File file,
                                    final List<ConfigurationListener> listeners,
                                    final int intervalSeconds) {
        this.reconfigurable = reconfigurable;
        this.file = file;
        this.lastModified = file.lastModified();
        this.listeners = listeners;
        this.intervalNano = TimeUnit.SECONDS.toNanos(Math.max(intervalSeconds, MIN_INTERVAL));
        this.nextCheck = System.nanoTime() + this.intervalNano;
    }

    /**
     * Called to determine if the configuration has changed.
     */
    @Override
    public void checkConfiguration() {
        final long current;
        if (((counter.incrementAndGet() & MASK) == 0) && ((current = System.nanoTime()) - nextCheck >= 0)) {
            LOCK.lock();
            try {
                nextCheck = current + intervalNano;
                final long currentLastModified = file.lastModified();
                if (currentLastModified > lastModified) {
                    lastModified = currentLastModified;
                    for (final ConfigurationListener listener : listeners) {
                        final Thread thread = new Thread(new ReconfigurationWorker(listener, reconfigurable));
                        thread.setDaemon(true);
                        thread.start();
                    }
                }
            } finally {
                LOCK.unlock();
            }
        }
    }

    private static class ReconfigurationWorker implements Runnable {

        private final ConfigurationListener listener;
        private final Reconfigurable reconfigurable;

        public ReconfigurationWorker(final ConfigurationListener listener, final Reconfigurable reconfigurable) {
            this.listener = listener;
            this.reconfigurable = reconfigurable;
        }

        @Override
        public void run() {
            listener.onChange(reconfigurable);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.logging.log4j.core.config.ReliabilityStrategyFactory#getReliabilityStrategy(org.apache.logging.log4j.core.config.LoggerConfig)
     */
    @Override
    public ReliabilityStrategy getReliabilityStrategy(LoggerConfig loggerConfig) {
        return new AwaitCompletionReliabilityStrategy(loggerConfig);
    }
}
