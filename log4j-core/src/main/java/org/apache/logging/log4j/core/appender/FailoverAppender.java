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
package org.apache.logging.log4j.core.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Constants;

/**
 * The FailoverAppender will capture exceptions in an Appender and then route the event
 * to a different appender. Hopefully it is obvious that the Appenders must be configured
 * to not suppress exceptions for the FailoverAppender to work.
 */
@Plugin(name = "Failover", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class FailoverAppender extends AbstractAppender {

    private static final int DEFAULT_INTERVAL_SECONDS = 60;

    private final String primaryRef;

    private final String[] failovers;

    private final Configuration config;

    private AppenderControl primary;

    private final List<AppenderControl> failoverAppenders = new ArrayList<>();

    private final long intervalNanos;

    private volatile long nextCheckNanos;

    private FailoverAppender(
            final String name,
            final Filter filter,
            final String primary,
            final String[] failovers,
            final int intervalMillis,
            final Configuration config,
            final boolean ignoreExceptions,
            final Property[] properties) {
        super(name, filter, null, ignoreExceptions, properties);
        this.primaryRef = primary;
        this.failovers = failovers;
        this.config = config;
        this.intervalNanos = TimeUnit.MILLISECONDS.toNanos(intervalMillis);
    }

    @Override
    public void start() {
        final Map<String, Appender> map = config.getAppenders();
        int errors = 0;
        final Appender appender = map.get(primaryRef);
        if (appender != null) {
            primary = new AppenderControl(appender, null, null);
        } else {
            LOGGER.error("Unable to locate primary Appender " + primaryRef);
            ++errors;
        }
        for (final String name : failovers) {
            final Appender foAppender = map.get(name);
            if (foAppender != null) {
                failoverAppenders.add(new AppenderControl(foAppender, null, null));
            } else {
                LOGGER.error("Failover appender " + name + " is not configured");
            }
        }
        if (failoverAppenders.isEmpty()) {
            LOGGER.error("No failover appenders are available");
            ++errors;
        }
        if (errors == 0) {
            super.start();
        }
    }

    /**
     * Handle the Log event.
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        if (!isStarted()) {
            error("FailoverAppender " + getName() + " did not start successfully");
            return;
        }
        final long localCheckNanos = nextCheckNanos;
        if (localCheckNanos == 0 || System.nanoTime() - localCheckNanos > 0) {
            callAppender(event);
        } else {
            failover(event, null);
        }
    }

    private void callAppender(final LogEvent event) {
        try {
            primary.callAppender(event);
            nextCheckNanos = 0;
        } catch (final Exception ex) {
            nextCheckNanos = System.nanoTime() + intervalNanos;
            failover(event, ex);
        }
    }

    private void failover(final LogEvent event, final Exception ex) {
        final RuntimeException re =
                ex != null ? (ex instanceof LoggingException ? (LoggingException) ex : new LoggingException(ex)) : null;
        boolean written = false;
        Exception failoverException = null;
        for (final AppenderControl control : failoverAppenders) {
            try {
                control.callAppender(event);
                written = true;
                break;
            } catch (final Exception fex) {
                if (failoverException == null) {
                    failoverException = fex;
                }
            }
        }
        if (!written && !ignoreExceptions()) {
            if (re != null) {
                throw re;
            }
            throw new LoggingException("Unable to write to failover appenders", failoverException);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getName());
        sb.append(" primary=").append(primary).append(", failover={");
        boolean first = true;
        for (final String str : failovers) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(str);
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Create a Failover Appender.
     * @param name The name of the Appender (required).
     * @param primary The name of the primary Appender (required).
     * @param failovers The name of one or more Appenders to fail over to (at least one is required).
     * @param retryIntervalSeconds The retry interval in seconds.
     * @param config The current Configuration (passed by the Configuration when the appender is created).
     * @param filter A Filter (optional).
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @return The FailoverAppender that was created.
     */
    @PluginFactory
    public static FailoverAppender createAppender(
            @PluginAttribute("name") final String name,
            @PluginAttribute("primary") final String primary,
            @PluginElement("Failovers") final String[] failovers,
            @PluginAliases("retryInterval") // deprecated
                    @PluginAttribute("retryIntervalSeconds")
                    final String retryIntervalSeconds,
            @PluginConfiguration final Configuration config,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("ignoreExceptions") final String ignore) {
        if (name == null) {
            LOGGER.error("A name for the Appender must be specified");
            return null;
        }
        if (primary == null) {
            LOGGER.error("A primary Appender must be specified");
            return null;
        }
        if (failovers == null || failovers.length == 0) {
            LOGGER.error("At least one failover Appender must be specified");
            return null;
        }

        final int seconds = parseInt(retryIntervalSeconds, DEFAULT_INTERVAL_SECONDS);
        int retryIntervalMillis;
        if (seconds >= 0) {
            retryIntervalMillis = seconds * Constants.MILLIS_IN_SECONDS;
        } else {
            LOGGER.warn("Interval " + retryIntervalSeconds + " is less than zero. Using default");
            retryIntervalMillis = DEFAULT_INTERVAL_SECONDS * Constants.MILLIS_IN_SECONDS;
        }

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        return new FailoverAppender(
                name, filter, primary, failovers, retryIntervalMillis, config, ignoreExceptions, null);
    }
}
