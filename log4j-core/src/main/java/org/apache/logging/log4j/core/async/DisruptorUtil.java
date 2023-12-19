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

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.WaitStrategy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Utility methods for getting Disruptor related configuration.
 */
final class DisruptorUtil {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;
    private static final int RINGBUFFER_NO_GC_DEFAULT_SIZE = 4 * 1024;
    public static final String LOGGER_EXCEPTION_HANDLER_PROPERTY = "AsyncLogger.ExceptionHandler";
    public static final String LOGGER_CONFIG_EXCEPTION_HANDLER_PROPERTY = "AsyncLoggerConfig.ExceptionHandler";

    /**
     * LOG4J2-2606: Users encountered excessive CPU utilization with Disruptor v3.4.2 when the application
     * was logging more than the underlying appender could keep up with and the ringbuffer became full,
     * especially when the number of application threads vastly outnumbered the number of cores.
     * CPU utilization is significantly reduced by restricting access to the enqueue operation.
     */
    static final boolean ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL =
            PropertiesUtil.getProperties().getBooleanProperty("AsyncLogger.SynchronizeEnqueueWhenQueueFull", true);

    static final boolean ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = PropertiesUtil.getProperties()
            .getBooleanProperty("AsyncLoggerConfig.SynchronizeEnqueueWhenQueueFull", true);

    static final int DISRUPTOR_MAJOR_VERSION =
            LoaderUtil.isClassAvailable("com.lmax.disruptor.SequenceReportingEventHandler") ? 3 : 4;

    private DisruptorUtil() {}

    static WaitStrategy createWaitStrategy(
            final String propertyName, final AsyncWaitStrategyFactory asyncWaitStrategyFactory) {

        if (asyncWaitStrategyFactory == null) {
            LOGGER.debug("No AsyncWaitStrategyFactory was configured in the configuration, using default factory...");
            return new DefaultAsyncWaitStrategyFactory(propertyName).createWaitStrategy();
        }

        LOGGER.debug(
                "Using configured AsyncWaitStrategyFactory {}",
                asyncWaitStrategyFactory.getClass().getName());
        return asyncWaitStrategyFactory.createWaitStrategy();
    }

    static int calculateRingBufferSize(final String propertyName) {
        int ringBufferSize = Constants.ENABLE_THREADLOCALS ? RINGBUFFER_NO_GC_DEFAULT_SIZE : RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize =
                PropertiesUtil.getProperties().getStringProperty(propertyName, String.valueOf(ringBufferSize));
        try {
            int size = Integers.parseInt(userPreferredRBSize);
            if (size < RINGBUFFER_MIN_SIZE) {
                size = RINGBUFFER_MIN_SIZE;
                LOGGER.warn(
                        "Invalid RingBufferSize {}, using minimum size {}.", userPreferredRBSize, RINGBUFFER_MIN_SIZE);
            }
            ringBufferSize = size;
        } catch (final Exception ex) {
            LOGGER.warn("Invalid RingBufferSize {}, using default size {}.", userPreferredRBSize, ringBufferSize);
        }
        return Integers.ceilingNextPowerOfTwo(ringBufferSize);
    }

    static ExceptionHandler<RingBufferLogEvent> getAsyncLoggerExceptionHandler() {
        try {
            return LoaderUtil.newCheckedInstanceOfProperty(
                    LOGGER_EXCEPTION_HANDLER_PROPERTY, ExceptionHandler.class, AsyncLoggerDefaultExceptionHandler::new);
        } catch (final ReflectiveOperationException e) {
            LOGGER.debug("Invalid AsyncLogger.ExceptionHandler value: {}", e.getMessage(), e);
            return new AsyncLoggerDefaultExceptionHandler();
        }
    }

    static ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> getAsyncLoggerConfigExceptionHandler() {
        try {
            return LoaderUtil.newCheckedInstanceOfProperty(
                    LOGGER_CONFIG_EXCEPTION_HANDLER_PROPERTY,
                    ExceptionHandler.class,
                    AsyncLoggerConfigDefaultExceptionHandler::new);
        } catch (final ReflectiveOperationException e) {
            LOGGER.debug("Invalid AsyncLogger.ExceptionHandler value: {}", e.getMessage(), e);
            return new AsyncLoggerConfigDefaultExceptionHandler();
        }
    }

    /**
     * Returns the thread ID of the background appender thread. This allows us to detect Logger.log() calls initiated
     * from the appender thread, which may cause deadlock when the RingBuffer is full. (LOG4J2-471)
     *
     * @param executor runs the appender thread
     * @return the thread ID of the background appender thread
     */
    public static long getExecutorThreadId(final ExecutorService executor) {
        final Future<Long> result = executor.submit(() -> Thread.currentThread().getId());
        try {
            return result.get();
        } catch (final Exception ex) {
            final String msg =
                    "Could not obtain executor thread Id. " + "Giving up to avoid the risk of application deadlock.";
            throw new IllegalStateException(msg, ex);
        }
    }
}
