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
package org.apache.logging.log4j.async.logger.internal;

import static org.apache.logging.log4j.core.impl.Log4jPropertyKey.ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME;
import static org.apache.logging.log4j.core.impl.Log4jPropertyKey.ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.WaitStrategy;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.async.logger.AsyncLoggerConfigDefaultExceptionHandler;
import org.apache.logging.log4j.async.logger.AsyncLoggerConfigDisruptor;
import org.apache.logging.log4j.async.logger.AsyncLoggerDefaultExceptionHandler;
import org.apache.logging.log4j.async.logger.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.async.logger.RingBufferLogEvent;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;

/**
 * Utility methods for getting Disruptor related configuration.
 */
public final class DisruptorUtil {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 4 * 1024;

    /**
     * LOG4J2-2606: Users encountered excessive CPU utilization with Disruptor v3.4.2 when the application
     * was logging more than the underlying appender could keep up with and the ringbuffer became full,
     * especially when the number of application threads vastly outnumbered the number of cores.
     * CPU utilization is significantly reduced by restricting access to the enqueue operation.
     */
    public static final boolean ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = PropertiesUtil.getProperties()
            .getBooleanProperty(Log4jPropertyKey.ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL, true);

    public static final boolean ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = PropertiesUtil.getProperties()
            .getBooleanProperty(Log4jPropertyKey.ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL, true);

    private DisruptorUtil() {}

    public static WaitStrategy createWaitStrategy(
            final PropertyKey key, final AsyncWaitStrategyFactory asyncWaitStrategyFactory) {

        if (asyncWaitStrategyFactory == null) {
            LOGGER.debug("No AsyncWaitStrategyFactory was configured in the configuration, using default factory...");
            return new DefaultAsyncWaitStrategyFactory(key).createWaitStrategy();
        }
        LOGGER.debug(
                "Using configured AsyncWaitStrategyFactory {}",
                asyncWaitStrategyFactory.getClass().getName());
        return asyncWaitStrategyFactory.createWaitStrategy();
    }

    public static int calculateRingBufferSize(final PropertyKey key) {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize =
                PropertiesUtil.getProperties().getStringProperty(key, String.valueOf(ringBufferSize));
        try {
            int size = Integer.parseInt(userPreferredRBSize);
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

    public static ExceptionHandler<RingBufferLogEvent> getAsyncLoggerExceptionHandler() {
        try {
            return LoaderUtil.newCheckedInstanceOfProperty(
                    ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME,
                    ExceptionHandler.class,
                    AsyncLoggerDefaultExceptionHandler::new);
        } catch (final ReflectiveOperationException e) {
            LOGGER.debug("Invalid AsyncLogger.ExceptionHandler value: {}", e.getMessage(), e);
            return new AsyncLoggerDefaultExceptionHandler();
        }
    }

    public static ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>
            getAsyncLoggerConfigExceptionHandler() {
        try {
            return LoaderUtil.newCheckedInstanceOfProperty(
                    ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME,
                    ExceptionHandler.class,
                    AsyncLoggerConfigDefaultExceptionHandler::new);
        } catch (final ReflectiveOperationException e) {
            LOGGER.debug("Invalid AsyncLogger.ExceptionHandler value: {}", e.getMessage(), e);
            return new AsyncLoggerConfigDefaultExceptionHandler();
        }
    }
}
