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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.ExecutorServices;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * Helper class for async loggers: AsyncLoggerDisruptor handles the mechanics of working with the LMAX Disruptor, and
 * works with its associated AsyncLoggerContext to synchronize the life cycle of the Disruptor and its thread with the
 * life cycle of the context. The AsyncLoggerDisruptor of the context is shared by all AsyncLogger objects created by
 * that AsyncLoggerContext.
 */
class AsyncLoggerDisruptor extends AbstractLifeCycle {
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;

    private volatile Disruptor<RingBufferLogEvent> disruptor;
    private ExecutorService executor;
    private String contextName;

    private boolean useThreadLocalTranslator = true;
    private long backgroundThreadId;
    private AsyncQueueFullPolicy asyncQueueFullPolicy;
    private int ringBufferSize;

    AsyncLoggerDisruptor(final String contextName) {
        this.contextName = contextName;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(final String name) {
        contextName = name;
    }

    Disruptor<RingBufferLogEvent> getDisruptor() {
        return disruptor;
    }

    /**
     * Creates and starts a new Disruptor and associated thread if none currently exists.
     *
     * @see #stop()
     */
    @Override
    public synchronized void start() {
        if (disruptor != null) {
            LOGGER.trace(
                    "[{}] AsyncLoggerDisruptor not starting new disruptor for this context, using existing object.",
                    contextName);
            return;
        }
        LOGGER.trace("[{}] AsyncLoggerDisruptor creating new disruptor for this context.", contextName);
        ringBufferSize = DisruptorUtil.calculateRingBufferSize("AsyncLogger.RingBufferSize");
        final WaitStrategy waitStrategy = DisruptorUtil.createWaitStrategy("AsyncLogger.WaitStrategy");
        executor = Executors.newSingleThreadExecutor(Log4jThreadFactory.createDaemonThreadFactory("AsyncLogger[" + contextName + "]"));
        backgroundThreadId = DisruptorUtil.getExecutorThreadId(executor);
        asyncQueueFullPolicy = AsyncQueueFullPolicyFactory.create();

        disruptor = new Disruptor<>(RingBufferLogEvent.FACTORY, ringBufferSize, executor, ProducerType.MULTI,
                waitStrategy);

        final ExceptionHandler<RingBufferLogEvent> errorHandler = DisruptorUtil.getAsyncLoggerExceptionHandler();
        disruptor.handleExceptionsWith(errorHandler);

        final RingBufferLogEventHandler[] handlers = {new RingBufferLogEventHandler()};
        disruptor.handleEventsWith(handlers);

        LOGGER.debug("[{}] Starting AsyncLogger disruptor for this context with ringbufferSize={}, waitStrategy={}, "
                + "exceptionHandler={}...", contextName, disruptor.getRingBuffer().getBufferSize(), waitStrategy
                .getClass().getSimpleName(), errorHandler);
        disruptor.start();

        LOGGER.trace("[{}] AsyncLoggers use a {} translator", contextName, useThreadLocalTranslator ? "threadlocal"
                : "vararg");
        super.start();
    }

    /**
     * Decreases the reference count. If the reference count reached zero, the Disruptor and its associated thread are
     * shut down and their references set to {@code null}.
     */
    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        final Disruptor<RingBufferLogEvent> temp = getDisruptor();
        if (temp == null) {
            LOGGER.trace("[{}] AsyncLoggerDisruptor: disruptor for this context already shut down.", contextName);
            return true; // disruptor was already shut down by another thread
        }
        setStopping();
        LOGGER.debug("[{}] AsyncLoggerDisruptor: shutting down disruptor for this context.", contextName);

        // We must guarantee that publishing to the RingBuffer has stopped before we call disruptor.shutdown().
        disruptor = null; // client code fails with NPE if log after stop. This is by design.

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
            // busy-spins until all events currently in the disruptor have been processed
            temp.shutdown(timeout, timeUnit);
        } catch (final TimeoutException e) {
            temp.shutdown();
        } 

