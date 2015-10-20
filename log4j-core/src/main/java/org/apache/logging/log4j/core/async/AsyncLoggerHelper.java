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
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 * Helper class for AsyncLogger:
 * <ul>
 * <li>Separates the AsyncLogger business logic (logging) from the mechanics of working with the LMAX Disruptor.</li>
 * <li>Disruptor initialization no longer happens in a static initializer block in AsyncLogger to prevent problems where
 * the Disruptor is unintentionally initialized (and a thread started) because a static method in AsyncLogger is called
 * (LOG4J2-1159).</li>
 * <li>Enable a reference counting scheme tracking the total number of starts and stops, so the Disruptor and associated
 * Executor can be shut down when usage becomes zero. This fixes a long-standing issue that made AsyncLoggers unsuitable
 * for use in web applications (LOG4J2-493).</li>
 * </ul>
 */
public class AsyncLoggerHelper {
    private static final int SLEEP_MILLIS_BETWEEN_DRAIN_ATTEMPTS = 50;
    private static final int MAX_DRAIN_ATTEMPTS_BEFORE_SHUTDOWN = 200;
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;
    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private static ExecutorService executor;
    private static volatile Disruptor<RingBufferLogEvent> disruptor;
    private static volatile int count = 0;

    static Disruptor<RingBufferLogEvent> getDisruptor() {
        return disruptor;
    }

    /**
     * Increases the reference count and creates and starts a new Disruptor and associated thread if none currently
     * exists.
     * 
     * @see #release()
     */
    static synchronized void start() {
        count++;
        initDisruptor();
    }

    private static synchronized void initDisruptor() {
        if (disruptor != null) {
            LOGGER.trace("AsyncLoggerHelper not starting new disruptor, using existing object. Ref count is {}.", count);
            return;
        }
        LOGGER.trace("AsyncLoggerHelper creating new disruptor. Ref count is {}.", count);
        final int ringBufferSize = calculateRingBufferSize();
        final WaitStrategy waitStrategy = createWaitStrategy();
        executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory("AsyncLogger-"));
        Info.initExecutorThreadInstance(executor);

        disruptor = new Disruptor<>(RingBufferLogEvent.FACTORY, ringBufferSize, executor, ProducerType.MULTI,
                waitStrategy);
        disruptor.handleExceptionsWith(getExceptionHandler());

        final RingBufferLogEventHandler[] handlers = {new RingBufferLogEventHandler()};
        disruptor.handleEventsWith(handlers);

        LOGGER.debug("Starting AsyncLogger disruptor with ringbuffer size {}...", disruptor.getRingBuffer()
                .getBufferSize());
        disruptor.start();
    }

    private static int calculateRingBufferSize() {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize = PropertiesUtil.getProperties().getStringProperty(
                "AsyncLogger.RingBufferSize", String.valueOf(ringBufferSize));
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

    private static WaitStrategy createWaitStrategy() {
        final String strategy = PropertiesUtil.getProperties().getStringProperty("AsyncLogger.WaitStrategy");
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

    private static ExceptionHandler<RingBufferLogEvent> getExceptionHandler() {
        final String cls = PropertiesUtil.getProperties().getStringProperty("AsyncLogger.ExceptionHandler");
        if (cls == null) {
            LOGGER.debug("No AsyncLogger.ExceptionHandler specified");
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            final ExceptionHandler<RingBufferLogEvent> result = Loader
                    .newCheckedInstanceOf(cls, ExceptionHandler.class);
            LOGGER.debug("AsyncLogger.ExceptionHandler={}", result);
            return result;
        } catch (final Exception ignored) {
            LOGGER.debug("AsyncLogger.ExceptionHandler not set: error creating " + cls + ": ", ignored);
            return null;
        }
    }

    static void enqueueLogMessageInfo(final RingBufferLogEventTranslator translator) {
        // LOG4J2-639: catch NPE if disruptor field was set to null in release()
        try {
            // Note: we deliberately access the volatile disruptor field afresh here.
            // Avoiding this and using an older refrence could result in adding a log event to the disruptor after it
            // was shut down, which could cause the publishEvent method to hang and never return.
            disruptor.publishEvent(translator);
        } catch (final NullPointerException npe) {
            LOGGER.fatal("Ignoring log event after log4j was shut down.");
        }
    }

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the ringbuffer of the {@code AsyncLogger}.
     *
     * @param contextName name of the global {@code AsyncLoggerContext}
     * @return a new {@code RingBufferAdmin} that instruments the ringbuffer
     */
    public static RingBufferAdmin createRingBufferAdmin(final String contextName) {
        return RingBufferAdmin.forAsyncLogger(disruptor.getRingBuffer(), contextName);
    }

    /**
     * Decreases the reference count. If the reference count reached zero, the Disruptor and its associated thread are
     * shut down and their references set to {@code null}.
     */
    static synchronized void stop() {
        if (--count > 0) {
            LOGGER.trace("AsyncLoggerHelper: not shutting down disruptor: ref count is {}.", count);
            return;
        }

        final Disruptor<RingBufferLogEvent> temp = getDisruptor();
        if (temp == null) {
            LOGGER.trace("AsyncLoggerHelper: disruptor already shut down: ref count is {}. (Resetting to zero.)", count);
            count = 0; // ref count must not be negative or #claim() will not work correctly
            return; // disruptor was already shut down by another thread
        }
        LOGGER.debug("AsyncLoggerHelper: shutting down disruptor: ref count is {}.", count);

        // Must guarantee that publishing to the RingBuffer has stopped
        // before we call disruptor.shutdown()
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

        LOGGER.trace("AsyncLoggerHelper: shutting down disruptor executor.");
        executor.shutdown(); // finally, kill the processor thread
        executor = null;
        // Info.THREADLOCAL.remove(); // LOG4J2-323
    }

    /**
     * Returns {@code true} if the specified disruptor still has unprocessed events.
     */
    private static boolean hasBacklog(final Disruptor<?> theDisruptor) {
        final RingBuffer<?> ringBuffer = theDisruptor.getRingBuffer();
        return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
    }

}
