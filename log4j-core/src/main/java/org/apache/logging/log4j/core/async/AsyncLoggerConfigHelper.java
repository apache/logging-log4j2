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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * Helper class decoupling the {@code AsyncLoggerConfig} class from the LMAX Disruptor library.
 * <p>
 * {@code AsyncLoggerConfig} is a plugin, and will be loaded even if users do not configure any {@code <asyncLogger>} or
 * {@code <asyncRoot>} elements in the configuration. If {@code AsyncLoggerConfig} has inner classes that extend or
 * implement classes from the Disruptor library, a {@code NoClassDefFoundError} is thrown if the Disruptor jar is not in
 * the classpath when the PluginManager loads the {@code AsyncLoggerConfig} plugin from the pre-defined plugins
 * definition file.
 * <p>
 * This class serves to make the dependency on the Disruptor optional, so that these classes are only loaded when the
 * {@code AsyncLoggerConfig} is actually used.
 */
public class AsyncLoggerConfigHelper implements AsyncLoggerConfigDelegate {

    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;
    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * RingBuffer events contain all information necessary to perform the work in a separate thread.
     */
    private static class Log4jEventWrapper {
        private AsyncLoggerConfig loggerConfig;
        private LogEvent event;

        /**
         * Release references held by ring buffer to allow objects to be garbage-collected.
         */
        public void clear() {
            loggerConfig = null;
            event = null;
        }
    }

    /**
     * EventHandler performs the work in a separate thread.
     */
    private static class Log4jEventWrapperHandler implements SequenceReportingEventHandler<Log4jEventWrapper> {
        private static final int NOTIFY_PROGRESS_THRESHOLD = 50;
        private Sequence sequenceCallback;
        private int counter;

        @Override
        public void setSequenceCallback(final Sequence sequenceCallback) {
            this.sequenceCallback = sequenceCallback;
        }

        @Override
        public void onEvent(final Log4jEventWrapper event, final long sequence, final boolean endOfBatch)
                throws Exception {
            event.event.setEndOfBatch(endOfBatch);
            event.loggerConfig.asyncCallAppenders(event.event);
            event.clear();

            notifyIntermediateProgress(sequence);
        }

        /**
         * Notify the BatchEventProcessor that the sequence has progressed. Without this callback the sequence would not
         * be progressed until the batch has completely finished.
         */
        private void notifyIntermediateProgress(final long sequence) {
            if (++counter > NOTIFY_PROGRESS_THRESHOLD) {
                sequenceCallback.set(sequence);
                counter = 0;
            }
        }
    }

    /**
     * Factory used to populate the RingBuffer with events. These event objects are then re-used during the life of the
     * RingBuffer.
     */
    private static final EventFactory<Log4jEventWrapper> FACTORY = new EventFactory<Log4jEventWrapper>() {
        @Override
        public Log4jEventWrapper newInstance() {
            return new Log4jEventWrapper();
        }
    };

    /**
     * Object responsible for passing on data to a specific RingBuffer event.
     */
    private final static EventTranslatorTwoArg<Log4jEventWrapper, LogEvent, AsyncLoggerConfig> translator =
            new EventTranslatorTwoArg<Log4jEventWrapper, LogEvent, AsyncLoggerConfig>() {

        @Override
        public void translateTo(final Log4jEventWrapper ringBufferElement, final long sequence,
                final LogEvent logEvent, final AsyncLoggerConfig loggerConfig) {
            ringBufferElement.event = logEvent;
            ringBufferElement.loggerConfig = loggerConfig;
        }
    };

    private static ThreadFactory threadFactory = new DaemonThreadFactory("AsyncLoggerConfig-");
    private static final ThreadLocal<Boolean> isAppenderThread = new ThreadLocal<>();

    private volatile Disruptor<Log4jEventWrapper> disruptor;
    private ExecutorService executor;

    public AsyncLoggerConfigHelper() {
    }

