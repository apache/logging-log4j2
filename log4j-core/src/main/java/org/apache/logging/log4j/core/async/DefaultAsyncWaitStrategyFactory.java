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

import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyKey;
import org.apache.logging.log4j.util.Strings;

class DefaultAsyncWaitStrategyFactory implements AsyncWaitStrategyFactory {
    static final String DEFAULT_WAIT_STRATEGY_CLASSNAME = TimeoutBlockingWaitStrategy.class.getName();
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final PropertyKey propertyKey;

    public DefaultAsyncWaitStrategyFactory(PropertyKey key) {
        this.propertyKey = key;
    }

    @Override
    public WaitStrategy createWaitStrategy() {
        final String strategy = PropertiesUtil.getProperties().getStringProperty(propertyKey, "TIMEOUT");
        LOGGER.trace("DefaultAsyncWaitStrategyFactory property {}={}", propertyKey, strategy);
        final String strategyUp = Strings.toRootUpperCase(strategy);
        // String (not enum) is deliberately used here to avoid IllegalArgumentException being thrown. In case of
        // incorrect property value, default WaitStrategy is created.
        switch (strategyUp) {
            case "SLEEP":
                String component = propertyKey.getComponent();
                PropertyKey key = Log4jPropertyKey.findKey(component, "sleepTimeNs");
                final long sleepTimeNs = PropertiesUtil.getProperties().getLongProperty(key, 100L);
                key = Log4jPropertyKey.findKey(component, "retries");
                final int retries = PropertiesUtil.getProperties().getIntegerProperty(key, 200);
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
                return createDefaultWaitStrategy(propertyKey);
            default:
                return createDefaultWaitStrategy(propertyKey);
        }
    }

    static WaitStrategy createDefaultWaitStrategy(final PropertyKey propertyKey) {
        String component = propertyKey.getComponent();
        PropertyKey key = Log4jPropertyKey.findKey(component, "timeout");
        final long timeoutMillis = PropertiesUtil.getProperties().getLongProperty(key, 10L);
        LOGGER.trace("DefaultAsyncWaitStrategyFactory creating TimeoutBlockingWaitStrategy(timeout={}, unit=MILLIS)", timeoutMillis);
        return new TimeoutBlockingWaitStrategy(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
