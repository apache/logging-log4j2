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
package org.apache.logging.log4j.core.filter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.message.Message;

/**
 * The <code>BurstFilter</code> is a logging filter that regulates logging traffic.
 *
 * <p>
 * Use this filter when you want to control the maximum burst of log statements that can be sent to an appender. The
 * filter is configured in the log4j configuration file. For example, the following configuration limits the number of
 * INFO level (as well as DEBUG and TRACE) log statements that can be sent to the console to a burst of 100 with an
 * average rate of 16 per second. WARN, ERROR and FATAL messages would continue to be delivered.
 * </p>
 * <code>
 * &lt;Console name="console"&gt;<br>
 * &nbsp;&lt;PatternLayout pattern="%-5p %d{dd-MMM-yyyy HH:mm:ss} %x %t %m%n"/&gt;<br>
 * &nbsp;&lt;Filters&gt;<br>
 * &nbsp;&nbsp;&lt;BurstFilter level="INFO" rate="16" maxBurst="100"/&gt;<br>
 * &nbsp;&lt;/Filters&gt;<br>
 * &lt;/Console&gt;<br>
 * </code><br>
 */
@Plugin(name = "BurstFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public final class BurstFilter extends AbstractFilter {

    private static final long NANOS_IN_SECONDS = 1000000000;

    private static final int DEFAULT_RATE = 10;

    private static final int DEFAULT_RATE_MULTIPLE = 100;

    private static final int HASH_SHIFT = 32;

    /**
     * Level of messages to be filtered. Anything at or below this level will be
     * filtered out if <code>maxBurst</code> has been exceeded. The default is
     * WARN meaning any messages that are higher than warn will be logged
     * regardless of the size of a burst.
     */
    private final Level level;

    private final long burstInterval;

    private final DelayQueue<LogDelay> history = new DelayQueue<>();

    private final Queue<LogDelay> available = new ConcurrentLinkedQueue<>();

    static LogDelay createLogDelay(final long expireTime) {
        return new LogDelay(expireTime);
    }

    private BurstFilter(
            final Level level, final float rate, final long maxBurst, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.level = level;
        this.burstInterval = (long) (NANOS_IN_SECONDS * (maxBurst / rate));
        for (int i = 0; i < maxBurst; ++i) {
            available.add(createLogDelay(0));
        }
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getLevel());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return filter(level);
    }

    /**
     * Decide if we're going to log <code>event</code> based on whether the
     * maximum burst of log statements has been exceeded.
     *
     * @param level The log level.
     * @return The onMatch value if the filter passes, onMismatch otherwise.
     */
    private Result filter(final Level level) {
        if (this.level.isMoreSpecificThan(level)) {
            LogDelay delay = history.poll();
            while (delay != null) {
                available.add(delay);
                delay = history.poll();
            }
            delay = available.poll();
            if (delay != null) {
                delay.setDelay(burstInterval);
                history.add(delay);
                return onMatch;
            }
            return onMismatch;
        }
        return onMatch;
    }

    /**
     * Returns the number of available slots. Used for unit testing.
     * @return The number of available slots.
     */
    public int getAvailable() {
        return available.size();
    }

    /**
     * Clear the history. Used for unit testing.
     */
    public void clear() {
        for (final LogDelay delay : history) {
            history.remove(delay);
            available.add(delay);
        }
    }

    @Override
    public String toString() {
        return "level=" + level.toString() + ", interval=" + burstInterval + ", max=" + history.size();
    }

    /**
     * Delay object to represent each log event that has occurred within the timespan.
     *
     * Consider this class private, package visibility for testing.
     */
    private static class LogDelay implements Delayed {

        LogDelay(final long expireTime) {
            this.expireTime = expireTime;
        }

        private long expireTime;

        public void setDelay(final long delay) {
            this.expireTime = delay + System.nanoTime();
        }

        @Override
        public long getDelay(final TimeUnit timeUnit) {
            return timeUnit.convert(expireTime - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(final Delayed delayed) {
            final long diff = this.expireTime - ((LogDelay) delayed).expireTime;
            return Long.signum(diff);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LogDelay logDelay = (LogDelay) o;

            if (expireTime != logDelay.expireTime) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return (int) (expireTime ^ (expireTime >>> HASH_SHIFT));
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractFilterBuilder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<BurstFilter> {

        @PluginBuilderAttribute
        private Level level = Level.WARN;

        @PluginBuilderAttribute
        private float rate = DEFAULT_RATE;

        @PluginBuilderAttribute
        private long maxBurst;

        /**
         * Sets the logging level to use.
         * @param level the logging level to use.
         * @return this
         */
        public Builder setLevel(final Level level) {
            this.level = level;
            return this;
        }

        /**
         * Sets the average number of events per second to allow.
         * @param rate the average number of events per second to allow. This must be a positive number.
         * @return this
         */
        public Builder setRate(final float rate) {
            this.rate = rate;
            return this;
        }

        /**
         * Sets the maximum number of events that can occur before events are filtered for exceeding the average rate.
         * @param maxBurst Sets the maximum number of events that can occur before events are filtered for exceeding the average rate.
         * The default is 10 times the rate.
         * @return this
         */
        public Builder setMaxBurst(final long maxBurst) {
            this.maxBurst = maxBurst;
            return this;
        }

        @Override
        public BurstFilter build() {
            if (this.rate <= 0) {
                this.rate = DEFAULT_RATE;
            }
            if (this.maxBurst <= 0) {
                this.maxBurst = (long) (this.rate * DEFAULT_RATE_MULTIPLE);
            }
            return new BurstFilter(this.level, this.rate, this.maxBurst, this.getOnMatch(), this.getOnMismatch());
        }
    }
}
