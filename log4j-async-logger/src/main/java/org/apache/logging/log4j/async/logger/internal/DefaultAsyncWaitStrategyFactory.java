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

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.async.logger.AsyncLoggerProperties.WaitStrategyProperties;
import org.apache.logging.log4j.async.logger.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.status.StatusLogger;

public class DefaultAsyncWaitStrategyFactory implements AsyncWaitStrategyFactory {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final WaitStrategyProperties properties;

    public DefaultAsyncWaitStrategyFactory(final WaitStrategyProperties properties) {
        this.properties = properties;
    }

    @Override
    public WaitStrategy createWaitStrategy() {
        LOGGER.trace("DefaultAsyncWaitStrategyFactory strategy name: {}", properties.type());
        // String (not enum) is deliberately used here to avoid IllegalArgumentException being thrown. In case of
        // incorrect property value, default WaitStrategy is created.
        return switch (toRootUpperCase(properties.type())) {
            case "SLEEP" -> {
                LOGGER.trace(
                        "DefaultAsyncWaitStrategyFactory creating SleepingWaitStrategy(retries={}, sleepTimeNs={})",
                        properties.retries(),
                        properties.sleepTimeNs());
                yield new SleepingWaitStrategy(properties.retries(), properties.sleepTimeNs());
            }
            case "YIELD" -> {
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating YieldingWaitStrategy");
                yield new YieldingWaitStrategy();
            }
            case "BLOCK" -> {
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BlockingWaitStrategy");
                yield new BlockingWaitStrategy();
            }
            case "BUSYSPIN" -> {
                LOGGER.trace("DefaultAsyncWaitStrategyFactory creating BusySpinWaitStrategy");
                yield new BusySpinWaitStrategy();
            }
            default -> {
                LOGGER.trace(
                        "DefaultAsyncWaitStrategyFactory creating TimeoutBlockingWaitStrategy(timeout={}, unit=MILLIS)",
                        properties.timeout());
                yield new TimeoutBlockingWaitStrategy(properties.timeout(), TimeUnit.MILLISECONDS);
            }
        };
    }
}
