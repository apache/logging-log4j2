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
package org.apache.logging.log4j.core.async;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Asynchronous Logger object that is created via configuration and can be
 * combined with synchronous loggers.
 * <p>
 * AsyncLoggerConfig is a logger designed for high throughput and low latency
 * logging. It does not perform any I/O in the calling (application) thread, but
 * instead hands off the work to another thread as soon as possible. The actual
 * logging is performed in the background thread. It uses
 * <a href="https://lmax-exchange.github.io/disruptor/">LMAX Disruptor</a> for
 * inter-thread communication.
 * <p>
 * To use AsyncLoggerConfig, specify {@code <asyncLogger>} or
 * {@code <asyncRoot>} in configuration.
 * <p>
 * Note that for performance reasons, this logger does not include source
 * location by default. You need to specify {@code includeLocation="true"} in
 * the configuration or any %class, %location or %line conversion patterns in
 * your log4j.xml configuration will produce either a "?" character or no output
 * at all.
 * <p>
 * For best performance, use AsyncLoggerConfig with the RandomAccessFileAppender or
 * RollingRandomAccessFileAppender, with immediateFlush=false. These appenders have
 * built-in support for the batching mechanism used by the Disruptor library,
 * and they will flush to disk at the end of each batch. This means that even
 * with immediateFlush=false, there will never be any items left in the buffer;
 * all log events will all be written to disk in a very efficient manner.
 */
