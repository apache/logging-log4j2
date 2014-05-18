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
import org.apache.logging.log4j.status.StatusLogger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
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
import com.lmax.disruptor.util.Util;

/**
 * Helper class decoupling the {@code AsyncLoggerConfig} class from the LMAX
 * Disruptor library.
 * <p>
 * {@code AsyncLoggerConfig} is a plugin, and will be loaded even if users do
 * not configure any {@code <asyncLogger>} or {@code <asyncRoot>} elements in
 * the configuration. If {@code AsyncLoggerConfig} has inner classes that extend
 * or implement classes from the Disruptor library, a
 * {@code NoClassDefFoundError} is thrown if the Disruptor jar is not in the
 * classpath when the PluginManager loads the {@code AsyncLoggerConfig} plugin
 * from the pre-defined plugins definition file.
 * <p>
 * This class serves to make the dependency on the Disruptor optional, so that
 * these classes are only loaded when the {@code AsyncLoggerConfig} is actually
 * used.
 */
class AsyncLoggerConfigHelper {

    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static ThreadFactory threadFactory = new DaemonThreadFactory("AsyncLoggerConfig-");
    private static volatile Disruptor<Log4jEventWrapper> disruptor;
    private static ExecutorService executor;

    private static volatile int count = 0;
    private static ThreadLocal<Boolean> isAppenderThread = new ThreadLocal<Boolean>();

    /**
     * Factory used to populate the RingBuffer with events. These event objects
     * are then re-used during the life of the RingBuffer.
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
    private final EventTranslatorTwoArg<Log4jEventWrapper, LogEvent, AsyncLoggerConfig> translator 
            = new EventTranslatorTwoArg<Log4jEventWrapper, LogEvent, AsyncLoggerConfig>() {

        @Override
        public void translateTo(Log4jEventWrapper ringBufferElement, long sequence, 
                LogEvent logEvent, AsyncLoggerConfig loggerConfig) {
            ringBufferElement.event = logEvent;
            ringBufferElement.loggerConfig = loggerConfig;
        }
    };

    private final AsyncLoggerConfig asyncLoggerConfig;

    public AsyncLoggerConfigHelper(final AsyncLoggerConfig asyncLoggerConfig) {
        this.asyncLoggerConfig = asyncLoggerConfig;
        claim();
    }

    private static synchronized void initDisruptor() {
        if (disruptor != null) {
            LOGGER.trace("AsyncLoggerConfigHelper not starting new disruptor, using existing object. Ref count is {}.", count);
            return;
        }
        LOGGER.trace("AsyncLoggerConfigHelper creating new disruptor. Ref count is {}.", count);
        final int ringBufferSize = calculateRingBufferSize();
        final WaitStrategy waitStrategy = createWaitStrategy();
        executor = Executors.newSingleThreadExecutor(threadFactory);
        initThreadLocalForExecutorThread();
        disruptor = new Disruptor<Log4jEventWrapper>(FACTORY, ringBufferSize,
                executor, ProducerType.MULTI, waitStrategy);
        final EventHandler<Log4jEventWrapper>[] handlers = new Log4jEventWrapperHandler[] {//
        new Log4jEventWrapperHandler() };
        final ExceptionHandler errorHandler = getExceptionHandler();
        disruptor.handleExceptionsWith(errorHandler);
        disruptor.handleEventsWith(handlers);

        LOGGER.debug(
                "Starting AsyncLoggerConfig disruptor with ringbuffer size={}, waitStrategy={}, exceptionHandler={}...",
                disruptor.getRingBuffer().getBufferSize(), waitStrategy.getClass().getSimpleName(), errorHandler);
        disruptor.start();
    }

    private static WaitStrategy createWaitStrategy() {
        final String strategy = System
                .getProperty("AsyncLoggerConfig.WaitStrategy");
        LOGGER.debug("property AsyncLoggerConfig.WaitStrategy={}", strategy);
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

    private static int calculateRingBufferSize() {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize = System.getProperty(
                "AsyncLoggerConfig.RingBufferSize",
                String.valueOf(ringBufferSize));
        try {
            int size = Integer.parseInt(userPreferredRBSize);
            if (size < RINGBUFFER_MIN_SIZE) {
                size = RINGBUFFER_MIN_SIZE;
                LOGGER.warn(
                        "Invalid RingBufferSize {}, using minimum size {}.",
                        userPreferredRBSize, RINGBUFFER_MIN_SIZE);
            }
            ringBufferSize = size;
        } catch (final Exception ex) {
            LOGGER.warn("Invalid RingBufferSize {}, using default size {}.",
                    userPreferredRBSize, ringBufferSize);
        }
        return Util.ceilingNextPowerOfTwo(ringBufferSize);
    }

    private static ExceptionHandler getExceptionHandler() {
        final String cls = System
                .getProperty("AsyncLoggerConfig.ExceptionHandler");
        if (cls == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends ExceptionHandler> klass = (Class<? extends ExceptionHandler>) Class
                    .forName(cls);
            final ExceptionHandler result = klass.newInstance();
            return result;
        } catch (final Exception ignored) {
            LOGGER.debug(
                    "AsyncLoggerConfig.ExceptionHandler not set: error creating "
                            + cls + ": ", ignored);
            return null;
        }
    }

    /**
     * RingBuffer events contain all information necessary to perform the work
     * in a separate thread.
     */
    private static class Log4jEventWrapper {
        private AsyncLoggerConfig loggerConfig;
        private LogEvent event;

