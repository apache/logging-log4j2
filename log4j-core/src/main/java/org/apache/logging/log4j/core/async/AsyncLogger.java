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

import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.status.StatusLogger;

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
public class AsyncLogger extends Logger {

    private static final long serialVersionUID = 1L;
    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static final Clock CLOCK = ClockFactory.getClock();
    private static volatile NanoClock nanoClock = new DummyNanoClock();

    /**
     * Constructs an {@code AsyncLogger} with the specified context, name and message factory.
     *
     * @param context context of this logger
     * @param name name of this logger
     * @param messageFactory message factory of this logger
     */
    public AsyncLogger(final LoggerContext context, final String name, final MessageFactory messageFactory) {
        super(context, name, messageFactory);
    }

    @Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message,
            final Throwable thrown) {

        final Disruptor<RingBufferLogEvent> temp = AsyncLoggerHelper.getDisruptor();
        if (temp == null) { // LOG4J2-639
            LOGGER.fatal("Ignoring log event after log4j was shut down");
        } else {
            logMessage0(temp, fqcn, level, marker, message, thrown);
        }
    }

    private void logMessage0(final Disruptor<RingBufferLogEvent> theDisruptor, final String fqcn, final Level level,
            final Marker marker, final Message message, final Throwable thrown) {
        final Info info = Info.get();
        logMessageInAppropriateThread(info, theDisruptor, fqcn, level, marker, message, thrown);
    }

    private void logMessageInAppropriateThread(final Info info, final Disruptor<RingBufferLogEvent> theDisruptor,
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        if (!logMessageInCurrentThread(info, theDisruptor, fqcn, level, marker, message, thrown)) {
            logMessageInBackgroundThread(info, fqcn, level, marker, message, thrown);
        }
    }

    /**
     * LOG4J2-471: prevent deadlock when RingBuffer is full and object being logged calls Logger.log() from its
     * toString() method
     *
     * @param info threadlocal information - used to determine if the current thread is the background appender thread
     * @param theDisruptor used to check if the buffer is full
     * @param fqcn fully qualified caller name
     * @param level log level
     * @param marker optional marker
     * @param message log message
     * @param thrown optional exception
     * @return {@code true} if the event has been logged in the current thread, {@code false} if it should be passed to
     *         the background thread
     */
    private boolean logMessageInCurrentThread(Info info, final Disruptor<RingBufferLogEvent> theDisruptor,
            final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        if (info.isAppenderThread && theDisruptor.getRingBuffer().remainingCapacity() == 0) {
            // bypass RingBuffer and invoke Appender directly
            final ReliabilityStrategy strategy = privateConfig.loggerConfig.getReliabilityStrategy();
            strategy.log(this, getName(), fqcn, marker, level, message, thrown);
            return true;
        }
        return false;
    }

    /**
     * Enqueues the specified message to be logged in the background thread.
     * 
     * @param info holds some cached information
     * @param fqcn fully qualified caller name
     * @param level log level
     * @param marker optional marker
     * @param message log message
     * @param thrown optional exception
     */
    private void logMessageInBackgroundThread(Info info, final String fqcn, final Level level, final Marker marker,
            final Message message, final Throwable thrown) {

        message.getFormattedMessage(); // LOG4J2-763: ask message to freeze parameters

        initLogMessageInfo(info, fqcn, level, marker, message, thrown);
        AsyncLoggerHelper.enqueueLogMessageInfo(info.translator);
    }

    private void initLogMessageInfo(Info info, final String fqcn, final Level level, final Marker marker,
            final Message message, final Throwable thrown) {
        info.translator.setValues(this, getName(), marker, fqcn, level, message, //
                // don't construct ThrowableProxy until required
                thrown, //

                // config properties are taken care of in the EventHandler
                // thread in the #actualAsyncLog method

                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableContext(), //

                // needs shallow copy to be fast (LOG4J2-154)
                ThreadContext.getImmutableStack(), //

                // Thread.currentThread().getName(), //
                // info.cachedThreadName, //
                info.threadName(), //

                // location: very expensive operation. LOG4J2-153:
                // Only include if "includeLocation=true" is specified,
                // exclude if not specified or if "false" was specified.
                calcLocationIfRequested(fqcn),

                // System.currentTimeMillis());
                // CoarseCachedClock: 20% faster than system clock, 16ms gaps
                // CachedClock: 10% faster than system clock, smaller gaps
                // LOG4J2-744 avoid calling clock altogether if message has the timestamp
                eventTimeMillis(message), //
                nanoClock.nanoTime() //
                );
    }

    private long eventTimeMillis(final Message message) {
        return message instanceof TimestampMessage ? ((TimestampMessage) message).getTimestamp() : CLOCK
                .currentTimeMillis();
    }

    /**
     * Returns the caller location if requested, {@code null} otherwise.
     * 
     * @param fqcn fully qualified caller name.
     * @return the caller location if requested, {@code null} otherwise.
     */
    private StackTraceElement calcLocationIfRequested(String fqcn) {
        final boolean includeLocation = privateConfig.loggerConfig.isIncludeLocation();
        return includeLocation ? location(fqcn) : null;
    }

    private static StackTraceElement location(final String fqcnOfLogger) {
        return Log4jLogEvent.calcLocation(fqcnOfLogger);
    }

    /**
     * This method is called by the EventHandler that processes the RingBufferLogEvent in a separate thread.
     *
     * @param event the event to log
     */
    public void actualAsyncLog(final RingBufferLogEvent event) {
        final Map<Property, Boolean> properties = privateConfig.loggerConfig.getProperties();
        event.mergePropertiesIntoContextMap(properties, privateConfig.config.getStrSubstitutor());
        final ReliabilityStrategy strategy = privateConfig.loggerConfig.getReliabilityStrategy();
        strategy.log(this, event);
    }

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the ringbuffer of the {@code AsyncLogger}.
     *
     * @param contextName name of the global {@code AsyncLoggerContext}
     * @return a new {@code RingBufferAdmin} that instruments the ringbuffer
     */
    public static RingBufferAdmin createRingBufferAdmin(final String contextName) {
        return AsyncLoggerHelper.createRingBufferAdmin(contextName);
    }

    /**
     * Returns the {@code NanoClock} to use for creating the nanoTime timestamp of log events.
     * 
     * @return the {@code NanoClock} to use for creating the nanoTime timestamp of log events
     */
    public static NanoClock getNanoClock() {
        return nanoClock;
    }

    /**
     * Sets the {@code NanoClock} to use for creating the nanoTime timestamp of log events.
     * <p>
     * FOR INTERNAL USE. This method may be called with a different {@code NanoClock} implementation when the
     * configuration changes.
     * 
     * @param nanoClock the {@code NanoClock} to use for creating the nanoTime timestamp of log events
     */
    public static void setNanoClock(NanoClock nanoClock) {
        AsyncLogger.nanoClock = Objects.requireNonNull(nanoClock, "NanoClock must be non-null");
    }
}
