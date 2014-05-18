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
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Integers;

/**
 * Triggering Policy that causes a rollover based on time.
 */
@Plugin(name = "TimeBasedTriggeringPolicy", category = "Core", printObject = true)
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
    @Override
    public void initialize(final RollingFileManager manager) {
        this.manager = manager;
        
        // LOG4J2-531: call getNextTime twice to force initialization of both prevFileTime and nextFileTime
        manager.getPatternProcessor().getNextTime(manager.getFileTime(), interval, modulate);
        
        nextRollover = manager.getPatternProcessor().getNextTime(manager.getFileTime(), interval, modulate);
    }

    /**
     * Determine whether a rollover should occur.
     * @param event   A reference to the currently event.
     * @return true if a rollover should occur.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        if (manager.getFileSize() == 0) {
            return false;
        }
        final long now = event.getTimeMillis();
        if (now > nextRollover) {
            nextRollover = manager.getPatternProcessor().getNextTime(now, interval, modulate);
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
    public static TimeBasedTriggeringPolicy createPolicy(
            @PluginAttribute("interval") final String interval,
            @PluginAttribute("modulate") final String modulate) {
        final int increment = Integers.parseInt(interval, 1);
        final boolean mod = Boolean.parseBoolean(modulate);
        return new TimeBasedTriggeringPolicy(increment, mod);
    }
}
