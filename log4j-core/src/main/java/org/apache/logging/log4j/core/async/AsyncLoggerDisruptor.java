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

import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.status.StatusLogger;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * Helper class for async loggers: AsyncLoggerDisruptor handles the mechanics of working with the LMAX Disruptor, and
 * works with its associated AsyncLoggerContext to synchronize the life cycle of the Disruptor and its thread with the
 * life cycle of the context. The AsyncLoggerDisruptor of the context is shared by all AsyncLogger objects created by
 * that AsyncLoggerContext.
 */
class AsyncLoggerDisruptor {
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private volatile Disruptor<RingBufferLogEvent> disruptor;
    private ExecutorService executor;
    private String contextName;

    private boolean useThreadLocalTranslator;
    private long backgroundThreadId;

    AsyncLoggerDisruptor(String contextName) {
        this.contextName = contextName;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String name) {
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
    synchronized void start() {
        if (disruptor != null) {
            LOGGER.trace(
                    "[{}] AsyncLoggerDisruptor not starting new disruptor for this context, using existing object.",
                    contextName);
            return;
        }
        LOGGER.trace("[{}] AsyncLoggerDisruptor creating new disruptor for this context.", contextName);
        final int ringBufferSize = DisruptorUtil.calculateRingBufferSize("AsyncLogger.RingBufferSize");
        final WaitStrategy waitStrategy = DisruptorUtil.createWaitStrategy("AsyncLogger.WaitStrategy");
        executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory("AsyncLogger[" + contextName + "]"));
        backgroundThreadId = DisruptorUtil.getExecutorThreadId(executor);

        disruptor = new Disruptor<>(RingBufferLogEvent.FACTORY, ringBufferSize, executor, ProducerType.MULTI,
                waitStrategy);

        final ExceptionHandler<RingBufferLogEvent> errorHandler = DisruptorUtil.getExceptionHandler(
                "AsyncLogger.ExceptionHandler", RingBufferLogEvent.class);
        disruptor.handleExceptionsWith(errorHandler);

        final RingBufferLogEventHandler[] handlers = {new RingBufferLogEventHandler()};
        disruptor.handleEventsWith(handlers);

        LOGGER.debug("[{}] Starting AsyncLogger disruptor for this context with ringbufferSize={}, waitStrategy={}, "
                + "exceptionHandler={}...", contextName, disruptor.getRingBuffer().getBufferSize(), waitStrategy
                .getClass().getSimpleName(), errorHandler);
        disruptor.start();

        LOGGER.trace("[{}] AsyncLoggers use a {} translator", contextName, useThreadLocalTranslator ? "threadlocal"
                : "vararg");
    }

    /**
     * Decreases the reference count. If the reference count reached zero, the Disruptor and its associated thread are
     * shut down and their references set to {@code null}.
     */
    synchronized void stop() {
        final Disruptor<RingBufferLogEvent> temp = getDisruptor();
        if (temp == null) {
            LOGGER.trace("[{}] AsyncLoggerDisruptor: disruptor for this context already shut down.", contextName);
            return; // disruptor was already shut down by another thread
        }
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
        temp.shutdown(); // busy-spins until all events currently in the disruptor have been processed

        LOGGER.trace("[{}] AsyncLoggerDisruptor: shutting down disruptor executor.", contextName);
        executor.shutdown(); // finally, kill the processor thread
        executor = null;
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

    /**
     * Returns {@code true} if the current log event should be logged in the current thread, {@code false} if it should
     * be logged in a background thread. (LOG4J2-471)
     * 
     * @return whether the current log event should be logged in the current thread
     */
    boolean shouldLogInCurrentThread() {
        return currentThreadIsAppenderThread() && isRingBufferFull();
    }

    /**
     * Returns {@code true} if the current thread is the Disruptor background thread, {@code false} otherwise.
     * 
     * @return whether this thread is the Disruptor background thread.
     */
    private boolean currentThreadIsAppenderThread() {
        return Thread.currentThread().getId() == backgroundThreadId;
    }

    /**
     * Returns {@code true} if the Disruptor is {@code null} because it has been stopped, or if its Ringbuffer is full.
     * 
     * @return {@code true} if the disruptor is currently not usable
     */
    private boolean isRingBufferFull() {
        final Disruptor<RingBufferLogEvent> theDisruptor = this.disruptor;
        return theDisruptor == null || theDisruptor.getRingBuffer().remainingCapacity() == 0;
    }

    void enqueueLogMessageInfo(final RingBufferLogEventTranslator translator) {
        // LOG4J2-639: catch NPE if disruptor field was set to null in stop()
        try {
            // Note: we deliberately access the volatile disruptor field afresh here.
            // Avoiding this and using an older reference could result in adding a log event to the disruptor after it
            // was shut down, which could cause the publishEvent method to hang and never return.
            disruptor.publishEvent(translator);
        } catch (final NullPointerException npe) {
            LOGGER.fatal("[{}] Ignoring log event after log4j was shut down.", contextName);
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
     * 
     * @param allow whether AsyncLoggers are allowed to use ThreadLocal objects
     * @since 2.5
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-1172">LOG4J2-1172</a>
     */
    public void setUseThreadLocals(final boolean allow) {
        useThreadLocalTranslator = allow;
    }
}
