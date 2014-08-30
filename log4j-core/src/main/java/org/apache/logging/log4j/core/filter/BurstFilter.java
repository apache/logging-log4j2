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
package org.apache.logging.log4j.core.filter;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

/**
 * The <code>BurstFilter</code> is a logging filter that regulates logging
 * traffic. Use this filter when you want to control the maximum burst of log
 * statements that can be sent to an appender. The filter is configured in the
 * log4j configuration file. For example, the following configuration limits the
 * number of INFO level (as well as DEBUG and TRACE) log statements that can be sent to the
 * console to a burst of 100 with an average rate of 16 per second. WARN, ERROR and FATAL messages would continue to
 * be delivered.<br>
 * <br>
 * <p/>
 * <code>
 * &lt;Console name="console"&gt;<br>
 * &nbsp;&lt;PatternLayout pattern="%-5p %d{dd-MMM-yyyy HH:mm:ss} %x %t %m%n"/&gt;<br>
 * &nbsp;&lt;filters&gt;<br>
 * &nbsp;&nbsp;&lt;Burst level="INFO" rate="16" maxBurst="100"/&gt;<br>
 * &nbsp;&lt;/filters&gt;<br>
 * &lt;/Console&gt;<br>
 * </code><br>
 */

@Plugin(name = "BurstFilter", category = "Core", elementType = "filter", printObject = true)
public final class BurstFilter extends AbstractFilter {

    private static final long serialVersionUID = 1L;

    private static final long NANOS_IN_SECONDS =  1000000000;

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

    private final DelayQueue<LogDelay> history = new DelayQueue<LogDelay>();

    private final Queue<LogDelay> available = new ConcurrentLinkedQueue<LogDelay>();

    private BurstFilter(final Level level, final float rate, final long maxBurst, final Result onMatch,
                        final Result onMismatch) {
        super(onMatch, onMismatch);
        this.level = level;
        this.burstInterval = (long) (NANOS_IN_SECONDS * (maxBurst / rate));
        for (int i = 0; i < maxBurst; ++i) {
            available.add(new LogDelay());
        }
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        return filter(level);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getLevel());
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
        final Iterator<LogDelay> iter = history.iterator();
        while (iter.hasNext()) {
            final LogDelay delay = iter.next();
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
     */
    private class LogDelay implements Delayed {

        private long expireTime;

        public LogDelay() {
        }

        public void setDelay(final long delay) {
            this.expireTime = delay + System.nanoTime();
        }

        @Override
        public long getDelay(final TimeUnit timeUnit) {
            return timeUnit.convert(expireTime - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(final Delayed delayed) {
            if (this.expireTime < ((LogDelay) delayed).expireTime) {
                return -1;
            } else if (this.expireTime > ((LogDelay) delayed).expireTime) {
                return 1;
            }
            return 0;
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

    /**
     * @param level  The logging level.
     * @param rate   The average number of events per second to allow.
     * @param maxBurst  The maximum number of events that can occur before events are filtered for exceeding the
     * average rate. The default is 10 times the rate.
     * @param match  The Result to return when the filter matches. Defaults to Result.NEUTRAL.
     * @param mismatch The Result to return when the filter does not match. The default is Result.DENY.
     * @return A BurstFilter.
     */
    @PluginFactory
    public static BurstFilter createFilter(
            @PluginAttribute("level") final Level level,
            @PluginAttribute("rate") final Float rate,
            @PluginAttribute("maxBurst") final Long maxBurst,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch) {
        final Result onMatch = match == null ? Result.NEUTRAL : match;
        final Result onMismatch = mismatch == null ? Result.DENY : mismatch;
        final Level actualLevel = level == null ? Level.WARN : level;
        float eventRate = rate == null ? DEFAULT_RATE : rate;
        if (eventRate <= 0) {
            eventRate = DEFAULT_RATE;
        }
        final long max = maxBurst == null ? (long) (eventRate * DEFAULT_RATE_MULTIPLE) : maxBurst;
        return new BurstFilter(actualLevel, eventRate, max, onMatch, onMismatch);
    }
}
