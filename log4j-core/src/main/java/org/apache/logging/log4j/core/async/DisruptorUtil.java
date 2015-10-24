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
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;

/**
 * Utility methods for getting Disruptor related configuration.
 */
class DisruptorUtil {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int RINGBUFFER_MIN_SIZE = 128;
    private static final int RINGBUFFER_DEFAULT_SIZE = 256 * 1024;

    private DisruptorUtil() {
    }

    static WaitStrategy createWaitStrategy(final String propertyName) {
        final String strategy = PropertiesUtil.getProperties().getStringProperty(propertyName);
        if (strategy != null) {
            LOGGER.trace("property {}={}", propertyName, strategy);
            if ("Sleep".equalsIgnoreCase(strategy)) {
                return new SleepingWaitStrategy();
            } else if ("Yield".equalsIgnoreCase(strategy)) {
                return new YieldingWaitStrategy();
            } else if ("Block".equalsIgnoreCase(strategy)) {
                return new BlockingWaitStrategy();
            }
        }
        return new BlockingWaitStrategy();
    }

    static int calculateRingBufferSize(final String propertyName) {
        int ringBufferSize = RINGBUFFER_DEFAULT_SIZE;
        final String userPreferredRBSize = PropertiesUtil.getProperties().getStringProperty(
                propertyName, String.valueOf(ringBufferSize));
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

    static <T> ExceptionHandler<T> getExceptionHandler(final String propertyName, Class<T> type) {
        final String cls = PropertiesUtil.getProperties().getStringProperty(propertyName);
        if (cls == null) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends ExceptionHandler<T>> klass =
                    (Class<? extends ExceptionHandler<T>>) Class.forName(cls);
            return klass.newInstance();
        } catch (final Exception ignored) {
            LOGGER.debug("Invalid {} value: error creating {}: ", propertyName, cls, ignored);
            return null;
        }
    }
}