        /**
         * Release references held by ring buffer to allow objects to be
         * garbage-collected.
         */
        public void clear() {
            loggerConfig = null;
            event = null;
        }
    }

    /**
     * EventHandler performs the work in a separate thread.
     */
    private static class Log4jEventWrapperHandler implements
            SequenceReportingEventHandler<Log4jEventWrapper> {
        private static final int NOTIFY_PROGRESS_THRESHOLD = 50;
        private Sequence sequenceCallback;
        private int counter;

        @Override
        public void setSequenceCallback(final Sequence sequenceCallback) {
            this.sequenceCallback = sequenceCallback;
        }

        @Override
        public void onEvent(final Log4jEventWrapper event, final long sequence,
                final boolean endOfBatch) throws Exception {
            event.event.setEndOfBatch(endOfBatch);
            event.loggerConfig.asyncCallAppenders(event.event);
            event.clear();

            // notify the BatchEventProcessor that the sequence has progressed.
            // Without this callback the sequence would not be progressed
            // until the batch has completely finished.
            if (++counter > NOTIFY_PROGRESS_THRESHOLD) {
                sequenceCallback.set(sequence);
                counter = 0;
            }
        }
    }

    /**
     * Increases the reference count and creates and starts a new Disruptor and
     * associated thread if none currently exists.
     * 
     * @see #release()
     */
    synchronized static void claim() {
        count++;
        initDisruptor();
    }

    /**
     * Decreases the reference count. If the reference count reached zero, the
     * Disruptor and its associated thread are shut down and their references
     * set to {@code null}.
     */
    synchronized static void release() {
        if (--count > 0) {
            LOGGER.trace("AsyncLoggerConfigHelper: not shutting down disruptor: ref count is {}.", count);
            return;
        }
        final Disruptor<Log4jEventWrapper> temp = disruptor;
        if (temp == null) {
            LOGGER.trace("AsyncLoggerConfigHelper: disruptor already shut down: ref count is {}. (Resetting to zero.)",
                    count);
            count = 0; // ref count must not be negative or #claim() will not work correctly
            return; // disruptor was already shut down by another thread
        }
        LOGGER.trace("AsyncLoggerConfigHelper: shutting down disruptor: ref count is {}.", count);

        // Must guarantee that publishing to the RingBuffer has stopped
        // before we call disruptor.shutdown()
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
        executor.shutdown(); // finally, kill the processor thread
        executor = null; // release reference to allow GC
    }

    /**
     * Returns {@code true} if the specified disruptor still has unprocessed events.
     */
    private static boolean hasBacklog(Disruptor<?> disruptor) {
        final RingBuffer<?> ringBuffer = disruptor.getRingBuffer();
        return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
    }

    /**
     * Initialize the threadlocal that allows us to detect Logger.log() calls 
     * initiated from the appender thread, which may cause deadlock when the 
     * RingBuffer is full. (LOG4J2-471)
     */
    private static void initThreadLocalForExecutorThread() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                isAppenderThread.set(Boolean.TRUE);
            }
        });
    }

    /**
     * If possible, delegates the invocation to {@code callAppenders} to another
     * thread and returns {@code true}. If this is not possible (if it detects
     * that delegating to another thread would cause deadlock because the
     * current call to Logger.log() originated from the appender thread and the
     * ringbuffer is full) then this method does nothing and returns {@code false}.
     * It is the responsibility of the caller to process the event when this
     * method returns {@code false}.
     * 
     * @param event the event to delegate to another thread
     * @return {@code true} if delegation was successful, {@code false} if the
     *          calling thread needs to process the event itself
     */
    public boolean callAppendersFromAnotherThread(final LogEvent event) {
        Disruptor<Log4jEventWrapper> temp = disruptor;
        if (temp == null) { // LOG4J2-639
            LOGGER.fatal("Ignoring log event after log4j was shut down");
            return true;
        }

        // LOG4J2-471: prevent deadlock when RingBuffer is full and object
        // being logged calls Logger.log() from its toString() method
        if (isAppenderThread.get() == Boolean.TRUE //
                && temp.getRingBuffer().remainingCapacity() == 0) {

            // bypass RingBuffer and invoke Appender directly
            return false;
        }
        // LOG4J2-639: catch NPE if disruptor field was set to null after our check above
        try {
            // Note: do NOT use the temp variable above!
            // That could result in adding a log event to the disruptor after it was shut down,
            // which could cause the publishEvent method to hang and never return.
            disruptor.getRingBuffer().publishEvent(translator, event, asyncLoggerConfig);
        } catch (NullPointerException npe) {
            LOGGER.fatal("Ignoring log event after log4j was shut down.");
        }
        return true;
    }

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the
     * ringbuffer of this {@code AsyncLoggerConfig}.
     * 
     * @param contextName name of the {@code LoggerContext}
     * @param loggerConfigName name of the logger config
     */
    public RingBufferAdmin createRingBufferAdmin(String contextName, String loggerConfigName) {
        return RingBufferAdmin.forAsyncLoggerConfig(disruptor.getRingBuffer(), contextName, loggerConfigName);
    }

}