        LOGGER.trace("[{}] AsyncLoggerDisruptor: shutting down disruptor executor.", contextName);
        // finally, kill the processor thread
        ExecutorServices.shutdown(executor, timeout, timeUnit, toString());
        executor = null;

        if (DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy) > 0) {
            LOGGER.trace("AsyncLoggerDisruptor: {} discarded {} events.", asyncQueueFullPolicy,
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

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the ringbuffer of the {@code AsyncLogger}.
     *
     * @param jmxContextName name of the {@code AsyncLoggerContext}
     * @return a new {@code RingBufferAdmin} that instruments the ringbuffer
     */
    public RingBufferAdmin createRingBufferAdmin(final String jmxContextName) {
        final RingBuffer<RingBufferLogEvent> ring = disruptor == null ? null : disruptor.getRingBuffer();
        return RingBufferAdmin.forAsyncLogger(ring, jmxContextName);
    }

    EventRoute getEventRoute(final Level logLevel) {
        final int remainingCapacity = remainingDisruptorCapacity();
        if (remainingCapacity < 0) {
            return EventRoute.DISCARD;
        }
        return asyncQueueFullPolicy.getRoute(backgroundThreadId, logLevel);
    }

    private int remainingDisruptorCapacity() {
        final Disruptor<RingBufferLogEvent> temp = disruptor;
        if (hasLog4jBeenShutDown(temp)) {
            return -1;
        }
        return (int) temp.getRingBuffer().remainingCapacity();
    }
        /**
         * Returns {@code true} if the specified disruptor is null.
         */
    private boolean hasLog4jBeenShutDown(final Disruptor<RingBufferLogEvent> aDisruptor) {
        if (aDisruptor == null) { // LOG4J2-639
            LOGGER.warn("Ignoring log event after log4j was shut down");
            return true;
        }
        return false;
    }

    public boolean tryPublish(final RingBufferLogEventTranslator translator) {
        // LOG4J2-639: catch NPE if disruptor field was set to null in stop()
        try {
            return disruptor.getRingBuffer().tryPublishEvent(translator);
        } catch (final NullPointerException npe) {
            LOGGER.warn("[{}] Ignoring log event after log4j was shut down.", contextName);
            return false;
        }
    }

    void enqueueLogMessageInfo(final RingBufferLogEventTranslator translator) {
        // LOG4J2-639: catch NPE if disruptor field was set to null in stop()
        try {
            // Note: we deliberately access the volatile disruptor field afresh here.
            // Avoiding this and using an older reference could result in adding a log event to the disruptor after it
            // was shut down, which could cause the publishEvent method to hang and never return.
            disruptor.publishEvent(translator);
        } catch (final NullPointerException npe) {
            LOGGER.warn("[{}] Ignoring log event after log4j was shut down.", contextName);
        }
    }

    /**
     * Returns whether it is allowed to store non-JDK classes in ThreadLocal objects for efficiency.
     *
     * @return whether AsyncLoggers are allowed to use ThreadLocal objects
     * @since 2.5
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-1172">LOG4J2-1172</a>
     */
    public boolean isUseThreadLocals() {
        return useThreadLocalTranslator;
    }

    /**
     * Signals this AsyncLoggerDisruptor whether it is allowed to store non-JDK classes in ThreadLocal objects for
     * efficiency.
     * <p>
     * This property may be modified after the {@link #start()} method has been called.
     * </p>
     *
     * @param allow whether AsyncLoggers are allowed to use ThreadLocal objects
     * @since 2.5
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-1172">LOG4J2-1172</a>
     */
    public void setUseThreadLocals(final boolean allow) {
        useThreadLocalTranslator = allow;
        LOGGER.trace("[{}] AsyncLoggers have been modified to use a {} translator", contextName,
                useThreadLocalTranslator ? "threadlocal" : "vararg");
    }
}
