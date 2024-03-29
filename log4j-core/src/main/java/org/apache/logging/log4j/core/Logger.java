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
package org.apache.logging.log4j.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.kit.logger.AbstractLogger;
import org.apache.logging.log4j.kit.recycler.RecyclerFactory;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The core implementation of the {@link org.apache.logging.log4j.Logger} interface. Besides providing an implementation
 * of all the Logger methods, this class also provides some convenience methods for Log4j 1.x compatibility as well as
 * access to the {@link org.apache.logging.log4j.core.Filter Filters} and {@link org.apache.logging.log4j.core.Appender
 * Appenders} associated with this Logger. Note that access to these underlying objects is provided primarily for use in
 * unit tests or bridging legacy Log4j 1.x code. Future versions of this class may or may not include the various
 * methods that are noted as not being part of the public API.
 */
@NullMarked
public class Logger extends AbstractLogger implements Supplier<LoggerConfig> {

    /**
     * Config should be consistent across threads.
     */
    protected volatile PrivateConfig privateConfig;

    private final LoggerContext context;

    /**
     * The constructor.
     *
     * @param context The LoggerContext this Logger is associated with.
     * @param name The name of the Logger.
     * @param messageFactory The message factory to use for logging methods.
     * @param flowMessageFactory The flow message factory to use for flow logging methods.
     * @param recyclerFactory The recycler to use for log builder instances.
     */
    protected Logger(
            final LoggerContext context,
            final String name,
            final MessageFactory messageFactory,
            final FlowMessageFactory flowMessageFactory,
            final RecyclerFactory recyclerFactory,
            final org.apache.logging.log4j.Logger statusLogger) {
        super(name, messageFactory, flowMessageFactory, recyclerFactory, statusLogger);
        this.context = context;
        privateConfig = new PrivateConfig(context.getConfiguration(), this);
    }

    /**
     * This method is only used for 1.x compatibility. Returns the parent of this Logger. If it doesn't already exist
     * return a temporary Logger.
     *
     * @return The parent Logger.
     */
    public Logger getParent() {
        return getParent(privateConfig);
    }

    private Logger getParent(final PrivateConfig config) {
        final LoggerConfig lc =
                config.loggerConfig.getName().equals(getName()) ? config.loggerConfig.getParent() : config.loggerConfig;
        if (lc == null) {
            return null;
        }
        final String lcName = lc.getName();
        final MessageFactory messageFactory = getMessageFactory();
        return context.getLogger(lcName, messageFactory);
    }

    /**
     * Returns the LoggerContext this Logger is associated with.
     *
     * @return the LoggerContext.
     */
    public LoggerContext getContext() {
        return context;
    }

