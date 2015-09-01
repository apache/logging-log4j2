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

/**
 * This filter returns the onMatch result if the level in the LogEvent is in the
 * range of the configured min and max levels, otherwise it returns onMismatch
 * value . For example, if the filter is configured with Level ERROR and Level
 * INFO and the LogEvent contains Level WARN then the onMatch value will be
 * returned since WARN events are in between ERROR and INFO.
 *
 * The default Levels are both ERROR.
 */
@Plugin(name = "LevelRangeFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public final class LevelRangeFilter extends AbstractFilter {

    private static final long serialVersionUID = 1L;

    /**
     * Create a ThresholdFilter.
     * 
     * @param minLevel
     *            The log Level.
     * @param match
     *            The action to take on a match.
     * @param mismatch
     *            The action to take on a mismatch.
     * @return The created ThresholdFilter.
     */
    @PluginFactory
    public static LevelRangeFilter createFilter(
            // @formatter:off
            @PluginAttribute("minLevel") final Level minLevel,
            @PluginAttribute("maxLevel") final Level maxLevel,
            @PluginAttribute("onMatch") final Result match, 
            @PluginAttribute("onMismatch") final Result mismatch) {
            // @formatter:on
        final Level actualMinLevel = minLevel == null ? Level.ERROR : minLevel;
        final Level actualMaxLevel = minLevel == null ? Level.ERROR : maxLevel;
        final Result onMatch = match == null ? Result.NEUTRAL : match;
        final Result onMismatch = mismatch == null ? Result.DENY : mismatch;
        return new LevelRangeFilter(actualMinLevel, actualMaxLevel, onMatch, onMismatch);
    }
    private final Level maxLevel;

    private final Level minLevel;

    private LevelRangeFilter(final Level minLevel, final Level maxLevel, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    private Result filter(final Level level) {
        return level.isInRange(this.minLevel, this.maxLevel) ? onMatch : onMismatch;
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getLevel());
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
            final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
            final Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object... params) {
        return filter(level);
    }

    public Level getMinLevel() {
        return minLevel;
    }

    @Override
    public String toString() {
        return minLevel.toString();
    }

}
