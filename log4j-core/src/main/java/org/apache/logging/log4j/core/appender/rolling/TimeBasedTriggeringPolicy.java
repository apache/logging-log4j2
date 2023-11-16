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
package org.apache.logging.log4j.core.appender.rolling;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.util.Integers;

/**
 * Rolls a file over based on time.
 */
@Plugin(name = "TimeBasedTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
public final class TimeBasedTriggeringPolicy extends AbstractTriggeringPolicy {

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<TimeBasedTriggeringPolicy> {

        @PluginBuilderAttribute
        private int interval = 1;

        @PluginBuilderAttribute
        private boolean modulate = false;

        @PluginBuilderAttribute
        private int maxRandomDelay = 0;

        @Override
        public TimeBasedTriggeringPolicy build() {
            final long maxRandomDelayMillis = TimeUnit.SECONDS.toMillis(maxRandomDelay);
            return new TimeBasedTriggeringPolicy(interval, modulate, maxRandomDelayMillis);
        }

        public int getInterval() {
            return interval;
        }

        public boolean isModulate() {
            return modulate;
        }

        public int getMaxRandomDelay() {
            return maxRandomDelay;
        }

        public Builder withInterval(final int interval) {
            this.interval = interval;
            return this;
        }

        public Builder withModulate(final boolean modulate) {
            this.modulate = modulate;
            return this;
        }

        public Builder withMaxRandomDelay(final int maxRandomDelay) {
            this.maxRandomDelay = maxRandomDelay;
            return this;
        }
    }

    private long nextRolloverMillis;
    private final int interval;
    private final boolean modulate;
    private final long maxRandomDelayMillis;

    private RollingFileManager manager;

    private TimeBasedTriggeringPolicy(final int interval, final boolean modulate, final long maxRandomDelayMillis) {
        this.interval = interval;
        this.modulate = modulate;
        this.maxRandomDelayMillis = maxRandomDelayMillis;
    }

    public int getInterval() {
        return interval;
    }

    public long getNextRolloverMillis() {
        return nextRolloverMillis;
    }

    /**
     * Initializes the policy.
     * @param aManager The RollingFileManager.
     */
    @Override
    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    public void initialize(final RollingFileManager aManager) {
        this.manager = aManager;
        long current = aManager.getFileTime();
        if (current == 0) {
            current = System.currentTimeMillis();
        }

        // LOG4J2-531: call getNextTime twice to force initialization of both prevFileTime and nextFileTime
        aManager.getPatternProcessor().getNextTime(current, interval, modulate);
        aManager.getPatternProcessor().setTimeBased(true);

        nextRolloverMillis = ThreadLocalRandom.current().nextLong(0, 1 + maxRandomDelayMillis)
                + aManager.getPatternProcessor().getNextTime(current, interval, modulate);
    }

    /**
     * Determines whether a rollover should occur.
     * @param event   A reference to the currently event.
     * @return true if a rollover should occur.
     */
    @Override
    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    public boolean isTriggeringEvent(final LogEvent event) {
        final long nowMillis = event.getTimeMillis();
        if (nowMillis >= nextRolloverMillis) {
            nextRolloverMillis = ThreadLocalRandom.current().nextLong(0, 1 + maxRandomDelayMillis)
                    + manager.getPatternProcessor().getNextTime(nowMillis, interval, modulate);
            manager.getPatternProcessor().setCurrentFileTime(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    /**
     * Creates a TimeBasedTriggeringPolicy.
     * @param interval The interval between rollovers.
     * @param modulate If true the time will be rounded to occur on a boundary aligned with the increment.
     * @return a TimeBasedTriggeringPolicy.
     * @deprecated Use {@link #newBuilder()}.
     */
    @Deprecated
    public static TimeBasedTriggeringPolicy createPolicy(
            @PluginAttribute("interval") final String interval, @PluginAttribute("modulate") final String modulate) {
        return newBuilder()
                .withInterval(Integers.parseInt(interval, 1))
                .withModulate(Boolean.parseBoolean(modulate))
                .build();
    }

    @PluginBuilderFactory
    public static TimeBasedTriggeringPolicy.Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "TimeBasedTriggeringPolicy(nextRolloverMillis=" + nextRolloverMillis + ", interval=" + interval
                + ", modulate=" + modulate + ")";
    }
}
