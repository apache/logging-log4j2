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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * This filter returns the {@link #onMatch} result if the level of the {@link LogEvent} is in the range of the configured {@link #minLevel} and {@link #maxLevel} values, otherwise it returns the {@link #onMismatch} result.
 * The default values for {@link #minLevel} and {@link #maxLevel} are set to {@link Level#OFF} and {@link Level#ALL}, respectively.
 * The default values for {@link #onMatch} and {@link #onMismatch} are set to {@link Result#NEUTRAL} and {@link Result#DENY}, respectively.
 * <p>
 * The levels get compared by their associated integral values; {@link Level#OFF} has an integral value of 0, {@link Level#FATAL} 100, {@link Level#ERROR} 200, and so on.
 * For example, if the filter is configured with {@link #maxLevel} set to {@link Level#INFO}, the filter will return {@link #onMismatch} result for {@link LogEvent}s of level with higher integral values; {@link Level#DEBUG}, {@link Level#TRACE}, etc.
 * </p>
 */
@Plugin(name = "LevelRangeFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
@PerformanceSensitive("allocation")
public final class LevelRangeFilter extends AbstractFilter {

    /**
     * The default minimum level threshold.
     */
    public static final Level DEFAULT_MIN_LEVEL = Level.OFF;

    /**
     * THe default maximum level threshold.
     */
    public static final Level DEFAULT_MAX_LEVEL = Level.ALL;

    /**
     * The default result on a match.
     */
    public static final Result DEFAULT_ON_MATCH = Result.NEUTRAL;

    /**
     * The default result on a mismatch.
     */
    public static final Result DEFAULT_ON_MISMATCH = Result.DENY;

    /**
     * Creates an instance with the provided properties.
     *
     * @param minLevel the minimum level threshold
     * @param maxLevel the maximum level threshold
     * @param onMatch the result to return on a match
     * @param onMismatch the result to return on a mismatch
     * @return a new instance
     */
    @PluginFactory
    public static LevelRangeFilter createFilter(
            // @formatter:off
            @PluginAttribute("minLevel") final Level minLevel,
            @PluginAttribute("maxLevel") final Level maxLevel,
            @PluginAttribute("onMatch") final Result onMatch,
            @PluginAttribute("onMismatch") final Result onMismatch) {
        // @formatter:on
        final Level effectiveMinLevel = minLevel == null ? DEFAULT_MIN_LEVEL : minLevel;
        final Level effectiveMaxLevel = maxLevel == null ? DEFAULT_MAX_LEVEL : maxLevel;
        final Result effectiveOnMatch = onMatch == null ? DEFAULT_ON_MATCH : onMatch;
        final Result effectiveOnMismatch = onMismatch == null ? DEFAULT_ON_MISMATCH : onMismatch;
        return new LevelRangeFilter(effectiveMinLevel, effectiveMaxLevel, effectiveOnMatch, effectiveOnMismatch);
    }

    private final Level maxLevel;

    private final Level minLevel;

    private LevelRangeFilter(
            final Level minLevel, final Level maxLevel, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    private Result filter(final Level level) {
        return level.isInRange(minLevel, maxLevel) ? onMatch : onMismatch;
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getLevel());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter(level);
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
     * @return the minimum level threshold
     */
    public Level getMinLevel() {
        return minLevel;
    }

    /**
     * @return the maximum level threshold
     */
    public Level getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String toString() {
        return String.format("[%s,%s]", minLevel, maxLevel);
    }
}