@Plugin(name = "asyncLogger", category = Node.CATEGORY, printObject = true)
public class AsyncLoggerConfig extends LoggerConfig {

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newAsyncBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static class Builder<B extends Builder<B>> extends LoggerConfig.Builder<B> {

        @Override
        public LoggerConfig build() {
            final String name = getLoggerName().equals(ROOT) ? Strings.EMPTY : getLoggerName();
            final LevelAndRefs container =
                    LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(), getConfig());
            return new AsyncLoggerConfig(
                    name,
                    container.refs,
                    getFilter(),
                    container.level,
                    isAdditivity(),
                    getProperties(),
                    getConfig(),
                    includeLocation(getIncludeLocation()));
        }
    }

    private static final ThreadLocal<Boolean> ASYNC_LOGGER_ENTERED = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private final AsyncLoggerConfigDelegate delegate;

    protected AsyncLoggerConfig(
            final String name,
            final List<AppenderRef> appenders,
            final Filter filter,
            final Level level,
            final boolean additive,
            final Property[] properties,
            final Configuration config,
            final boolean includeLocation) {
        super(name, appenders, filter, level, additive, properties, config, includeLocation);
        delegate = config.getAsyncLoggerConfigDelegate();
        delegate.setLogEventFactory(getLogEventFactory());
    }

    // package-protected for testing
    AsyncLoggerConfigDelegate getAsyncLoggerConfigDelegate() {
        return delegate;
    }

    @Override
    protected void log(final LogEvent event, final LoggerConfigPredicate predicate) {
        // See LOG4J2-2301
        if (predicate == LoggerConfigPredicate.ALL
                && ASYNC_LOGGER_ENTERED.get() == Boolean.FALSE
                &&
                // Optimization: AsyncLoggerConfig is identical to LoggerConfig
                // when no appenders are present. Avoid splitting for synchronous
                // and asynchronous execution paths until encountering an
                // AsyncLoggerConfig with appenders.
                hasAppenders()) {
            // This is the first AsnycLoggerConfig encountered by this LogEvent
            ASYNC_LOGGER_ENTERED.set(Boolean.TRUE);
            try {
                if (!isFiltered(event)) {
                    // Detect the first time we encounter an AsyncLoggerConfig. We must log
                    // to all non-async loggers first.
                    processLogEvent(event, LoggerConfigPredicate.SYNCHRONOUS_ONLY);
                    // Then pass the event to the background thread where
                    // all async logging is executed. It is important this
                    // happens at most once and after all synchronous loggers
                    // have been invoked, because we lose parameter references
                    // from reusable messages.
                    logToAsyncDelegate(event);
                }
            } finally {
                ASYNC_LOGGER_ENTERED.set(Boolean.FALSE);
            }
        } else {
            super.log(event, predicate);
        }
    }

    @Override
    protected void callAppenders(final LogEvent event) {
        super.callAppenders(event);
    }

    private void logToAsyncDelegate(final LogEvent event) {
        // Passes on the event to a separate thread that will call
        // asyncCallAppenders(LogEvent).
        populateLazilyInitializedFields(event);
        if (!delegate.tryEnqueue(event, this)) {
            handleQueueFull(event);
        }
    }

    private void handleQueueFull(final LogEvent event) {
        if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-1518, LOG4J2-2031
            // If queue is full AND we are in a recursive call, call appender directly to prevent deadlock
            AsyncQueueFullMessageUtil.logWarningToStatusLogger();
            logToAsyncLoggerConfigsOnCurrentThread(event);
        } else {
            // otherwise, we leave it to the user preference
            final EventRoute eventRoute = delegate.getEventRoute(event.getLevel());
            eventRoute.logMessage(this, event);
        }
    }

    private void populateLazilyInitializedFields(final LogEvent event) {
        event.getSource();
        event.getThreadName();
    }

    void logInBackgroundThread(final LogEvent event) {
        delegate.enqueueEvent(event, this);
    }

    /**
     * Called by AsyncLoggerConfigHelper.RingBufferLog4jEventHandler.
     *
     * This method will log the provided event to only configs of type {@link AsyncLoggerConfig} (not
     * default {@link LoggerConfig} definitions), which will be invoked on the <b>calling thread</b>.
     */
    void logToAsyncLoggerConfigsOnCurrentThread(final LogEvent event) {
        // skip the filter, which was already called on the logging thread
        processLogEvent(event, LoggerConfigPredicate.ASYNCHRONOUS_ONLY);
    }

    private String displayName() {
        return LogManager.ROOT_LOGGER_NAME.equals(getName()) ? LoggerConfig.ROOT : getName();
    }

    @Override
    public void start() {
        LOGGER.trace("AsyncLoggerConfig[{}] starting...", displayName());
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        LOGGER.trace("AsyncLoggerConfig[{}] stopping...", displayName());
        setStopped();
        return true;
    }

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the
     * ringbuffer of this {@code AsyncLoggerConfig}.
     *
     * @param contextName name of the {@code LoggerContext}
     * @return a new {@code RingBufferAdmin} that instruments the ringbuffer
     */
    public RingBufferAdmin createRingBufferAdmin(final String contextName) {
        return delegate.createRingBufferAdmin(contextName, getName());
    }

    /**
     * Factory method to create a LoggerConfig.
     *
     * @param additivity True if additive, false otherwise.
     * @param levelName The Level to be associated with the Logger.
     * @param loggerName The name of the Logger.
     * @param includeLocation "true" if location should be passed downstream
     * @param refs An array of Appender names.
     * @param properties Properties to pass to the Logger.
     * @param config The Configuration.
     * @param filter A Filter.
     * @return A new LoggerConfig.
     * @deprecated use {@link #createLogger(boolean, Level, String, String, AppenderRef[], Property[], Configuration, Filter)}
     */
    @Deprecated
    public static LoggerConfig createLogger(
            final String additivity,
            final String levelName,
            final String loggerName,
            final String includeLocation,
            final AppenderRef[] refs,
            final Property[] properties,
            final Configuration config,
            final Filter filter) {
        if (loggerName == null) {
            LOGGER.error("Loggers cannot be configured without a name");
            return null;
        }

        final List<AppenderRef> appenderRefs = Arrays.asList(refs);
        Level level;
        try {
            level = Level.toLevel(levelName, Level.ERROR);
        } catch (final Exception ex) {
            LOGGER.error("Invalid Log level specified: {}. Defaulting to Error", levelName);
            level = Level.ERROR;
        }
        final String name = loggerName.equals(LoggerConfig.ROOT) ? Strings.EMPTY : loggerName;
        final boolean additive = Booleans.parseBoolean(additivity, true);

        return new AsyncLoggerConfig(
                name, appenderRefs, filter, level, additive, properties, config, includeLocation(includeLocation));
    }

    /**
     * Factory method to create a LoggerConfig.
     *
     * @param additivity True if additive, false otherwise.
     * @param level The Level to be associated with the Logger.
     * @param loggerName The name of the Logger.
     * @param includeLocation "true" if location should be passed downstream
     * @param refs An array of Appender names.
     * @param properties Properties to pass to the Logger.
     * @param config The Configuration.
     * @param filter A Filter.
     * @return A new LoggerConfig.
     * @since 3.0
     */
    @Deprecated
    public static LoggerConfig createLogger(
            @PluginAttribute(value = "additivity", defaultBoolean = true) final boolean additivity,
            @PluginAttribute("level") final Level level,
            @Required(message = "Loggers cannot be configured without a name") @PluginAttribute("name")
                    final String loggerName,
            @PluginAttribute("includeLocation") final String includeLocation,
            @PluginElement("AppenderRef") final AppenderRef[] refs,
            @PluginElement("Properties") final Property[] properties,
            @PluginConfiguration final Configuration config,
            @PluginElement("Filter") final Filter filter) {
        final String name = loggerName.equals(ROOT) ? Strings.EMPTY : loggerName;
        return new AsyncLoggerConfig(
                name,
                Arrays.asList(refs),
                filter,
                level,
                additivity,
                properties,
                config,
                includeLocation(includeLocation));
    }

    // Note: for asynchronous loggers, includeLocation default is FALSE
    protected static boolean includeLocation(final String includeLocationConfigValue) {
        return Boolean.parseBoolean(includeLocationConfigValue);
    }

    /**
     * An asynchronous root Logger.
     */
    @Plugin(name = "asyncRoot", category = Core.CATEGORY_NAME, printObject = true)
    public static class RootLogger extends LoggerConfig {

        @PluginBuilderFactory
        public static <B extends Builder<B>> B newAsyncRootBuilder() {
            return new Builder<B>().asBuilder();
        }

        public static class Builder<B extends Builder<B>> extends RootLogger.Builder<B> {

            @Override
            public LoggerConfig build() {
                final LevelAndRefs container =
                        LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(), getConfig());
                return new AsyncLoggerConfig(
                        LogManager.ROOT_LOGGER_NAME,
                        container.refs,
                        getFilter(),
                        container.level,
                        isAdditivity(),
                        getProperties(),
                        getConfig(),
                        AsyncLoggerConfig.includeLocation(getIncludeLocation()));
            }
        }

        /**
         * @deprecated use {@link #createLogger(String, Level, String, AppenderRef[], Property[], Configuration, Filter)}
         */
        @Deprecated
        public static LoggerConfig createLogger(
                final String additivity,
                final String levelName,
                final String includeLocation,
                final AppenderRef[] refs,
                final Property[] properties,
                final Configuration config,
                final Filter filter) {
            final List<AppenderRef> appenderRefs = Arrays.asList(refs);
            Level level = null;
            try {
                level = Level.toLevel(levelName, Level.ERROR);
            } catch (final Exception ex) {
                LOGGER.error("Invalid Log level specified: {}. Defaulting to Error", levelName);
                level = Level.ERROR;
            }
            final boolean additive = Booleans.parseBoolean(additivity, true);
            return new AsyncLoggerConfig(
                    LogManager.ROOT_LOGGER_NAME,
                    appenderRefs,
                    filter,
                    level,
                    additive,
                    properties,
                    config,
                    AsyncLoggerConfig.includeLocation(includeLocation));
        }

        /**
         *
         */
        @Deprecated
        public static LoggerConfig createLogger(
                @PluginAttribute("additivity") final String additivity,
                @PluginAttribute("level") final Level level,
                @PluginAttribute("includeLocation") final String includeLocation,
                @PluginElement("AppenderRef") final AppenderRef[] refs,
                @PluginElement("Properties") final Property[] properties,
                @PluginConfiguration final Configuration config,
                @PluginElement("Filter") final Filter filter) {
            final List<AppenderRef> appenderRefs = Arrays.asList(refs);
            final Level actualLevel = level == null ? Level.ERROR : level;
            final boolean additive = Booleans.parseBoolean(additivity, true);
            return new AsyncLoggerConfig(
                    LogManager.ROOT_LOGGER_NAME,
                    appenderRefs,
                    filter,
                    actualLevel,
                    additive,
                    properties,
                    config,
                    AsyncLoggerConfig.includeLocation(includeLocation));
        }
    }
}
