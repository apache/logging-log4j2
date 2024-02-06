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

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.impl.ReusableLogEventFactory;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.message.ReusableMessage;

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
public class AsyncLoggerConfigDisruptor extends AbstractLifeCycle implements AsyncLoggerConfigDelegate {

    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;

    /**
     * RingBuffer events contain all information necessary to perform the work in a separate thread.
     */
    public static class Log4jEventWrapper {
        public Log4jEventWrapper() {}

        public Log4jEventWrapper(final MutableLogEvent mutableLogEvent) {
            event = mutableLogEvent;
        }

        private AsyncLoggerConfig loggerConfig;
        private LogEvent event;

        /**
         * Release references held by ring buffer to allow objects to be garbage-collected.
         */
        public void clear() {
            loggerConfig = null;
            if (event instanceof MutableLogEvent) {
                ((MutableLogEvent) event).clear();
            } else {
                event = null;
            }
        }

        @Override
        public String toString() {
            return String.valueOf(event);
        }
    }

    /**
     * EventHandler performs the work in a separate thread.
     * <p>
     *     <strong>Warning:</strong> this implementation only works with Disruptor 4.x.
     * </p>
     */
    private static class Log4jEventWrapperHandler implements EventHandler<Log4jEventWrapper> {
        private static final int NOTIFY_PROGRESS_THRESHOLD = 50;
        private Sequence sequenceCallback;
        private int counter;

        /*
         * Overrides a method from Disruptor 4.x. Do not remove.
         */
        public void setSequenceCallback(final Sequence sequenceCallback) {
            this.sequenceCallback = sequenceCallback;
        }

