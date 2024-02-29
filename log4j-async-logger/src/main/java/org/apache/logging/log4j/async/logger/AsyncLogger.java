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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.ReusableLogEvent;
import org.apache.logging.log4j.core.async.AsyncQueueFullMessageUtil;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.kit.logger.AbstractLogger;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.recycler.Recycler;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.util.StringMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * AsyncLogger is a logger designed for high throughput and low latency logging. It does not perform any I/O in the
 * calling (application) thread, but instead hands off the work to another thread as soon as possible. The actual
 * logging is performed in the background thread. It uses <a href="https://lmax-exchange.github.io/disruptor/">LMAX
 * Disruptor</a> for inter-thread communication.
 * <p>
 * To use AsyncLogger, specify the System property
 * {@code -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector} before you obtain a
 * Logger, and all Loggers returned by LogManager.getLogger will be AsyncLoggers.
 * <p>
 * Note that for performance reasons, this logger does not include source location by default. You need to specify
 * {@code includeLocation="true"} in the configuration or any %class, %location or %line conversion patterns in your
 * log4j.xml configuration will produce either a "?" character or no output at all.
 * <p>
 * For best performance, use AsyncLogger with the RandomAccessFileAppender or RollingRandomAccessFileAppender, with
 * immediateFlush=false. These appenders have built-in support for the batching mechanism used by the Disruptor library,
 * and they will flush to disk at the end of each batch. This means that even with immediateFlush=false, there will
 * never be any items left in the buffer; all log events will all be written to disk in a very efficient manner.
 */
@NullMarked
public class AsyncLogger extends Logger {
    // Implementation note: many methods in this class are tuned for performance. MODIFY WITH CARE!
    // Specifically, try to keep the hot methods to 35 bytecodes or less:
    // this is within the MaxInlineSize threshold and makes these methods candidates for
    // immediate inlining instead of waiting until they are designated "hot enough".

    private final Clock clock; // not reconfigurable
    private final ContextDataInjector contextDataInjector; // not reconfigurable

    private final Recycler<RingBufferLogEventTranslator> translatorRecycler;
    private final AsyncLoggerDisruptor loggerDisruptor;

    private volatile boolean includeLocation; // reconfigurable
    private volatile NanoClock nanoClock; // reconfigurable

    /**
     * Constructs an {@code AsyncLogger} with the specified context, name and message factory.
     *
     * @param context context of this logger
     * @param name name of this logger
     * @param messageFactory message factory of this logger
     * @param loggerDisruptor helper class that logging can be delegated to. This object owns the Disruptor.
     */
    AsyncLogger(
            final LoggerContext context,
            final String name,
            final MessageFactory messageFactory,
            final FlowMessageFactory flowMessageFactory,
            final RecyclerFactory recyclerFactory,
            final org.apache.logging.log4j.Logger statusLogger,
            final AsyncLoggerDisruptor loggerDisruptor) {
        super(context, name, messageFactory, flowMessageFactory, recyclerFactory, statusLogger);
        final Configuration configuration = context.getConfiguration();
        this.translatorRecycler = configuration
                .getRecyclerFactory()
                .create(RingBufferLogEventTranslator::new, RingBufferLogEventTranslator::clear);
        this.loggerDisruptor = loggerDisruptor;
        includeLocation = privateConfig.loggerConfig.isIncludeLocation();
        nanoClock = configuration.getNanoClock();
        clock = configuration.getComponent(Clock.KEY);
        contextDataInjector = configuration.getComponent(ContextDataInjector.KEY);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.Logger#updateConfiguration(org.apache.logging.log4j.core.config.Configuration)
     */
    @Override
    protected void updateConfiguration(final Configuration newConfig) {
        nanoClock = newConfig.getNanoClock();
        includeLocation = newConfig.getLoggerConfig(getName()).isIncludeLocation();
        super.updateConfiguration(newConfig);
    }

    // package protected for unit tests
    NanoClock getNanoClock() {
        return nanoClock;
    }

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This re-uses a {@code RingBufferLogEventTranslator} instance cached in a {@code ThreadLocal} to avoid creating
     * unnecessary objects with each event.
     *
     * @param fqcn      fully qualified name of the caller
     * @param location  the Location of the caller.
     * @param level     level at which the caller wants to log the message
     * @param marker    message marker
     * @param message   the log message
     * @param throwable a {@code Throwable} or {@code null}
     */
    @Override
    protected void doLog(
            final String fqcn,
            final @Nullable StackTraceElement location,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        // Implementation note: this method is tuned for performance. MODIFY WITH CARE!

        final RingBufferLogEventTranslator translator = translatorRecycler.acquire();
        try {
            initTranslator(translator, fqcn, location, level, marker, message, throwable);
            translator.updateThreadValues();
            publish(translator);
        } finally {
            translatorRecycler.release(translator);
        }
    }

    private void publish(final RingBufferLogEventTranslator translator) {
        if (!loggerDisruptor.tryPublish(translator)) {
            handleRingBufferFull(translator);
        }
    }

    private void handleRingBufferFull(final RingBufferLogEventTranslator translator) {
        if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-1518, LOG4J2-2031
            // If queue is full AND we are in a recursive call, call appender directly to prevent deadlock
            AsyncQueueFullMessageUtil.logWarningToStatusLogger();
            logMessageInCurrentThread(
                    translator.fqcn, translator.level, translator.marker, translator.message, translator.thrown);
            translator.clear();
            return;
        }
        final EventRoute eventRoute = loggerDisruptor.getEventRoute(translator.level);
        switch (eventRoute) {
            case ENQUEUE:
                loggerDisruptor.enqueueLogMessageWhenQueueFull(translator);
                break;
            case SYNCHRONOUS:
                logMessageInCurrentThread(
                        translator.fqcn, translator.level, translator.marker, translator.message, translator.thrown);
                translator.clear();
                break;
            case DISCARD:
                translator.clear();
                break;
            default:
                throw new IllegalStateException("Unknown EventRoute " + eventRoute);
        }
    }

