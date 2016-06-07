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

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.*;
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

    private DisruptorUtil() {
    }

    static long getTimeout(final String propertyName, final long defaultTimeout) {
        return PropertiesUtil.getProperties().getLongProperty(propertyName, defaultTimeout);
    }

    static WaitStrategy createWaitStrategy(final String propertyName) {
        final String key = propertyName.startsWith("AsyncLogger.")
                ? "AsyncLogger.Timeout"
                : "AsyncLoggerConfig.Timeout";
        final long timeoutMillis = DisruptorUtil.getTimeout(key, 10L);
        return createWaitStrategy(propertyName, timeoutMillis);
    }

    static WaitStrategy createWaitStrategy(final String propertyName, final long timeoutMillis) {
        final String strategy = PropertiesUtil.getProperties().getStringProperty(propertyName, "TIMEOUT");
        LOGGER.trace("property {}={}", propertyName, strategy);
        final String strategyUp = strategy.toUpperCase(Locale.ROOT); // TODO Refactor into Strings.toRootUpperCase(String)
        switch (strategyUp) { // TODO Define a DisruptorWaitStrategy enum?
        case "SLEEP":
            return new SleepingWaitStrategy();
        case "YIELD":
            return new YieldingWaitStrategy();
        case "BLOCK":
            return new BlockingWaitStrategy();
        case "BUSYSPIN":
            return new BusySpinWaitStrategy();
        case "TIMEOUT":
            return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
        default:
            return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    static int calculateRingBufferSize(final String propertyName) {
        int ringBufferSize = Constants.ENABLE_THREADLOCALS ? RINGBUFFER_NO_GC_DEFAULT_SIZE : RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize = PropertiesUtil.getProperties().getStringProperty(propertyName,
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
        return Integers.ceilingNextPowerOfTwo(ringBufferSize);
    }

    static ExceptionHandler<RingBufferLogEvent> getAsyncLoggerExceptionHandler() {
        final String cls = PropertiesUtil.getProperties().getStringProperty("AsyncLogger.ExceptionHandler");
        if (cls == null) {
            return new AsyncLoggerDefaultExceptionHandler();
        }
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends ExceptionHandler<RingBufferLogEvent>> klass =
                (Class<? extends ExceptionHandler<RingBufferLogEvent>>) LoaderUtil.loadClass(cls);
            return klass.newInstance();
        } catch (final Exception ignored) {
            LOGGER.debug("Invalid AsyncLogger.ExceptionHandler value: error creating {}: ", cls, ignored);
            return new AsyncLoggerDefaultExceptionHandler();
        }
    }

    static ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> getAsyncLoggerConfigExceptionHandler() {
        final String cls = PropertiesUtil.getProperties().getStringProperty("AsyncLoggerConfig.ExceptionHandler");
        if (cls == null) {
            return new AsyncLoggerConfigDefaultExceptionHandler();
        }
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>> klass =
                    (Class<? extends ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>>) LoaderUtil.loadClass(cls);
            return klass.newInstance();
        } catch (final Exception ignored) {
            LOGGER.debug("Invalid AsyncLoggerConfig.ExceptionHandler value: error creating {}: ", cls, ignored);
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
        final Future<Long> result = executor.submit(new Callable<Long>() {
            @Override
            public Long call() {
                return Thread.currentThread().getId();
            }
        });
        try {
            return result.get();
        } catch (final Exception ex) {
            final String msg = "Could not obtain executor thread Id. "
                    + "Giving up to avoid the risk of application deadlock.";
            throw new IllegalStateException(msg, ex);
        }
    }
}
