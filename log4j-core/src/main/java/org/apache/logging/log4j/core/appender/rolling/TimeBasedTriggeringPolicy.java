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
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Rolls a file over based on time.
 */
@Configurable(printObject = true)
@Plugin
public final class TimeBasedTriggeringPolicy extends AbstractTriggeringPolicy {


    public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<TimeBasedTriggeringPolicy> {

        @PluginBuilderAttribute
        private int interval = 1;

        @PluginBuilderAttribute
        private boolean modulate = false;

        @PluginBuilderAttribute
        private int maxRandomDelay = 0;

        private Clock clock;

        @Override
        public TimeBasedTriggeringPolicy build() {
            final long maxRandomDelayMillis = TimeUnit.SECONDS.toMillis(maxRandomDelay);
            return new TimeBasedTriggeringPolicy(interval, modulate, maxRandomDelayMillis, clock);
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

        public Clock getClock() {
            return clock;
        }

        public Builder setInterval(final int interval){
            this.interval = interval;
            return this;
        }

        public Builder setModulate(final boolean modulate){
            this.modulate = modulate;
            return this;
        }

        public Builder setMaxRandomDelay(final int maxRandomDelay){
            this.maxRandomDelay = maxRandomDelay;
            return this;
        }

        @Inject
        public Builder setClock(final Clock clock) {
            this.clock = clock;
            return this;
        }
    }

    private long nextRolloverMillis;
    private final int interval;
    private final boolean modulate;
    private final long maxRandomDelayMillis;
    private final Clock clock;

    private RollingFileManager manager;

    private TimeBasedTriggeringPolicy(
            final int interval, final boolean modulate, final long maxRandomDelayMillis, final Clock clock) {
        this.interval = interval;
        this.modulate = modulate;
        this.maxRandomDelayMillis = maxRandomDelayMillis;
        this.clock = clock;
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
    public void initialize(final RollingFileManager aManager) {
        this.manager = aManager;
        long current = aManager.getFileTime();
        if (current == 0) {
            current = clock.currentTimeMillis();
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
    public boolean isTriggeringEvent(final LogEvent event) {
        final long nowMillis = event.getTimeMillis();
        if (nowMillis >= nextRolloverMillis) {
            nextRolloverMillis = ThreadLocalRandom.current().nextLong(0, 1 + maxRandomDelayMillis)
                    + manager.getPatternProcessor().getNextTime(nowMillis, interval, modulate);
            manager.getPatternProcessor().setCurrentFileTime(clock.currentTimeMillis());
            return true;
        }
        return false;
    }

    @PluginFactory
    public static TimeBasedTriggeringPolicy.Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "TimeBasedTriggeringPolicy(nextRolloverMillis=" + nextRolloverMillis + ", interval=" + interval
                + ", modulate=" + modulate + ")";
    }

}