    /**
     * Increases the reference count and creates and starts a new Disruptor and associated thread if none currently
     * exists.
     * 
     * @see #release()
     */
    public synchronized void start() {
        if (disruptor != null) {
            LOGGER.trace("AsyncLoggerConfigHelper not starting new disruptor for this configuration, using existing object.");
            return;
        }
        LOGGER.trace("AsyncLoggerConfigHelper creating new disruptor for this configuration.");
        final int ringBufferSize = calculateRingBufferSize();
        final WaitStrategy waitStrategy = createWaitStrategy();
        executor = Executors.newSingleThreadExecutor(threadFactory);
        initThreadLocalForExecutorThread(executor);
        
        disruptor = new Disruptor<>(FACTORY, ringBufferSize, executor, ProducerType.MULTI, waitStrategy);

        final ExceptionHandler<Log4jEventWrapper> errorHandler = getExceptionHandler();
        disruptor.handleExceptionsWith(errorHandler);

        final Log4jEventWrapperHandler[] handlers = {new Log4jEventWrapperHandler()};
        disruptor.handleEventsWith(handlers);

        LOGGER.debug(
                "Starting AsyncLoggerConfig disruptor for this configuration with ringbufferSize={}, waitStrategy={}, exceptionHandler={}...",
                disruptor.getRingBuffer().getBufferSize(), waitStrategy.getClass().getSimpleName(), errorHandler);
        disruptor.start();
    }

    private static WaitStrategy createWaitStrategy() {
        final String strategy = System.getProperty("AsyncLoggerConfig.WaitStrategy");
        if (strategy != null) {
	        LOGGER.trace("property AsyncLoggerConfig.WaitStrategy={}", strategy);
	        if ("Sleep".equals(strategy)) {
	            return new SleepingWaitStrategy();
	        } else if ("Yield".equals(strategy)) {
	            return new YieldingWaitStrategy();
	        } else if ("Block".equals(strategy)) {
	            return new BlockingWaitStrategy();
	        }
        }
        return new BlockingWaitStrategy();
    }

    private static int calculateRingBufferSize() {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize = PropertiesUtil.getProperties().getStringProperty(
                "AsyncLoggerConfig.RingBufferSize", String.valueOf(ringBufferSize));
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
        return Integers.ceilingNextPowerOfTwo(ringBufferSize);
    }

    private static ExceptionHandler<Log4jEventWrapper> getExceptionHandler() {
        final String cls = System.getProperty("AsyncLoggerConfig.ExceptionHandler");
        if (cls == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends ExceptionHandler<Log4jEventWrapper>> klass =
                    (Class<? extends ExceptionHandler<Log4jEventWrapper>>) Class.forName(cls);
            return klass.newInstance();
        } catch (final Exception ignored) {
            LOGGER.debug("AsyncLoggerConfig.ExceptionHandler not set: error creating " + cls + ": ", ignored);
            return null;
        }
    }

    /**
     * Decreases the reference count. If the reference count reached zero, the Disruptor and its associated thread are
     * shut down and their references set to {@code null}.
     */
    public synchronized void stop() {
        final Disruptor<Log4jEventWrapper> temp = disruptor;
        if (temp == null) {
            LOGGER.trace("AsyncLoggerConfigHelper: disruptor for this configuration already shut down.");
            return; // disruptor was already shut down by another thread
        }
        LOGGER.trace("AsyncLoggerConfigHelper: shutting down disruptor for this configuration.");

        // We must guarantee that publishing to the RingBuffer has stopped before we call disruptor.shutdown().
        disruptor = null; // client code fails with NPE if log after stop = OK

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

        LOGGER.trace("AsyncLoggerConfigHelper: shutting down disruptor executor for this configuration.");
        executor.shutdown(); // finally, kill the processor thread
        executor = null; // release reference to allow GC
    }

    /**
     * Returns {@code true} if the specified disruptor still has unprocessed events.
     */
    private static boolean hasBacklog(final Disruptor<?> theDisruptor) {
        final RingBuffer<?> ringBuffer = theDisruptor.getRingBuffer();
        return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
    }