    /**
     * This method is not exposed through the public API and is provided primarily for unit testing.
     * <p>
     * If the new level is null, this logger inherits the level from its parent.
     * </p>
     *
     * @param level The Level to use on this Logger, may be null.
     */
    public void setLevel(final Level level) {
        var currentConfig = privateConfig;
        if (level == currentConfig.loggerConfigLevel) {
            return;
        }
        final Level actualLevel;
        if (level != null) {
            actualLevel = level;
        } else {
            final Logger parent = getParent(currentConfig);
            actualLevel = parent != null ? parent.getLevel() : currentConfig.loggerConfigLevel;
        }
        privateConfig = new PrivateConfig(currentConfig, actualLevel);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.util.Supplier#get()
     */
    @Override
    public LoggerConfig get() {
        return privateConfig.loggerConfig;
    }

    protected boolean requiresLocation() {
        return privateConfig.requiresLocation;
    }

    @Override
    protected void doLog(
            final String fqcn,
            final @Nullable StackTraceElement location,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        final ReliabilityStrategy strategy = privateConfig.loggerConfig.getReliabilityStrategy();
        strategy.log(this, getName(), fqcn, location, marker, level, message, throwable);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker) {
        return privateConfig.filter(level, marker, null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message) {
        return privateConfig.filter(level, marker, message);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable t) {
        return privateConfig.filter(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
        return privateConfig.filter(level, marker, message, params);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
        return privateConfig.filter(level, marker, message, p0);
    }

    @Override
    public boolean isEnabled(
            final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
        return privateConfig.filter(level, marker, message, p0, p1);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        return privateConfig.filter(level, marker, message, p0, p1, p2);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return privateConfig.filter(level, marker, message, p0, p1, p2, p3);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return privateConfig.filter(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final CharSequence message, final Throwable t) {
        return privateConfig.filter(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
        return privateConfig.filter(level, marker, message, t);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
        return privateConfig.filter(level, marker, message, t);
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     *
     * @param appender The Appender to add to the Logger.
     */
    public void addAppender(final Appender appender) {
        privateConfig.config.addLoggerAppender(this, appender);
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     *
     * @param appender The Appender to remove from the Logger.
     */
    public void removeAppender(final Appender appender) {
        privateConfig.loggerConfig.removeAppender(appender.getName());
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     *
     * @return A Map containing the Appender's name as the key and the Appender as the value.
     */
    public Map<String, Appender> getAppenders() {
        return privateConfig.loggerConfig.getAppenders();
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     *
     * @return An Iterator over all the Filters associated with the Logger.
     */
    // FIXME: this really ought to be an Iterable instead of an Iterator
    public Iterator<Filter> getFilters() {
        final Filter filter = privateConfig.loggerConfig.getFilter();
        if (filter == null) {
            return Collections.emptyIterator();
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
        return privateConfig.loggerConfigLevel;
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     *
     * @return The number of Filters associated with the Logger.
     */
    public int filterCount() {
        final Filter filter = privateConfig.loggerConfig.getFilter();
        if (filter == null) {
            return 0;
        } else if (filter instanceof CompositeFilter) {
            return ((CompositeFilter) filter).size();
        }
        return 1;
    }

    /**
     * This method is not exposed through the public API and is used primarily for unit testing.
     *
     * @param filter The Filter to add.
     */
    public void addFilter(final Filter filter) {
        privateConfig.config.addLoggerFilter(this, filter);
    }

    /**
     * This method is not exposed through the public API and is present only to support the Log4j 1.2 compatibility
     * bridge.
     *
     * @return true if the associated LoggerConfig is additive, false otherwise.
     */
    public boolean isAdditive() {
        return privateConfig.loggerConfig.isAdditive();
    }

    /**
     * This method is not exposed through the public API and is present only to support the Log4j 1.2 compatibility
     * bridge.
     *
     * @param additive Boolean value to indicate whether the Logger is additive or not.
     */
    public void setAdditive(final boolean additive) {
        privateConfig.config.setLoggerAdditive(this, additive);
    }

    @Override
    public LogBuilder atLevel(final Level level) {
        // A global filter might accept messages less specific than level.
        // Therefore we return always a functional builder.
        if (privateConfig.hasFilter()) {
            return getLogBuilder(level);
        }
        return super.atLevel(level);
    }

    /**
     * Associates this Logger with a new Configuration. This method is not
     * exposed through the public API.
     * <p>
     * There are two ways this could be used to guarantee all threads are aware
     * of changes to config.
     * <ol>
     * <li>Synchronize this method. Accessors don't need to be synchronized as
     * Java will treat all variables within a synchronized block as volatile.
     * </li>
     * <li>Declare the variable volatile. Option 2 is used here as the
     * performance cost is very low and it does a better job at documenting how
     * it is used.</li>
     *
     * @param newConfig
     *            The new Configuration.
     */
    protected void updateConfiguration(final Configuration newConfig) {
        this.privateConfig = new PrivateConfig(newConfig, this);
    }

    /**
     * The binding between a Logger and its configuration.
     */
    protected class PrivateConfig {
        // config fields are public to make them visible to Logger subclasses
        /** LoggerConfig to delegate the actual logging to. */
        public final LoggerConfig loggerConfig; // SUPPRESS CHECKSTYLE
        /** The current Configuration associated with the LoggerConfig. */
        public final Configuration config; // SUPPRESS CHECKSTYLE

        private final Level loggerConfigLevel;
        private final int intLevel;
        private final Logger logger;
        private final boolean requiresLocation;

        public PrivateConfig(final Configuration config, final Logger logger) {
            this.config = config;
            this.loggerConfig = config.getLoggerConfig(getName());
            this.loggerConfigLevel = this.loggerConfig.getLevel();
            this.intLevel = this.loggerConfigLevel.intLevel();
            this.logger = logger;
            this.requiresLocation = this.loggerConfig.requiresLocation();
        }

        public PrivateConfig(final PrivateConfig pc, final Level level) {
            this.config = pc.config;
            this.loggerConfig = pc.loggerConfig;
            this.loggerConfigLevel = level;
            this.intLevel = this.loggerConfigLevel.intLevel();
            this.logger = pc.logger;
            this.requiresLocation = this.loggerConfig.requiresLocation();
        }

        public PrivateConfig(final PrivateConfig pc, final LoggerConfig lc) {
            this.config = pc.config;
            this.loggerConfig = lc;
            this.loggerConfigLevel = lc.getLevel();
            this.intLevel = this.loggerConfigLevel.intLevel();
            this.logger = pc.logger;
            this.requiresLocation = this.loggerConfig.requiresLocation();
        }

        // LOG4J2-151: changed visibility to public
        public void logEvent(final LogEvent event) {
            loggerConfig.log(event);
        }

        boolean hasFilter() {
            return config.getFilter() != null;
        }

        boolean filter(final Level level, final Marker marker, final String msg) {
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
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, (Object) msg, t);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final String msg, final Object... p1) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p1);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final String msg, final Object p0) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1, p2);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1, p2, p3);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7,
                final Object p8) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(
                final Level level,
                final Marker marker,
                final String msg,
                final Object p0,
                final Object p1,
                final Object p2,
                final Object p3,
                final Object p4,
                final Object p5,
                final Object p6,
                final Object p7,
                final Object p8,
                final Object p9) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r =
                        filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final CharSequence msg, final Throwable t) {
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, t);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        boolean filter(final Level level, final Marker marker, final Object msg, final Throwable t) {
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
            final Filter filter = config.getFilter();
            if (filter != null) {
                final Filter.Result r = filter.filter(logger, level, marker, msg, t);
                if (r != Filter.Result.NEUTRAL) {
                    return r == Filter.Result.ACCEPT;
                }
            }
            return level != null && intLevel >= level.intLevel();
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("PrivateConfig [loggerConfig=");
            builder.append(loggerConfig);
            builder.append(", config=");
            builder.append(config);
            builder.append(", loggerConfigLevel=");
            builder.append(loggerConfigLevel);
            builder.append(", intLevel=");
            builder.append(intLevel);
            builder.append(", logger=");
            builder.append(logger);
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Returns a String representation of this instance in the form {@code "name:level[ in context_name]"}.
     *
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Logger that = (Logger) o;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public static class Builder {
        private String name;
        /**
         * Message factory explicitly requested by the user.
         */
        private @Nullable MessageFactory messageFactory;

        private final LoggerContext context;
        private final MessageFactory defaultMessageFactory;
        private final FlowMessageFactory flowMessageFactory;
        private final RecyclerFactory recyclerFactory;
        private final org.apache.logging.log4j.Logger statusLogger;

        @Inject
        public Builder(
                final LoggerContext context,
                final MessageFactory defaultMessageFactory,
                final FlowMessageFactory flowMessageFactory,
                final RecyclerFactory recyclerFactory,
                final @Named("StatusLogger") org.apache.logging.log4j.Logger statusLogger) {
            this.context = context;
            this.defaultMessageFactory = defaultMessageFactory;
            this.flowMessageFactory = flowMessageFactory;
            this.recyclerFactory = recyclerFactory;
            this.statusLogger = statusLogger;
        }

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder setMessageFactory(final MessageFactory messageFactory) {
            this.messageFactory = messageFactory;
            return this;
        }

        protected LoggerContext getContext() {
            return context;
        }

        protected String getName() {
            return name;
        }

        protected MessageFactory getActualMessageFactory() {
            return messageFactory != null ? messageFactory : defaultMessageFactory;
        }

        protected FlowMessageFactory getFlowMessageFactory() {
            return flowMessageFactory;
        }

        protected RecyclerFactory getRecyclerFactory() {
            return recyclerFactory;
        }

        protected org.apache.logging.log4j.Logger getStatusLogger() {
            return statusLogger;
        }

        public Logger build() {
            return new Logger(
                    getContext(),
                    getName(),
                    getActualMessageFactory(),
                    getFlowMessageFactory(),
                    getRecyclerFactory(),
                    getStatusLogger());
        }
    }
}
