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
package org.apache.logging.log4j.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * The core implementation of the {@link org.apache.logging.log4j.Logger} interface. Besides providing an
 * implementation of all the Logger methods, this class also provides some convenience methods for Log4j 1.x
 * compatibility as well as access to the {@link org.apache.logging.log4j.core.Filter Filters} and
 * {@link org.apache.logging.log4j.core.Appender Appenders} associated with this Logger. Note that access to these
 * underlying objects is provided primarily for use in unit tests or bridging legacy Log4j 1.x code. Future versions
 * of this class may or may not include the various methods that are noted as not being part of the public API.
 *
 * TODO All the isEnabled methods could be pushed into a filter interface.  Not sure of the utility of having
 * isEnabled be able to examine the message pattern and parameters. (RG) Moving the isEnabled methods out of
 * Logger noticeably impacts performance. The message pattern and parameters are required so that they can be
 * used in global filters.
 */
public class Logger extends AbstractLogger {

    private static final long serialVersionUID = 1L;

    /**
     * Config should be consistent across threads.
     */
    protected volatile PrivateConfig config;

    // FIXME: ditto to the above
    private final LoggerContext context;

    /**
     * The constructor.
     * @param context The LoggerContext this Logger is associated with.
     * @param messageFactory The message factory.
     * @param name The name of the Logger.
     */
    protected Logger(final LoggerContext context, final String name, final MessageFactory messageFactory) {
        super(name, messageFactory);
        this.context = context;
        config = new PrivateConfig(context.getConfiguration(), this);
    }

    /**
     * This method is only used for 1.x compatibility.
     * Returns the parent of this Logger. If it doesn't already exist return a temporary Logger.
     * @return The parent Logger.
     */
    public Logger getParent() {
        final LoggerConfig lc = config.loggerConfig.getName().equals(getName()) ? config.loggerConfig.getParent() :
            config.loggerConfig;
        if (lc == null) {
            return null;
        }
        if (context.hasLogger(lc.getName())) {
            return context.getLogger(lc.getName(), getMessageFactory());
        }
        return new Logger(context, lc.getName(), this.getMessageFactory());
    }

    /**
     * Returns the LoggerContext this Logger is associated with.
     * @return the LoggerContext.
     */
    public LoggerContext getContext() {
        return context;
    }

    /**
     * This method is not exposed through the public API and is provided primarily for unit testing.
     * @param level The Level to use on this Logger.
     */
    public synchronized void setLevel(final Level level) {
        if (level != null) {
            config = new PrivateConfig(config, level);
        }
    }

