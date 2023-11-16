/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.routing;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.Scheduled;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Policy is purging appenders that were not in use specified time in minutes
 */
@Plugin(name = "IdlePurgePolicy", category = Core.CATEGORY_NAME, printObject = true)
@Scheduled
public class IdlePurgePolicy extends AbstractLifeCycle implements PurgePolicy, Runnable {

    private final long timeToLive;
    private final long checkInterval;
    private final ConcurrentMap<String, Long> appendersUsage = new ConcurrentHashMap<>();
    private RoutingAppender routingAppender;
    private final ConfigurationScheduler scheduler;
    private volatile ScheduledFuture<?> future;

    public IdlePurgePolicy(final long timeToLive, final long checkInterval, final ConfigurationScheduler scheduler) {
        this.timeToLive = timeToLive;
        this.checkInterval = checkInterval;
        this.scheduler = scheduler;
    }

    @Override
    public void initialize(@SuppressWarnings("hiding") final RoutingAppender routingAppender) {
        this.routingAppender = routingAppender;
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        final boolean stopped = stop(future);
        setStopped();
        return stopped;
    }

    /**
     * Purging appenders that were not in use specified time
     */
    @Override
    public void purge() {
        final long createTime = System.currentTimeMillis() - timeToLive;
        for (final Entry<String, Long> entry : appendersUsage.entrySet()) {
            final long entryValue = entry.getValue();
            if (entryValue < createTime) {
                if (appendersUsage.remove(entry.getKey(), entryValue)) {
                    LOGGER.debug("Removing appender {}", entry.getKey());
                    routingAppender.deleteAppender(entry.getKey());
                }
            }
        }
    }

    @Override
    public void update(final String key, final LogEvent event) {
        final long now = System.currentTimeMillis();
        appendersUsage.put(key, now);
        if (future == null) {
            synchronized (this) {
                if (future == null) {
                    scheduleNext();
                }
            }
        }
    }

    @Override
    public void run() {
        purge();
        scheduleNext();
    }

    private void scheduleNext() {
        long updateTime = Long.MAX_VALUE;
        for (final Entry<String, Long> entry : appendersUsage.entrySet()) {
            if (entry.getValue() < updateTime) {
                updateTime = entry.getValue();
            }
        }

        if (updateTime < Long.MAX_VALUE) {
            final long interval = timeToLive - (System.currentTimeMillis() - updateTime);
            future = scheduler.schedule(this, interval, TimeUnit.MILLISECONDS);
        } else {
            // reset to initial state - in case of all appenders already purged
            future = scheduler.schedule(this, checkInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Create the PurgePolicy
     *
     * @param timeToLive    the number of increments of timeUnit before the Appender should be purged.
     * @param checkInterval when all appenders purged, the number of increments of timeUnit to check if any appenders appeared
     * @param timeUnit      the unit of time the timeToLive and the checkInterval is expressed in.
     * @return The Routes container.
     */
    @PluginFactory
    public static PurgePolicy createPurgePolicy(
            @PluginAttribute("timeToLive") final String timeToLive,
            @PluginAttribute("checkInterval") final String checkInterval,
            @PluginAttribute("timeUnit") final String timeUnit,
            @PluginConfiguration final Configuration configuration) {

        if (timeToLive == null) {
            LOGGER.error("A timeToLive value is required");
            return null;
        }
        TimeUnit units;
        if (timeUnit == null) {
            units = TimeUnit.MINUTES;
        } else {
            try {
                units = TimeUnit.valueOf(toRootUpperCase(timeUnit));
            } catch (final Exception ex) {
                LOGGER.error("Invalid timeUnit value {}. timeUnit set to MINUTES", timeUnit, ex);
                units = TimeUnit.MINUTES;
            }
        }

        long ttl = units.toMillis(Long.parseLong(timeToLive));
        if (ttl < 0) {
            LOGGER.error("timeToLive must be positive. timeToLive set to 0");
            ttl = 0;
        }

        long ci;
        if (checkInterval == null) {
            ci = ttl;
        } else {
            ci = units.toMillis(Long.parseLong(checkInterval));
            if (ci < 0) {
                LOGGER.error("checkInterval must be positive. checkInterval set equal to timeToLive = {}", ttl);
                ci = ttl;
            }
        }

        return new IdlePurgePolicy(ttl, ci, configuration.getScheduler());
    }

    @Override
    public String toString() {
        return "timeToLive=" + timeToLive;
    }
}
