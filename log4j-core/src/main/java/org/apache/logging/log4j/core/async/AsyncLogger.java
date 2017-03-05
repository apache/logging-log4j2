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
package org.apache.logging.log4j.core.async;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.status.StatusLogger;

import com.lmax.disruptor.EventTranslatorVararg;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * AsyncLogger is a logger designed for high throughput and low latency logging. It does not perform any I/O in the
 * calling (application) thread, but instead hands off the work to another thread as soon as possible. The actual
 * logging is performed in the background thread. It uses the LMAX Disruptor library for inter-thread communication. (<a
 * href="http://lmax-exchange.github.com/disruptor/" >http://lmax-exchange.github.com/disruptor/</a>)
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
public class AsyncLogger extends Logger implements EventTranslatorVararg<RingBufferLogEvent> {
    // Implementation note: many methods in this class are tuned for performance. MODIFY WITH CARE!
    // Specifically, try to keep the hot methods to 35 bytecodes or less:
    // this is within the MaxInlineSize threshold and makes these methods candidates for
    // immediate inlining instead of waiting until they are designated "hot enough".

    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final Clock CLOCK = ClockFactory.getClock(); // not reconfigurable
    private static final ContextDataInjector CONTEXT_DATA_INJECTOR = ContextDataInjectorFactory.createInjector();

    private static final ThreadNameCachingStrategy THREAD_NAME_CACHING_STRATEGY = ThreadNameCachingStrategy.create();

    private final ThreadLocal<RingBufferLogEventTranslator> threadLocalTranslator = new ThreadLocal<>();
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
    public AsyncLogger(final LoggerContext context, final String name, final MessageFactory messageFactory,
            final AsyncLoggerDisruptor loggerDisruptor) {
        super(context, name, messageFactory);
        this.loggerDisruptor = loggerDisruptor;
        includeLocation = privateConfig.loggerConfig.isIncludeLocation();
        nanoClock = context.getConfiguration().getNanoClock();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.Logger#updateConfiguration(org.apache.logging.log4j.core.config.Configuration)
     */
    @Override
    protected void updateConfiguration(final Configuration newConfig) {
        nanoClock = newConfig.getNanoClock();
        includeLocation = newConfig.getLoggerConfig(name).isIncludeLocation();
        super.updateConfiguration(newConfig);
    }

    // package protected for unit tests
    NanoClock getNanoClock() {
        return nanoClock;
    }

    private RingBufferLogEventTranslator getCachedTranslator() {
        RingBufferLogEventTranslator result = threadLocalTranslator.get();
        if (result == null) {
            result = new RingBufferLogEventTranslator();
            threadLocalTranslator.set(result);
        }
        return result;
    }

    @Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message,
            final Throwable thrown) {

        if (loggerDisruptor.isUseThreadLocals()) {
            logWithThreadLocalTranslator(fqcn, level, marker, message, thrown);
        } else {
            // LOG4J2-1172: avoid storing non-JDK classes in ThreadLocals to avoid memory leaks in web apps
            logWithVarargTranslator(fqcn, level, marker, message, thrown);
        }
    }

    private boolean isReused(final Message message) {
        return message instanceof ReusableMessage;
    }

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This re-uses a {@code RingBufferLogEventTranslator} instance cached in a {@code ThreadLocal} to avoid creating
     * unnecessary objects with each event.
     *
     * @param fqcn fully qualified name of the caller
     * @param level level at which the caller wants to log the message
     * @param marker message marker
     * @param message the log message
     * @param thrown a {@code Throwable} or {@code null}
     */
    private void logWithThreadLocalTranslator(final String fqcn, final Level level, final Marker marker,
            final Message message, final Throwable thrown) {
        // Implementation note: this method is tuned for performance. MODIFY WITH CARE!

        final RingBufferLogEventTranslator translator = getCachedTranslator();
        initTranslator(translator, fqcn, level, marker, message, thrown);
        initTranslatorThreadValues(translator);
        publish(translator);
    }

    private void publish(final RingBufferLogEventTranslator translator) {
        if (!loggerDisruptor.tryPublish(translator)) {
            handleRingBufferFull(translator);
        }
    }

    private void handleRingBufferFull(final RingBufferLogEventTranslator translator) {
        final EventRoute eventRoute = loggerDisruptor.getEventRoute(translator.level);
        switch (eventRoute) {
            case ENQUEUE:
                loggerDisruptor.enqueueLogMessageInfo(translator);
                break;
            case SYNCHRONOUS:
                logMessageInCurrentThread(translator.fqcn, translator.level, translator.marker, translator.message,
                        translator.thrown);
                break;
            case DISCARD:
                break;
            default:
                throw new IllegalStateException("Unknown EventRoute " + eventRoute);
        }
    }

    private void initTranslator(final RingBufferLogEventTranslator translator, final String fqcn,
            final Level level, final Marker marker, final Message message, final Throwable thrown) {

        translator.setBasicValues(this, name, marker, fqcn, level, message, //
                // don't construct ThrowableProxy until required
                thrown,

                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableStack(), //

                // location (expensive to calculate)
                calcLocationIfRequested(fqcn), //
                CLOCK.currentTimeMillis(), //
                nanoClock.nanoTime() //
        );
    }

    private void initTranslatorThreadValues(final RingBufferLogEventTranslator translator) {
        // constant check should be optimized out when using default (CACHED)
        if (THREAD_NAME_CACHING_STRATEGY == ThreadNameCachingStrategy.UNCACHED) {
            translator.updateThreadValues();
        }
    }

    /**
     * Returns the caller location if requested, {@code null} otherwise.
     *
     * @param fqcn fully qualified caller name.
     * @return the caller location if requested, {@code null} otherwise.
     */
    private StackTraceElement calcLocationIfRequested(final String fqcn) {
        // location: very expensive operation. LOG4J2-153:
        // Only include if "includeLocation=true" is specified,
        // exclude if not specified or if "false" was specified.
        return includeLocation ? Log4jLogEvent.calcLocation(fqcn) : null;
    }

    /**
     * Enqueues the specified log event data for logging in a background thread.
     * <p>
     * This creates a new varargs Object array for each invocation, but does not store any non-JDK classes in a
     * {@code ThreadLocal} to avoid memory leaks in web applications (see LOG4J2-1172).
     *
     * @param fqcn fully qualified name of the caller
     * @param level level at which the caller wants to log the message
     * @param marker message marker
     * @param message the log message
     * @param thrown a {@code Throwable} or {@code null}
     */
    private void logWithVarargTranslator(final String fqcn, final Level level, final Marker marker,
            final Message message, final Throwable thrown) {
        // Implementation note: candidate for optimization: exceeds 35 bytecodes.

        final Disruptor<RingBufferLogEvent> disruptor = loggerDisruptor.getDisruptor();
        if (disruptor == null) {
            LOGGER.error("Ignoring log event after Log4j has been shut down.");
            return;
        }
        // if the Message instance is reused, there is no point in freezing its message here
        if (!canFormatMessageInBackground(message) && !isReused(message)) {
            message.getFormattedMessage(); // LOG4J2-763: ask message to freeze parameters
        }
        // calls the translateTo method on this AsyncLogger
        disruptor.getRingBuffer().publishEvent(this, this, calcLocationIfRequested(fqcn), fqcn, level, marker, message,
                thrown);
    }

    private boolean canFormatMessageInBackground(final Message message) {
        return Constants.FORMAT_MESSAGES_IN_BACKGROUND // LOG4J2-898: user wants to format all msgs in background
                || message.getClass().isAnnotationPresent(AsynchronouslyFormattable.class); // LOG4J2-1718
    }

    /*
     * (non-Javadoc)
     *
     * @see com.lmax.disruptor.EventTranslatorVararg#translateTo(java.lang.Object, long, java.lang.Object[])
     */
    @Override
    public void translateTo(final RingBufferLogEvent event, final long sequence, final Object... args) {
        // Implementation note: candidate for optimization: exceeds 35 bytecodes.
        final AsyncLogger asyncLogger = (AsyncLogger) args[0];
        final StackTraceElement location = (StackTraceElement) args[1];
        final String fqcn = (String) args[2];
        final Level level = (Level) args[3];
        final Marker marker = (Marker) args[4];
        final Message message = (Message) args[5];
        final Throwable thrown = (Throwable) args[6];

        // needs shallow copy to be fast (LOG4J2-154)
        final ContextStack contextStack = ThreadContext.getImmutableStack();

        final Thread currentThread = Thread.currentThread();
        final String threadName = THREAD_NAME_CACHING_STRATEGY.getThreadName();
        event.setValues(asyncLogger, asyncLogger.getName(), marker, fqcn, level, message, thrown,
                // config properties are taken care of in the EventHandler thread
                // in the AsyncLogger#actualAsyncLog method
                CONTEXT_DATA_INJECTOR.injectContextData(null, (StringMap) event.getContextData()),
                contextStack, currentThread.getId(), threadName, currentThread.getPriority(), location,
                CLOCK.currentTimeMillis(), nanoClock.nanoTime());
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
    void logMessageInCurrentThread(final String fqcn, final Level level, final Marker marker,
            final Message message, final Throwable thrown) {
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
    public void actualAsyncLog(final RingBufferLogEvent event) {
        final List<Property> properties = privateConfig.loggerConfig.getPropertyList();

        if (properties != null) {
            StringMap contextData = (StringMap) event.getContextData();
            if (contextData.isFrozen()) {
                final StringMap temp = ContextDataFactory.createContextData();
                temp.putAll(contextData);
                contextData = temp;
            }
            for (int i = 0; i < properties.size(); i++) {
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

        final ReliabilityStrategy strategy = privateConfig.loggerConfig.getReliabilityStrategy();
        strategy.log(this, event);
    }
}
