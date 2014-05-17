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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.status.StatusLogger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.Util;

/**
 * AsyncLogger is a logger designed for high throughput and low latency logging.
 * It does not perform any I/O in the calling (application) thread, but instead
 * hands off the work to another thread as soon as possible. The actual logging
 * is performed in the background thread. It uses the LMAX Disruptor library for
 * inter-thread communication. (<a
 * href="http://lmax-exchange.github.com/disruptor/"
 * >http://lmax-exchange.github.com/disruptor/</a>)
 * <p>
 * To use AsyncLogger, specify the System property
 * {@code -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector}
 * before you obtain a Logger, and all Loggers returned by LogManager.getLogger
 * will be AsyncLoggers.
 * <p>
 * Note that for performance reasons, this logger does not include source
 * location by default. You need to specify {@code includeLocation="true"} in
 * the configuration or any %class, %location or %line conversion patterns in
 * your log4j.xml configuration will produce either a "?" character or no output
 * at all.
 * <p>
 * For best performance, use AsyncLogger with the RandomAccessFileAppender or
 * RollingRandomAccessFileAppender, with immediateFlush=false. These appenders
 * have built-in support for the batching mechanism used by the Disruptor
 * library, and they will flush to disk at the end of each batch. This means
 * that even with immediateFlush=false, there will never be any items left in
 * the buffer; all log events will all be written to disk in a very efficient
 * manner.
 */
public class AsyncLogger extends Logger {
    private static final long serialVersionUID = 1L;
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final ThreadNameStrategy THREAD_NAME_STRATEGY = ThreadNameStrategy.create();
    private static final ThreadLocal<Info> threadlocalInfo = new ThreadLocal<Info>();

    static enum ThreadNameStrategy { // LOG4J2-467
        CACHED {
            @Override
            public String getThreadName(final Info info) {
                return info.cachedThreadName;
            }
        },
        UNCACHED {
            @Override
            public String getThreadName(final Info info) {
                return Thread.currentThread().getName();
            }
        };
        abstract String getThreadName(Info info);

        static ThreadNameStrategy create() {
            final String name = System.getProperty("AsyncLogger.ThreadNameStrategy", CACHED.name());
            try {
                return ThreadNameStrategy.valueOf(name);
            } catch (final Exception ex) {
                LOGGER.debug("Using AsyncLogger.ThreadNameStrategy.CACHED: '{}' not valid: {}", name, ex.toString());
                return CACHED;
            }
        }
    }
    private static volatile Disruptor<RingBufferLogEvent> disruptor;
    private static final Clock clock = ClockFactory.getClock();

    private static final ExecutorService executor = Executors
            .newSingleThreadExecutor(new DaemonThreadFactory("AsyncLogger-"));

    static {
        initInfoForExecutorThread();
        LOGGER.debug("AsyncLogger.ThreadNameStrategy={}", THREAD_NAME_STRATEGY);
        final int ringBufferSize = calculateRingBufferSize();

        final WaitStrategy waitStrategy = createWaitStrategy();
        disruptor = new Disruptor<RingBufferLogEvent>(RingBufferLogEvent.FACTORY, ringBufferSize, executor,
                ProducerType.MULTI, waitStrategy);
        disruptor.handleExceptionsWith(getExceptionHandler());
        disruptor.handleEventsWith(new RingBufferLogEventHandler());

        LOGGER.debug("Starting AsyncLogger disruptor with ringbuffer size {}...", disruptor.getRingBuffer()
                .getBufferSize());
        disruptor.start();
    }

    private static int calculateRingBufferSize() {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize = System.getProperty("AsyncLogger.RingBufferSize",
                String.valueOf(ringBufferSize));
        try {
            int size = Integer.parseInt(userPreferredRBSize);
            if (size < RINGBUFFER_MIN_SIZE) {
                size = RINGBUFFER_MIN_SIZE;
                LOGGER.warn("Invalid RingBufferSize {}, using minimum size {}.", userPreferredRBSize,
                        RINGBUFFER_MIN_SIZE);
            }
            ringBufferSize = size;
        } catch (final Exception ex) {
            LOGGER.warn("Invalid RingBufferSize {}, using default size {}.", userPreferredRBSize, ringBufferSize);
        }
        return Util.ceilingNextPowerOfTwo(ringBufferSize);
    }

    /**
     * Initialize an {@code Info} object that is threadlocal to the consumer/appender thread.
     * This Info object uniquely has attribute {@code isAppenderThread} set to {@code true}.
     * All other Info objects will have this attribute set to {@code false}.
     * This allows us to detect Logger.log() calls initiated from the appender thread,
     * which may cause deadlock when the RingBuffer is full. (LOG4J2-471)
     */
    private static void initInfoForExecutorThread() {
        executor.submit(new Runnable(){
            @Override
            public void run() {
                final boolean isAppenderThread = true;
                final Info info = new Info(new RingBufferLogEventTranslator(), //
                        Thread.currentThread().getName(), isAppenderThread);
                threadlocalInfo.set(info);
            }
        });
    }

    private static WaitStrategy createWaitStrategy() {
        final String strategy = System.getProperty("AsyncLogger.WaitStrategy");
        LOGGER.debug("property AsyncLogger.WaitStrategy={}", strategy);
        if ("Sleep".equals(strategy)) {
            return new SleepingWaitStrategy();
        } else if ("Yield".equals(strategy)) {
            return new YieldingWaitStrategy();
        } else if ("Block".equals(strategy)) {
            return new BlockingWaitStrategy();
        }
        LOGGER.debug("disruptor event handler uses BlockingWaitStrategy");
        return new BlockingWaitStrategy();
    }