    /**
     * Initialize the threadlocal that allows us to detect Logger.log() calls initiated from the appender thread, which
     * may cause deadlock when the RingBuffer is full. (LOG4J2-471)
     * @param executor contains the appender background thread
     */
    private static void initThreadLocalForExecutorThread(final ExecutorService executor) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                isAppenderThread.set(Boolean.TRUE);
            }
        });
    }

    /* (non-Javadoc)
     * @see org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate#tryCallAppendersInBackground(org.apache.logging.log4j.core.LogEvent)
     */
    @Override
    public boolean tryCallAppendersInBackground(final LogEvent event, final AsyncLoggerConfig asyncLoggerConfig) {
        final Disruptor<Log4jEventWrapper> temp = disruptor;
        if (!hasLog4jBeenShutDown(temp)) {

            // LOG4J2-471: prevent deadlock when RingBuffer is full and object
            // being logged calls Logger.log() from its toString() method
            if (isCalledFromAppenderThreadAndBufferFull(temp)) {
                // bypass RingBuffer and invoke Appender directly
                return false;
            }
            enqueueEvent(event, asyncLoggerConfig);
        }
        return true;
    }

    /**
     * Returns {@code true} if the specified disruptor is null.
     */
    private boolean hasLog4jBeenShutDown(final Disruptor<Log4jEventWrapper> aDisruptor) {
        if (aDisruptor == null) { // LOG4J2-639
            LOGGER.fatal("Ignoring log event after log4j was shut down");
            return true;
        }
        return false;
    }

    private void enqueueEvent(final LogEvent event, final AsyncLoggerConfig asyncLoggerConfig) {
        // LOG4J2-639: catch NPE if disruptor field was set to null after our check above
        try {
            final LogEvent logEvent = prepareEvent(event);
            enqueue(logEvent, asyncLoggerConfig);
        } catch (final NullPointerException npe) {
            LOGGER.fatal("Ignoring log event after log4j was shut down.");
        }
    }

    private LogEvent prepareEvent(final LogEvent event) {
        final LogEvent logEvent = ensureImmutable(event);
        logEvent.getMessage().getFormattedMessage(); // LOG4J2-763: ask message to freeze parameters
        return logEvent;
    }

    private void enqueue(final LogEvent logEvent, final AsyncLoggerConfig asyncLoggerConfig) {
        // Note: do NOT use the temp variable above!
        // That could result in adding a log event to the disruptor after it was shut down,
        // which could cause the publishEvent method to hang and never return.
        disruptor.getRingBuffer().publishEvent(translator, logEvent, asyncLoggerConfig);
    }

    private LogEvent ensureImmutable(final LogEvent event) {
        LogEvent result = event;
        if (event instanceof RingBufferLogEvent) {
            // Deal with special case where both types of Async Loggers are used together:
            // RingBufferLogEvents are created by the all-loggers-async type, but
            // this event is also consumed by the some-loggers-async type (this class).
            // The original event will be re-used and modified in an application thread later,
            // so take a snapshot of it, which can be safely processed in the
            // some-loggers-async background thread.
            result = ((RingBufferLogEvent) event).createMemento();
        }
        return result;
    }

    /**
     * Returns true if the specified ringbuffer is full and the Logger.log() call was made from the appender thread.
     */
    private boolean isCalledFromAppenderThreadAndBufferFull(Disruptor<Log4jEventWrapper> theDisruptor) {
        return isAppenderThread.get() == Boolean.TRUE && theDisruptor.getRingBuffer().remainingCapacity() == 0;
    }

    /* (non-Javadoc)
     * @see org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate#createRingBufferAdmin(java.lang.String, java.lang.String)
     */
    @Override
    public RingBufferAdmin createRingBufferAdmin(final String contextName, final String loggerConfigName) {
        return RingBufferAdmin.forAsyncLoggerConfig(disruptor.getRingBuffer(), contextName, loggerConfigName);
    }

}
