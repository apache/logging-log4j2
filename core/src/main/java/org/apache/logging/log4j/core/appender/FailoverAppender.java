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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The FailoverAppender will capture exceptions in an Appender and then route the event
 * to a different appender. Hopefully it is obvious that the Appenders must be configured
 * to not suppress exceptions for the FailoverAppender to work.
 */
@Plugin(name = "Failover", type = "Core", elementType = "appender", printObject = true)
public final class FailoverAppender extends AbstractAppender {

    private static final int DEFAULT_INTERVAL = 60 * Constants.MILLIS_IN_SECONDS;

    private final String primaryRef;

    private final String[] failovers;

    private final Configuration config;

    private AppenderControl primary;

    private final List<AppenderControl> failoverAppenders = new ArrayList<AppenderControl>();

    private final long interval;

    private long nextCheck = 0;

    private volatile boolean failure = false;

    private FailoverAppender(final String name, final Filter filter, final String primary, final String[] failovers,
                             final int interval, final Configuration config, final boolean handleExceptions) {
        super(name, filter, null, handleExceptions);
        this.primaryRef = primary;
        this.failovers = failovers;
        this.config = config;
        this.interval = interval;
    }


    @Override
    public void start() {
        final Map<String, Appender<?>> map = config.getAppenders();
        int errors = 0;
        if (map.containsKey(primaryRef)) {
            primary = new AppenderControl(map.get(primaryRef), null, null);
        } else {
            LOGGER.error("Unable to locate primary Appender " + primaryRef);
            ++errors;
        }
        for (final String name : failovers) {
            if (map.containsKey(name)) {
                failoverAppenders.add(new AppenderControl(map.get(name), null, null));
            } else {
                LOGGER.error("Failover appender " + name + " is not configured");
            }
        }
        if (failoverAppenders.size() == 0) {
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
    public void append(final LogEvent event) {
        final RuntimeException re = null;
        if (!isStarted()) {
            error("FailoverAppender " + getName() + " did not start successfully");
            return;
        }
        if (!failure) {
            callAppender(event);
        } else {
            final long current = System.currentTimeMillis();
            if (current >= nextCheck) {
                callAppender(event);
            } else {
                failover(event, null);
            }
        }
    }

    private void callAppender(final LogEvent event) {
        try {
            primary.callAppender(event);
        } catch (final Exception ex) {
            nextCheck = System.currentTimeMillis() + interval;
            failure = true;
            failover(event, ex);
        }
    }

    private void failover(final LogEvent event, final Exception ex) {
        final RuntimeException re = ex != null ? new LoggingException(ex) : null;
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
        if (!written && !isExceptionSuppressed()) {
            if (re != null) {
                throw re;
            } else {
                throw new LoggingException("Unable to write to failover appenders", failoverException);
            }
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
        sb.append("}");
        return sb.toString();
    }

    /**
     * Create a Failover Appender.
     * @param name The name of the Appender (required).
     * @param primary The name of the primary Appender (required).
     * @param failovers The name of one or more Appenders to fail over to (at least one is required).
     * @param interval The retry interval.
     * @param config The current Configuration (passed by the Configuration when the appender is created).
     * @param filter A Filter (optional).
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @return The FailoverAppender that was created.
     */
    @PluginFactory
    public static FailoverAppender createAppender(@PluginAttr("name") final String name,
                                                  @PluginAttr("primary") final String primary,
                                                  @PluginElement("failovers") final String[] failovers,
                                                  @PluginAttr("retryInterval") final String interval,
                                                  @PluginConfiguration final Configuration config,
                                                  @PluginElement("filters") final Filter filter,
                                                  @PluginAttr("suppressExceptions") final String suppress) {
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

        int retryInterval;
        if (interval == null) {
            retryInterval = DEFAULT_INTERVAL;
        } else {
            try {
                final int value = Integer.parseInt(interval);
                if (value >= 0) {
                    retryInterval = value * Constants.MILLIS_IN_SECONDS;
                } else {
                    LOGGER.warn("Interval " + interval + " is less than zero. Using default");
                    retryInterval = DEFAULT_INTERVAL;
                }
            } catch (final NumberFormatException nfe) {
                LOGGER.error("Interval " + interval + " is non-numeric. Using default");
                retryInterval = DEFAULT_INTERVAL;
            }
        }

        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);

        return new FailoverAppender(name, filter, primary, failovers, retryInterval, config, handleExceptions);
    }
}
