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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Triggering Policy that causes a rollover based on time.
 */
@Plugin(name = "TimeBasedTriggeringPolicy", type = "Core", printObject = true)
public final class TimeBasedTriggeringPolicy implements TriggeringPolicy {

    private long nextRollover;

    private RollingFileManager manager;

    private TimeBasedTriggeringPolicy() {
    }

    /**
     * Initialize the policy.
     * @param manager The RollingFileManager.
     */
    public void initialize(RollingFileManager manager) {
        this.manager = manager;
        nextRollover = manager.getProcessor().getNextTime(manager.getFileTime());
    }

    /**
     * Determine whether a rollover should occur.
     * @param event   A reference to the currently event.
     * @return true if a rollover should occur.
     */
    public boolean isTriggeringEvent(LogEvent event) {
        if (manager.getFileSize() == 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now > nextRollover) {
            nextRollover = manager.getProcessor().getNextTime(now);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "TimeBasedTriggeringPolicy";
    }

    /**
     * Create a TimeBasedTriggeringPolicy.
     * @return a TimeBasedTriggeringPolicy.
     */
    @PluginFactory
    public static TimeBasedTriggeringPolicy createPolicy() {
        return new TimeBasedTriggeringPolicy();
    }
}