    private void initTranslator(
            final RingBufferLogEventTranslator translator,
            final String fqcn,
            final @Nullable StackTraceElement location,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable thrown) {

        translator.setBasicValues(
                this,
                getName(),
                marker,
                fqcn,
                level,
                message,
                // don't construct ThrowableProxy until required
                thrown,
                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableStack(),
                location,
                clock,
                nanoClock,
                contextDataInjector,
                requiresLocation());
    }

    /**
     * LOG4J2-471: prevent deadlock when RingBuffer is full and object being logged calls Logger.log() from its
     * toString() method
     *
     * @param fqcn fully qualified caller name
     * @param level log level
     * @param marker optional marker
     * @param message log message
     * @param thrown optional exception
     */
    void logMessageInCurrentThread(
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        // bypass RingBuffer and invoke Appender directly
        final ReliabilityStrategy strategy = privateConfig.loggerConfig.getReliabilityStrategy();
        strategy.log(this, getName(), fqcn, marker, level, message, thrown);
    }

    /**
     * This method is called by the EventHandler that processes the RingBufferLogEvent in a separate thread.
     * Merges the contents of the configuration map into the contextData, after replacing any variables in the property
     * values with the StrSubstitutor-supplied actual values.
     *
     * @param event the event to log
     */
    public void actualAsyncLog(final ReusableLogEvent event) {
        final LoggerConfig privateConfigLoggerConfig = privateConfig.loggerConfig;
        final List<Property> properties = privateConfigLoggerConfig.getPropertyList();

        if (properties != null) {
            onPropertiesPresent(event, properties);
        }

        privateConfigLoggerConfig.getReliabilityStrategy().log(this, event);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach") // Avoid iterator allocation
    private void onPropertiesPresent(final ReusableLogEvent event, final List<Property> properties) {
        final StringMap contextData = getContextData(event);
        for (int i = 0, size = properties.size(); i < size; i++) {
            final Property prop = properties.get(i);
            if (contextData.getValue(prop.getName()) != null) {
                continue; // contextMap overrides config properties
            }
            final String value = prop.isValueNeedsLookup() //
                    ? privateConfig.config.getStrSubstitutor().replace(event, prop.getValue()) //
                    : prop.getValue();
            contextData.putValue(prop.getName(), value);
        }
        event.setContextData(contextData);
    }

    private static StringMap getContextData(final ReusableLogEvent event) {
        final StringMap contextData = event.getContextData();
        if (contextData.isFrozen()) {
            final StringMap temp = ContextDataFactory.createContextData();
            temp.putAll(contextData);
            return temp;
        }
        return contextData;
    }

    // package-protected for tests
    AsyncLoggerDisruptor getAsyncLoggerDisruptor() {
        return loggerDisruptor;
    }
}