    private static ExceptionHandler getExceptionHandler() {
        final String cls = System.getProperty("AsyncLogger.ExceptionHandler");
        if (cls == null) {
            LOGGER.debug("No AsyncLogger.ExceptionHandler specified");
            return null;
        }
        try {
            final ExceptionHandler result = Loader.newCheckedInstanceOf(cls, ExceptionHandler.class);
            LOGGER.debug("AsyncLogger.ExceptionHandler={}", result);
            return result;
        } catch (final Exception ignored) {
            LOGGER.debug("AsyncLogger.ExceptionHandler not set: error creating " + cls + ": ", ignored);
            return null;
        }
    }

    /**
     * Constructs an {@code AsyncLogger} with the specified context, name and
     * message factory.
     *
     * @param context context of this logger
     * @param name name of this logger
     * @param messageFactory message factory of this logger
     */
    public AsyncLogger(final LoggerContext context, final String name, final MessageFactory messageFactory) {
        super(context, name, messageFactory);
    }

    /**
     * Tuple with the event translator and thread name for a thread.
     */
    static class Info {
        private final RingBufferLogEventTranslator translator;
        private final String cachedThreadName;
        private final boolean isAppenderThread;
        public Info(final RingBufferLogEventTranslator translator, final String threadName, final boolean appenderThread) {
            this.translator = translator;
            this.cachedThreadName = threadName;
            this.isAppenderThread = appenderThread;
        }
    }

    @Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable thrown) {
        Info info = threadlocalInfo.get();
        if (info == null) {
            info = new Info(new RingBufferLogEventTranslator(), Thread.currentThread().getName(), false);
            threadlocalInfo.set(info);
        }
        
        Disruptor<RingBufferLogEvent> temp = disruptor;
        if (temp == null) { // LOG4J2-639
            LOGGER.fatal("Ignoring log event after log4j was shut down");
            return;
        }

        // LOG4J2-471: prevent deadlock when RingBuffer is full and object
        // being logged calls Logger.log() from its toString() method
        if (info.isAppenderThread && temp.getRingBuffer().remainingCapacity() == 0) {
            // bypass RingBuffer and invoke Appender directly
            config.loggerConfig.log(getName(), fqcn, marker, level, message, thrown);
            return;
        }
        final boolean includeLocation = config.loggerConfig.isIncludeLocation();
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
                THREAD_NAME_STRATEGY.getThreadName(info), // LOG4J2-467

                // location: very expensive operation. LOG4J2-153:
                // Only include if "includeLocation=true" is specified,
                // exclude if not specified or if "false" was specified.
                includeLocation ? location(fqcn) : null,

                // System.currentTimeMillis());
                // CoarseCachedClock: 20% faster than system clock, 16ms gaps
                // CachedClock: 10% faster than system clock, smaller gaps
                clock.currentTimeMillis());

        // LOG4J2-639: catch NPE if disruptor field was set to null after our check above
        try {
            // Note: do NOT use the temp variable above!
            // That could result in adding a log event to the disruptor after it was shut down,
            // which could cause the publishEvent method to hang and never return.
            disruptor.publishEvent(info.translator);
        } catch (NullPointerException npe) {
            LOGGER.fatal("Ignoring log event after log4j was shut down.");
        }
    }

    private static StackTraceElement location(final String fqcnOfLogger) {
        return Log4jLogEvent.calcLocation(fqcnOfLogger);
    }

    /**
     * This method is called by the EventHandler that processes the
     * RingBufferLogEvent in a separate thread.
     *
     * @param event the event to log
     */
    public void actualAsyncLog(final RingBufferLogEvent event) {
        final Map<Property, Boolean> properties = config.loggerConfig.getProperties();
        event.mergePropertiesIntoContextMap(properties, config.config.getStrSubstitutor());
        config.logEvent(event);
    }

    public static void stop() {
        final Disruptor<RingBufferLogEvent> temp = disruptor;

        // Must guarantee that publishing to the RingBuffer has stopped
        // before we call disruptor.shutdown()
        disruptor = null; // client code fails with NPE if log after stop = OK
        if (temp == null) {
            return; // stop() has already been called
        }

        // Calling Disruptor.shutdown() will wait until all enqueued events are fully processed,
        // but this waiting happens in a busy-spin. To avoid (postpone) wasting CPU,
        // we sleep in short chunks, up to 10 seconds, waiting for the ringbuffer to drain.
        for (int i = 0; hasBacklog(temp) && i < MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN; i++) {
            try {
                Thread.sleep(SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS); // give up the CPU for a while
            } catch (final InterruptedException e) { // ignored
            }
        }
        temp.shutdown(); // busy-spins until all events currently in the disruptor have been processed
        executor.shutdown(); // finally, kill the processor thread
        threadlocalInfo.remove(); // LOG4J2-323
    }

    /**
     * Returns {@code true} if the specified disruptor still has unprocessed events.
     */
    private static boolean hasBacklog(Disruptor<?> disruptor) {
        final RingBuffer<?> ringBuffer = disruptor.getRingBuffer();
        return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
    }

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the
     * ringbuffer of the {@code AsyncLogger}.
     *
     * @param contextName name of the global {@code AsyncLoggerContext}
     */
    public static RingBufferAdmin createRingBufferAdmin(final String contextName) {
        return RingBufferAdmin.forAsyncLogger(disruptor.getRingBuffer(), contextName);
    }
}
