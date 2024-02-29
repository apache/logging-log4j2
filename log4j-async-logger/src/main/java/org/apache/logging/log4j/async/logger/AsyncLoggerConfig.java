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
package org.apache.logging.log4j.async.logger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.AsyncQueueFullMessageUtil;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.kit.logger.AbstractLogger;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.Strings;

/**
 * Asynchronous Logger object that is created via configuration and can be
 * combined with synchronous loggers.
 * <p>
 * AsyncLoggerConfig is a logger designed for high throughput and low latency
 * logging. It does not perform any I/O in the calling (application) thread, but
 * instead hands off the work to another thread as soon as possible. The actual
 * logging is performed in the background thread. It uses
 * <a href="https://lmax-exchange.github.io/disruptor/">LMAX Disruptor</a>
 * for inter-thread communication.
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
@Configurable(printObject = true)
@Plugin("asyncLogger")
public class AsyncLoggerConfig extends LoggerConfig {

    private static final ThreadLocal<Boolean> ASYNC_LOGGER_ENTERED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private AsyncLoggerConfigDelegate delegate;

    @PluginFactory
    public static <B extends Builder<B>> B newAsyncBuilder() {
        return new Builder<B>().asBuilder();
    }

    public static class Builder<B extends Builder<B>> extends LoggerConfig.Builder<B> {

        @Override
        public LoggerConfig build() {
            final String name = getLoggerName().equals(ROOT) ? Strings.EMPTY : getLoggerName();
            final LevelAndRefs container =
                    LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(), getConfig());
            final String includeLocationConfigValue = getIncludeLocation();
            return new AsyncLoggerConfig(
                    name,
                    container.refs,
                    getFilter(),
                    container.level,
                    isAdditivity(),
                    getProperties(),
                    getConfig(),
                    Boolean.parseBoolean(includeLocationConfigValue),
                    getLogEventFactory());
        }
    }

    protected AsyncLoggerConfig(
            final String name,
            final List<AppenderRef> appenders,
            final Filter filter,
            final Level level,
            final boolean additive,
            final Property[] properties,
            final Configuration config,
            final boolean includeLocation,
            final LogEventFactory logEventFactory) {
        super(name, appenders, filter, level, additive, properties, config, includeLocation, logEventFactory);
    }

    @Override
    public void initialize() {
        final Configuration configuration = getConfiguration();
        final DisruptorConfiguration disruptorConfig = configuration.addExtensionIfAbsent(
                DisruptorConfiguration.class,
                () -> DisruptorConfiguration.newBuilder().build());
        delegate = disruptorConfig.getAsyncLoggerConfigDelegate();
        delegate.setLogEventFactory(getLogEventFactory());
        super.initialize();
    }

    protected void log(final LogEvent event, final Predicate<LoggerConfig> predicate) {
        // See LOG4J2-2301
        if (predicate == null
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
                    processLogEvent(event, lc -> !(lc instanceof AsyncLoggerConfig));
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

    // package-protected for testing
    AsyncLoggerConfigDelegate getAsyncLoggerConfigDelegate() {
        return delegate;
    }

    @Override
    protected void callAppenders(final LogEvent event) {
        super.callAppenders(event);
    }

    private void logToAsyncDelegate(final LogEvent event) {
        // Passes on the event to a separate thread that will call
        // asyncCallAppenders(LogEvent).
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
            switch (eventRoute) {
                case DISCARD:
                    break;
                case ENQUEUE:
                    logInBackgroundThread(event);
                    break;
                case SYNCHRONOUS:
                    logToAsyncLoggerConfigsOnCurrentThread(event);
                    break;
                default:
            }
        }
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
        processLogEvent(event, lc -> lc instanceof AsyncLoggerConfig);
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
     * An asynchronous root Logger.
     */
    @Configurable(printObject = true)
    @Plugin("asyncRoot")
    public static class RootLogger extends LoggerConfig {

        @PluginFactory
        public static <B extends Builder<B>> B newAsyncRootBuilder() {
            return new Builder<B>().asBuilder();
        }

        public static class Builder<B extends Builder<B>> extends RootLogger.Builder<B> {

            @Override
            public LoggerConfig build() {
                final LevelAndRefs container =
                        LoggerConfig.getLevelAndRefs(getLevel(), getRefs(), getLevelAndRefs(), getConfig());
                final String includeLocationConfigValue = getIncludeLocation();
                return new AsyncLoggerConfig(
                        LogManager.ROOT_LOGGER_NAME,
                        container.refs,
                        getFilter(),
                        container.level,
                        isAdditivity(),
                        getProperties(),
                        getConfig(),
                        Boolean.parseBoolean(includeLocationConfigValue),
                        getLogEventFactory());
            }
        }
    }
}
