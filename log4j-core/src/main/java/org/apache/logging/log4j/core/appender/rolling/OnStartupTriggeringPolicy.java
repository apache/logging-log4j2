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
package org.apache.logging.log4j.core.appender.rolling;

import java.lang.management.ManagementFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Trigger a rollover on every restart. The target file's timestamp is compared with the JVM start time
 * and if it is older isTriggeringEvent will return true. After isTriggeringEvent has been called it will
 * always return false.
 */

@Plugin(name = "OnStartupTriggeringPolicy", category = "Core", printObject = true)
public class OnStartupTriggeringPolicy implements TriggeringPolicy {
    private static long JVM_START_TIME = ManagementFactory.getRuntimeMXBean().getStartTime();

    private static final Logger LOGGER = StatusLogger.getLogger();

    private boolean evaluated = false;

    private RollingFileManager manager;

    /* static {
        try {
            JVM_START_TIME = ManagementFactory.getRuntimeMXBean().getStartTime();
        } catch (Exception ex) {
            LOGGER.error("Unable to calculate JVM start time - {}", ex.getMessage());
        }
    } */

    /**
     * Provide the RollingFileManager to the policy.
     * @param manager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager manager) {
        this.manager = manager;
        if (JVM_START_TIME == 0) {
            evaluated = true;
        }
    }

    /**
     * Determine if a rollover should be triggered.
     * @param event   A reference to the current event.
     * @return true if the target file's timestamp is older than the JVM start time.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        if (evaluated) {
            return false;
        }
        evaluated = true;
        return manager.getFileTime() < JVM_START_TIME;
    }

    @Override
    public String toString() {
        return "OnStartupTriggeringPolicy";
    }

    @PluginFactory
    public static OnStartupTriggeringPolicy createPolicy() {
        return new OnStartupTriggeringPolicy();
    }
}
