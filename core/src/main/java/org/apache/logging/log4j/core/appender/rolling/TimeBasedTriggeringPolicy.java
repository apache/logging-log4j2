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
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Triggering Policy that causes a rollover based on time.
 */
@Plugin(name = "TimeBasedTriggeringPolicy", type = "Core", printObject = true)
public final class TimeBasedTriggeringPolicy implements TriggeringPolicy {

    private long nextRollover;
    private final int interval;
    private final boolean modulate;

    private RollingFileManager manager;

    private TimeBasedTriggeringPolicy(final int interval, final boolean modulate) {
        this.interval = interval;
        this.modulate = modulate;
    }

    /**
     * Initialize the policy.
     * @param manager The RollingFileManager.
     */
    public void initialize(final RollingFileManager manager) {
        this.manager = manager;
        nextRollover = manager.getProcessor().getNextTime(manager.getFileTime(), interval, modulate);
    }

    /**
     * Determine whether a rollover should occur.
     * @param event   A reference to the currently event.
     * @return true if a rollover should occur.
     */
    public boolean isTriggeringEvent(final LogEvent event) {
        if (manager.getFileSize() == 0) {
            return false;
        }
        final long now = System.currentTimeMillis();
        if (now > nextRollover) {
            nextRollover = manager.getProcessor().getNextTime(now, interval, modulate);
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
     * @param interval The interval between rollovers.
     * @param modulate If true the time will be rounded to occur on a boundary aligned with the increment.
     * @return a TimeBasedTriggeringPolicy.
     */
    @PluginFactory
    public static TimeBasedTriggeringPolicy createPolicy(@PluginAttr("interval") final String interval,
                                                         @PluginAttr("modulate") final String modulate) {
        final int increment = interval == null ? 1 : Integer.parseInt(interval);
        final boolean mod = modulate == null ? false : Boolean.parseBoolean(modulate);
        return new TimeBasedTriggeringPolicy(increment, mod);
    }
}
