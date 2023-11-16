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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Factory for ReliabilityStrategies.
 */
public final class ReliabilityStrategyFactory {
    private ReliabilityStrategyFactory() {}

    /**
     * Returns a new {@code ReliabilityStrategy} instance based on the value of system property
     * {@code log4j.ReliabilityStrategy}. If not value was specified this method returns a new
     * {@code AwaitUnconditionallyReliabilityStrategy}.
     * <p>
     * Valid values for this system property are {@code "AwaitUnconditionally"} (use
     * {@code AwaitUnconditionallyReliabilityStrategy}), {@code "Locking"} (use {@code LockingReliabilityStrategy}) and
     * {@code "AwaitCompletion"} (use the default {@code AwaitCompletionReliabilityStrategy}).
     * <p>
     * Users may also use this system property to specify the fully qualified class name of a class that implements the
     * {@code ReliabilityStrategy} and has a constructor that accepts a single {@code LoggerConfig} argument.
     *
     * @param loggerConfig the LoggerConfig the resulting {@code ReliabilityStrategy} is associated with
     * @return a ReliabilityStrategy that helps the specified LoggerConfig to log events reliably during or after a
     *         configuration change
     */
    public static ReliabilityStrategy getReliabilityStrategy(final LoggerConfig loggerConfig) {

        final String strategy =
                PropertiesUtil.getProperties().getStringProperty("log4j.ReliabilityStrategy", "AwaitCompletion");
        if ("AwaitCompletion".equals(strategy)) {
            return new AwaitCompletionReliabilityStrategy(loggerConfig);
        }
        if ("AwaitUnconditionally".equals(strategy)) {
            return new AwaitUnconditionallyReliabilityStrategy(loggerConfig);
        }
        if ("Locking".equals(strategy)) {
            return new LockingReliabilityStrategy(loggerConfig);
        }
        try {
            final Class<? extends ReliabilityStrategy> cls =
                    Loader.loadClass(strategy).asSubclass(ReliabilityStrategy.class);
            return cls.getConstructor(LoggerConfig.class).newInstance(loggerConfig);
        } catch (final Exception dynamicFailed) {
            StatusLogger.getLogger()
                    .warn(
                            "Could not create ReliabilityStrategy for '{}', using default AwaitCompletionReliabilityStrategy: {}",
                            strategy,
                            dynamicFailed);
            return new AwaitCompletionReliabilityStrategy(loggerConfig);
        }
    }
}
