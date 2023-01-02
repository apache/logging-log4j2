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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertyResolver;
import org.apache.logging.log4j.util.Strings;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;

class DefaultAsyncWaitStrategyFactory implements AsyncWaitStrategyFactory {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final PropertyResolver propertyResolver;
    private final String propertyName;

    public DefaultAsyncWaitStrategyFactory(final PropertyResolver propertyResolver, final String propertyName) {
        this.propertyResolver = propertyResolver;
        this.propertyName = propertyName;
    }

    @Override
    public WaitStrategy createWaitStrategy() {
        final String strategyUp = propertyResolver
                .getString(propertyName)
                .map(Strings::toRootUpperCase)
                .orElse("TIMEOUT");
        // String (not enum) is deliberately used here to avoid IllegalArgumentException being thrown. In case of
        // incorrect property value, default WaitStrategy is created.
        switch (strategyUp) {
            case "SLEEP":
                final String sleepTimeKey = getFullPropertyKey(propertyName, "SleepTimeNs");
                final long sleepTimeNs = propertyResolver.getLong(sleepTimeKey).orElse(100L);
                final String retriesKey = getFullPropertyKey(propertyName, "Retries");
                final int retries = propertyResolver.getInt(retriesKey).orElse(200);
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating SleepingWaitStrategy(retries={}, sleepTimeNs={})", retries, sleepTimeNs);
                return new SleepingWaitStrategy(retries, sleepTimeNs);
            case "YIELD":
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating YieldingWaitStrategy");
                return new YieldingWaitStrategy();
            case "BLOCK":
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BlockingWaitStrategy");
                return new BlockingWaitStrategy();
            case "BUSYSPIN":
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BusySpinWaitStrategy");
                return new BusySpinWaitStrategy();
            case "TIMEOUT":
            default:
                return createDefaultWaitStrategy(propertyName);
        }
    }

    static WaitStrategy createDefaultWaitStrategy(final String propertyName) {
        final String key = getFullPropertyKey(propertyName, "Timeout");
        final long timeoutMillis = LoggingSystem.getPropertyResolver().getLong(key).orElse(10L);
        LOGGER.trace("DefaultAsyncWaitStrategyFactory creating TimeoutBlockingWaitStrategy(timeout={}, unit=MILLIS)", timeoutMillis);
        return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private static String getFullPropertyKey(final String strategyKey, final String additionalKey) {
        if (strategyKey.startsWith("AsyncLogger.")) {
            return "log4j2.*.AsyncLogger." + additionalKey;
        } else if (strategyKey.startsWith("AsyncLoggerConfig.")) {
            return "log4j2.*.AsyncLoggerConfig." + additionalKey;
        }
        return strategyKey + additionalKey;
    }

}
