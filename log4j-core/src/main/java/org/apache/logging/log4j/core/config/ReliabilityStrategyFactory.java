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

package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Factory for ReliabilityStrategies.
 */
public class ReliabilityStrategyFactory {
    private ReliabilityStrategyFactory() {
    }

    /**
     * Returns a new {@code AwaitUnconditionallyReliabilityStrategy} if system property
     * {@code log4j.alwaysWaitBeforeStopOldConfig} was set to {@code true}, otherwise (by default return) a new
     * {@code AwaitCompletionReliabilityStrategy} instance.
     * 
     * @param loggerConfig the LoggerConfig the resulting {@code ReliabilityStrategy} is associated with
     * @return a ReliabilityStrategy that helps the specified LoggerConfig to log events reliably during or after a
     *         configuration change
     */
    public static ReliabilityStrategy getReliabilityStrategy(final LoggerConfig loggerConfig) {
        boolean waitUnconditionally = PropertiesUtil.getProperties().getBooleanProperty(
                "log4j.alwaysWaitBeforeStopOldConfig", false);
        if (waitUnconditionally) {
            return new AwaitUnconditionallyReliabilityStrategy(loggerConfig);
        }
        return new AwaitCompletionReliabilityStrategy(loggerConfig);
    }
}