        @Override
        public void onEvent(final Log4jEventWrapper event, final long sequence, final boolean endOfBatch)
                throws Exception {
            event.event.setEndOfBatch(endOfBatch);
            event.loggerConfig.logToAsyncLoggerConfigsOnCurrentThread(event.event);
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
     * EventHandler performs the work in a separate thread.
     * <p>
     *     <strong>Warning:</strong> this implementation only works with Disruptor 3.x.
     * </p>
     */
    private static final class Log4jEventWrapperHandler3 extends Log4jEventWrapperHandler
            implements SequenceReportingEventHandler<Log4jEventWrapper> {
        public Log4jEventWrapperHandler3() {}
    }

    /**
     * Factory used to populate the RingBuffer with events. These event objects are then re-used during the life of the
     * RingBuffer.
     */
    private static final EventFactory<Log4jEventWrapper> FACTORY = Log4jEventWrapper::new;

    /**
     * Factory used to populate the RingBuffer with events. These event objects are then re-used during the life of the
     * RingBuffer.
     */
    private static final EventFactory<Log4jEventWrapper> MUTABLE_FACTORY =
            () -> new Log4jEventWrapper(new MutableLogEvent());

    /**
     * Object responsible for passing on data to a specific RingBuffer event.
     */
    private static final EventTranslatorTwoArg<Log4jEventWrapper, LogEvent, AsyncLoggerConfig> TRANSLATOR =
            (ringBufferElement, sequence, logEvent, loggerConfig) -> {
                ringBufferElement.event = logEvent;
                ringBufferElement.loggerConfig = loggerConfig;
            };

    /**
     * Object responsible for passing on data to a RingBuffer event with a MutableLogEvent.
     */
    private static final EventTranslatorTwoArg<Log4jEventWrapper, LogEvent, AsyncLoggerConfig> MUTABLE_TRANSLATOR =
            (ringBufferElement, sequence, logEvent, loggerConfig) -> {
                ((MutableLogEvent) ringBufferElement.event).initFrom(logEvent);
                ringBufferElement.loggerConfig = loggerConfig;
            };

    private Log4jEventWrapperHandler createEventHandler() {
        if (DisruptorUtil.DISRUPTOR_MAJOR_VERSION == 3) {
            try {
                return (Log4jEventWrapperHandler) Class.forName(
                                "org.apache.logging.log4j.core.async.AsyncLoggerConfigDisruptor$Log4jEventWrapperHandler3")
                        .getConstructor()
                        .newInstance();
            } catch (final ReflectiveOperationException | LinkageError e) {
                LOGGER.warn("Failed to create event handler for LMAX Disruptor 3.x, trying version 4.x.", e);
            }
        }
        return new Log4jEventWrapperHandler();
    }

    private int ringBufferSize;
    private AsyncQueueFullPolicy asyncQueueFullPolicy;
    private Boolean mutable = Boolean.FALSE;

    private volatile Disruptor<Log4jEventWrapper> disruptor;
    private long backgroundThreadId; // LOG4J2-471
    private EventFactory<Log4jEventWrapper> factory;
    private EventTranslatorTwoArg<Log4jEventWrapper, LogEvent, AsyncLoggerConfig> translator;
    private volatile boolean alreadyLoggedWarning;
    private final AsyncWaitStrategyFactory asyncWaitStrategyFactory;
    private WaitStrategy waitStrategy;

    private final Object queueFullEnqueueLock = new Object();

    public AsyncLoggerConfigDisruptor(final AsyncWaitStrategyFactory asyncWaitStrategyFactory) {
        this.asyncWaitStrategyFactory = asyncWaitStrategyFactory; // may be null
    }

    // package-protected for testing
    WaitStrategy getWaitStrategy() {
        return waitStrategy;
    }

    // called from AsyncLoggerConfig constructor
    @Override
    public void setLogEventFactory(final LogEventFactory logEventFactory) {
        // if any AsyncLoggerConfig uses a ReusableLogEventFactory
        // then we need to populate our ringbuffer with MutableLogEvents
        this.mutable = mutable || (logEventFactory instanceof ReusableLogEventFactory);
    }

    /**
     * Increases the reference count and creates and starts a new Disruptor and associated thread if none currently
     * exists.
     *
     * @see #stop()
     */
    @Override
    public synchronized void start() {
        if (disruptor != null) {
            LOGGER.trace("AsyncLoggerConfigDisruptor not starting new disruptor for this configuration, "
                    + "using existing object.");
            return;
        }
        LOGGER.trace("AsyncLoggerConfigDisruptor creating new disruptor for this configuration.");
        ringBufferSize = DisruptorUtil.calculateRingBufferSize("AsyncLoggerConfig.RingBufferSize");
        waitStrategy = DisruptorUtil.createWaitStrategy("AsyncLoggerConfig.WaitStrategy", asyncWaitStrategyFactory);

        final ThreadFactory threadFactory = new Log4jThreadFactory("AsyncLoggerConfig", true, Thread.NORM_PRIORITY) {
            @Override
            public Thread newThread(final Runnable r) {
                final Thread result = super.newThread(r);
                backgroundThreadId = result.getId();
                return result;
            }
        };
        asyncQueueFullPolicy = AsyncQueueFullPolicyFactory.create();

        translator = mutable ? MUTABLE_TRANSLATOR : TRANSLATOR;
        factory = mutable ? MUTABLE_FACTORY : FACTORY;
        disruptor = new Disruptor<>(factory, ringBufferSize, threadFactory, ProducerType.MULTI, waitStrategy);

        final ExceptionHandler<Log4jEventWrapper> errorHandler = DisruptorUtil.getAsyncLoggerConfigExceptionHandler();
        disruptor.setDefaultExceptionHandler(errorHandler);

        final Log4jEventWrapperHandler[] handlers = {createEventHandler()};
        disruptor.handleEventsWith(handlers);

        LOGGER.debug(
                "Starting AsyncLoggerConfig disruptor for this configuration with ringbufferSize={}, "
                        + "waitStrategy={}, exceptionHandler={}...",
                disruptor.getRingBuffer().getBufferSize(),
                waitStrategy.getClass().getSimpleName(),
                errorHandler);
        disruptor.start();
        super.start();
    }

    /**
     * Decreases the reference count. If the reference count reached zero, the Disruptor and its associated thread are
     * shut down and their references set to {@code null}.
     */
    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        final Disruptor<Log4jEventWrapper> temp = disruptor;
        if (temp == null) {
            LOGGER.trace("AsyncLoggerConfigDisruptor: disruptor for this configuration already shut down.");
            return true; // disruptor was already shut down by another thread
        }
        setStopping();
        LOGGER.trace("AsyncLoggerConfigDisruptor: shutting down disruptor for this configuration.");

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
        try {
            // busy-spins until all events currently in the disruptor have been processed, or timeout
            temp.shutdown(timeout, timeUnit);
        } catch (final TimeoutException e) {
            LOGGER.warn("AsyncLoggerConfigDisruptor: shutdown timed out after {} {}", timeout, timeUnit);
            temp.halt(); // give up on remaining log events, if any
        }
        LOGGER.trace("AsyncLoggerConfigDisruptor: disruptor has been shut down.");

        if (DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy) > 0) {
            LOGGER.trace(
                    "AsyncLoggerConfigDisruptor: {} discarded {} events.",
                    asyncQueueFullPolicy,
                    DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy));
        }
        setStopped();
        return true;
    }

    /**
     * Returns {@code true} if the specified disruptor still has unprocessed events.
     */
    private static boolean hasBacklog(final Disruptor<?> theDisruptor) {
        final RingBuffer<?> ringBuffer = theDisruptor.getRingBuffer();
        return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
    }

    @Override
    public EventRoute getEventRoute(final Level logLevel) {
        final int remainingCapacity = remainingDisruptorCapacity();
        if (remainingCapacity < 0) {
            return EventRoute.DISCARD;
        }
        return asyncQueueFullPolicy.getRoute(backgroundThreadId, logLevel);
    }

    private int remainingDisruptorCapacity() {
        final Disruptor<Log4jEventWrapper> temp = disruptor;
        if (hasLog4jBeenShutDown(temp)) {
            return -1;
        }
        return (int) temp.getRingBuffer().remainingCapacity();
    }

    /**
     * Returns {@code true} if the specified disruptor is null.
     */
    private boolean hasLog4jBeenShutDown(final Disruptor<Log4jEventWrapper> aDisruptor) {
        if (aDisruptor == null) { // LOG4J2-639
            LOGGER.warn("Ignoring log event after log4j was shut down");
            return true;
        }
        return false;
    }

    @Override
    public void enqueueEvent(final LogEvent event, final AsyncLoggerConfig asyncLoggerConfig) {
        // LOG4J2-639: catch NPE if disruptor field was set to null after our check above
        try {
            final LogEvent logEvent = prepareEvent(event);
            enqueue(logEvent, asyncLoggerConfig);
        } catch (final NullPointerException npe) {
            // Note: NPE prevents us from adding a log event to the disruptor after it was shut down,
            // which could cause the publishEvent method to hang and never return.
            LOGGER.warn(
                    "Ignoring log event after log4j was shut down: {} [{}] {}",
                    event.getLevel(),
                    event.getLoggerName(),
                    event.getMessage().getFormattedMessage()
                            + (event.getThrown() == null ? "" : Throwables.toStringList(event.getThrown())));
        }
    }

    private LogEvent prepareEvent(final LogEvent event) {
        LogEvent logEvent = ensureImmutable(event);
        if (logEvent.getMessage() instanceof ReusableMessage) {
            if (logEvent instanceof Log4jLogEvent) {
                ((Log4jLogEvent) logEvent).makeMessageImmutable();
            } else if (logEvent instanceof MutableLogEvent) {
                // MutableLogEvents need to be translated into the RingBuffer by the MUTABLE_TRANSLATOR.
                // That translator calls MutableLogEvent.initFrom to copy the event, which will makeMessageImmutable the
                // message.
                if (translator != MUTABLE_TRANSLATOR) { // should not happen...
                    // TRANSLATOR expects an immutable LogEvent
                    logEvent = ((MutableLogEvent) logEvent).createMemento();
                }
            } else { // custom log event, with a ReusableMessage
                showWarningAboutCustomLogEventWithReusableMessage(logEvent);
            }
        } else { // message is not a ReusableMessage; makeMessageImmutable it to prevent
            // ConcurrentModificationExceptions
            InternalAsyncUtil.makeMessageImmutable(logEvent.getMessage()); // LOG4J2-1988, LOG4J2-1914
        }
        return logEvent;
    }

    private void showWarningAboutCustomLogEventWithReusableMessage(final LogEvent logEvent) {
        if (!alreadyLoggedWarning) {
            LOGGER.warn(
                    "Custom log event of type {} contains a mutable message of type {}."
                            + " AsyncLoggerConfig does not know how to make an immutable copy of this message."
                            + " This may result in ConcurrentModificationExceptions or incorrect log messages"
                            + " if the application modifies objects in the message while"
                            + " the background thread is writing it to the appenders.",
                    logEvent.getClass().getName(),
                    logEvent.getMessage().getClass().getName());
            alreadyLoggedWarning = true;
        }
    }

    private void enqueue(final LogEvent logEvent, final AsyncLoggerConfig asyncLoggerConfig) {
        if (synchronizeEnqueueWhenQueueFull()) {
            synchronized (queueFullEnqueueLock) {
                disruptor.getRingBuffer().publishEvent(translator, logEvent, asyncLoggerConfig);
            }
        } else {
            disruptor.getRingBuffer().publishEvent(translator, logEvent, asyncLoggerConfig);
        }
    }

    private boolean synchronizeEnqueueWhenQueueFull() {
        return DisruptorUtil.ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL
                // Background thread must never block
                && backgroundThreadId != Thread.currentThread().getId()
                // Threads owned by log4j are most likely to result in
                // deadlocks because they generally consume events.
                // This prevents deadlocks between AsyncLoggerContext
                // disruptors.
                && !(Thread.currentThread() instanceof Log4jThread);
    }

    @Override
    public boolean tryEnqueue(final LogEvent event, final AsyncLoggerConfig asyncLoggerConfig) {
        final LogEvent logEvent = prepareEvent(event);
        return disruptor.getRingBuffer().tryPublishEvent(translator, logEvent, asyncLoggerConfig);
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

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate#createRingBufferAdmin(java.lang.String,
     * java.lang.String)
     */
    @Override
    public RingBufferAdmin createRingBufferAdmin(final String contextName, final String loggerConfigName) {
        return RingBufferAdmin.forAsyncLoggerConfig(disruptor.getRingBuffer(), contextName, loggerConfigName);
    }
}