    @Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
        final Message msg = message == null ? new SimpleMessage(Strings.EMPTY) : message;
        config.config.getConfigurationMonitor().checkConfiguration();
        config.loggerConfig.log(getName(), fqcn, marker, level, msg, t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable t) {
        return config.filter(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message) {
        return config.filter(level, marker, message);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
        return config.filter(level, marker, message, params);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
        return config.filter(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
        return config.filter(level, marker, message, t);
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     * @param appender The Appender to add to the Logger.
     */
    public void addAppender(final Appender appender) {
        config.config.addLoggerAppender(this, appender);
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     * @param appender The Appender to remove from the Logger.
     */
    public void removeAppender(final Appender appender) {
        config.loggerConfig.removeAppender(appender.getName());
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     * @return A Map containing the Appender's name as the key and the Appender as the value.
     */
    public Map<String, Appender> getAppenders() {
         return config.loggerConfig.getAppenders();
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     * @return An Iterator over all the Filters associated with the Logger.
     */
    // FIXME: this really ought to be an Iterable instead of an Iterator
    public Iterator<Filter> getFilters() {
        final Filter filter = config.loggerConfig.getFilter();
        if (filter == null) {
            return new ArrayList<Filter>().iterator();
        } else if (filter instanceof CompositeFilter) {
            return ((CompositeFilter) filter).iterator();
        } else {
            final List<Filter> filters = new ArrayList<>();
            filters.add(filter);
            return filters.iterator();
        }
    }

    /**
     * Gets the Level associated with the Logger.
     *
     * @return the Level associate with the Logger.
     */
    @Override
    public Level getLevel() {
        return config.level;
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     * @return The number of Filters associated with the Logger.
     */
    public int filterCount() {
        final Filter filter = config.loggerConfig.getFilter();
        if (filter == null) {
            return 0;
        } else if (filter instanceof CompositeFilter) {
            return ((CompositeFilter) filter).size();
        }
        return 1;
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     * @param filter The Filter to add.
     */
    public void addFilter(final Filter filter) {
        config.config.addLoggerFilter(this, filter);
    }

    /**
     * This method is not exposed through the public API and is present only to support the Log4j 1.2
     * compatibility bridge.
     * @return true if the associated LoggerConfig is additive, false otherwise.
     */
    public boolean isAdditive() {
        return config.loggerConfig.isAdditive();
    }

    /**
     * This method is not exposed through the public API and is present only to support the Log4j 1.2
     * compatibility bridge.
     * @param additive Boolean value to indicate whether the Logger is additive or not.
     */
    public void setAdditive(final boolean additive) {
        config.config.setLoggerAdditive(this, additive);
    }

    /**
     * Associates the Logger with a new Configuration. This method is not exposed through the
     * public API.
     *
     * There are two ways that could be used to guarantee all threads are aware of changes to
     * config. 1. synchronize this method. Accessors don't need to be synchronized as Java will
     * treat all variables within a synchronized block as volatile. 2. Declare the variable
     * volatile. Option 2 is used here as the performance cost is very low and it does a better
     * job at documenting how it is used.
     *
     * @param newConfig The new Configuration.
     */
    protected void updateConfiguration(final Configuration newConfig) {
        this.config = new PrivateConfig(newConfig, this);
    }

    /**
     * The binding between a Logger and its configuration.
     */
    // TODO: Should not be Serializable per EJ item 74 (2nd Ed)?
    protected class PrivateConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        // config fields are public to make them visible to Logger subclasses
        public final LoggerConfig loggerConfig;
        public final Configuration config;
        private final Level level;
        private final int intLevel;
        private final Logger logger;

        public PrivateConfig(final Configuration config, final Logger logger) {
            this.config = config;
            this.loggerConfig = config.getLoggerConfig(getName());
            this.level = this.loggerConfig.getLevel();
            this.intLevel = this.level.intLevel();
            this.logger = logger;
        }

        public PrivateConfig(final PrivateConfig pc, final Level level) {
            this.config = pc.config;
            this.loggerConfig = pc.loggerConfig;
            this.level = level;
            this.intLevel = this.level.intLevel();
            this.logger = pc.logger;
        }

        public PrivateConfig(final PrivateConfig pc, final LoggerConfig lc) {
            this.config = pc.config;
            this.loggerConfig = lc;
            this.level = lc.getLevel();
            this.intLevel = this.level.intLevel();
            this.logger = pc.logger;
        }

        // LOG4J2-151: changed visibility to public
        public void logEvent(final LogEvent event) {
            config.getConfigurationMonitor().checkConfiguration();
            loggerConfig.log(event);
        }

        boolean filter(final Level level, final Marker marker, final String msg) {
            config.getConfigurationMonitor().checkConfiguration();
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final String msg, final Throwable t) {
            config.getConfigurationMonitor().checkConfiguration();
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, t);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final String msg, final Object... p1) {
            config.getConfigurationMonitor().checkConfiguration();
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p1);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final Object msg, final Throwable t) {
            config.getConfigurationMonitor().checkConfiguration();
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, t);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final Message msg, final Throwable t) {
            config.getConfigurationMonitor().checkConfiguration();
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, t);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }
    }

    /**
     * Returns a String representation of this instance in the form {@code "name:level[ in context_name]"}.
     * @return A String describing this Logger instance.
     */
    @Override
    public String toString() {
        final String nameLevel = Strings.EMPTY + getName() + ':' + getLevel();
        if (context == null) {
            return nameLevel;
        }
        final String contextName = context.getName();
        return contextName == null ? nameLevel : nameLevel + " in " + contextName;
    }
}
