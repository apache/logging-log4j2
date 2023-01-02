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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.spi.ClassFactory;
import org.apache.logging.log4j.spi.InstanceFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.PropertyResolver;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.WaitStrategy;

import static org.apache.logging.log4j.util.Constants.isThreadLocalsEnabled;

/**
 * Utility methods for getting Disruptor related configuration.
 */
@InternalApi
public class DisruptorConfiguration {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;
    private static final int RINGBUFFER_NO_GC_DEFAULT_SIZE = 4 * 1024;

    private final PropertyResolver propertyResolver;
    private final ClassFactory classFactory;
    private final InstanceFactory instanceFactory;

    @Inject
    DisruptorConfiguration(final PropertyResolver resolver, final ClassFactory classFactory, final InstanceFactory instanceFactory) {
        propertyResolver = resolver;
        this.classFactory = classFactory;
        this.instanceFactory = instanceFactory;
    }

    WaitStrategy createWaitStrategy(final String propertyName, final AsyncWaitStrategyFactory asyncWaitStrategyFactory) {
        if (asyncWaitStrategyFactory == null) {
            LOGGER.debug("No AsyncWaitStrategyFactory was configured in the configuration, using default factory...");
            return new DefaultAsyncWaitStrategyFactory(propertyResolver, propertyName).createWaitStrategy();
        }
        LOGGER.debug("Using configured AsyncWaitStrategyFactory {}", asyncWaitStrategyFactory.getClass().getName());
        return asyncWaitStrategyFactory.createWaitStrategy();
    }

    int calculateRingBufferSize(final String propertyName) {
        int ringBufferSize = isThreadLocalsEnabled(propertyResolver) ? RINGBUFFER_NO_GC_DEFAULT_SIZE : RINGBUFFER_DEFAULT_SIZE;
        int size = propertyResolver.getInt(propertyName).orElse(ringBufferSize);
        if (size < RINGBUFFER_MIN_SIZE) {
            LOGGER.warn("Invalid RingBufferSize {}, using minimum size {}.", size, RINGBUFFER_MIN_SIZE);
            size = RINGBUFFER_MIN_SIZE;
        }
        ringBufferSize = size;
        return Integers.ceilingNextPowerOfTwo(ringBufferSize);
    }

    /**
     * LOG4J2-2606: Users encountered excessive CPU utilization with Disruptor v3.4.2 when the application
     * was logging more than the underlying appender could keep up with and the ringbuffer became full,
     * especially when the number of application threads vastly outnumbered the number of cores.
     * CPU utilization is significantly reduced by restricting access to the enqueue operation.
     */
    boolean synchronizeEnqueueWhenQueueFull(final String propertyName, final long backgroundThreadId) {
        return propertyResolver.getBoolean(propertyName, true)
                // Background thread must never block
                && backgroundThreadId != Thread.currentThread().getId()
                // Threads owned by log4j are most likely to result in
                // deadlocks because they generally consume events.
                // This prevents deadlocks between AsyncLoggerContext
                // disruptors.
                && !(Thread.currentThread() instanceof Log4jThread);
    }

    // TODO(ms): default bindings for LMAX classes should go in a conditionally-loaded bundle class similar to ConditionalOnClass
    ExceptionHandler<RingBufferLogEvent> getAsyncLoggerExceptionHandler() {
        return propertyResolver.getString(Log4jProperties.ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME)
                .flatMap(className -> classFactory.tryGetClass(className, ExceptionHandler.class))
                .<ExceptionHandler<RingBufferLogEvent>>flatMap(type -> Cast.cast(instanceFactory.tryGetInstance(type)))
                .orElseGet(AsyncLoggerDefaultExceptionHandler::new);
    }

    ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> getAsyncLoggerConfigExceptionHandler() {
        return propertyResolver.getString(Log4jProperties.ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME)
                .flatMap(className -> classFactory.tryGetClass(className, ExceptionHandler.class))
                .<ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>>flatMap(type -> Cast.cast(instanceFactory.tryGetInstance(type)))
                .orElseGet(AsyncLoggerConfigDefaultExceptionHandler::new);
    }
}
