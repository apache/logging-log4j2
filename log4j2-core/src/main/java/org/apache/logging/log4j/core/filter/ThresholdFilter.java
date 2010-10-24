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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

import java.util.Map;

/**
 * This filter returns the onMatch result if the level in the LogEvent is the same or more specific
 * than the configured level and the onMismatch value otherwise. For example, if the ThresholdFilter
 * is configured with Level ERROR and the LogEvent contains Level DEBUG then the onMismatch value will
 * be returned since ERROR events are more specific than DEBUG.
 *
 * The default Level is ERROR.
 */
@Plugin(name="Threshold", type="Core", elementType="filter")
public class ThresholdFilter extends FilterBase {

    private static final String LEVEL = "level";

    private final Level level;

    public ThresholdFilter(Level level, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        this.level = level;
    }

    public Result filter(Logger logger, Level level, Marker marker, String msg, Object[] params) {
        return filter(level);
    }

    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(level);
    }

    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(LogEvent event) {
        return filter(event.getLevel());
    }

    private Result filter(Level level) {
        return this.level.greaterOrEqual(level) ? onMatch : onMismatch;
    }

    @PluginFactory
    public static ThresholdFilter createFilter(@PluginAttr("level") String loggerLevel,
                                               @PluginAttr("onMatch") String match,
                                               @PluginAttr("onMismatch") String mismatch) {
        Level level = loggerLevel == null ? Level.ERROR : Level.toLevel(loggerLevel.toUpperCase());
        Result onMatch = match == null ? null : Result.valueOf(match);
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch);

        return new ThresholdFilter(level, onMatch, onMismatch);
    }

}
