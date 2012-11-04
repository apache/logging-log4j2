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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.KeyValuePair;
import org.apache.logging.log4j.message.Message;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Compare against a log level that is associated with an MDC value.
 */
@Plugin(name = "DynamicThresholdFilter", type = "Core", elementType = "filter", printObject = true)
public final class DynamicThresholdFilter extends AbstractFilter {
    private Map<String, Level> levelMap = new HashMap<String, Level>();
    private Level defaultThreshold = Level.ERROR;
    private String key;

    private DynamicThresholdFilter(String key, Map<String, Level> pairs, Level defaultLevel,
                                   Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        this.key = key;
        this.levelMap = pairs;
        this.defaultThreshold = defaultLevel;
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object[] params) {
        return filter(level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(level);
    }

    @Override
    public Result filter(LogEvent event) {
        return filter(event.getLevel());
    }

    private Result filter(Level level) {
        Object value = ThreadContext.get(key);
        if (value != null) {
            Level ctxLevel = levelMap.get(value);
            if (ctxLevel == null) {
                ctxLevel = defaultThreshold;
            }
            return level.isAtLeastAsSpecificAs(ctxLevel) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;

    }

    public Map<String, Level> getLevelMap() {
        return levelMap;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("key=").append(key);
        sb.append(", default=").append(defaultThreshold);
        if (levelMap.size() > 0) {
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Level> entry : levelMap.entrySet()) {
                if (!first) {
                    sb.append(", ");
                    first = false;
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * Create the DynamicThresholdFilter.
     * @param key The name of the key to compare.
     * @param pairs An array of value and Level pairs.
     * @param level The default Level.
     * @param match The action to perform if a match occurs.
     * @param mismatch The action to perform if no match occurs.
     * @return The DynamicThresholdFilter.
     */
    @PluginFactory
    public static DynamicThresholdFilter createFilter(@PluginAttr("key") String key,
                                                      @PluginElement("pairs") KeyValuePair[] pairs,
                                                      @PluginAttr("defaultThreshold") String level,
                                                      @PluginAttr("onmatch") String match,
                                                      @PluginAttr("onmismatch") String mismatch) {
        Result onMatch = Result.toResult(match);
        Result onMismatch = Result.toResult(mismatch);
        Map<String, Level> map = new HashMap<String, Level>();
        for (KeyValuePair pair : pairs) {
            map.put(pair.getKey(), Level.toLevel(pair.getValue().toUpperCase(Locale.ENGLISH)));
        }
        Level l = Level.toLevel(level, Level.ERROR);
        return new DynamicThresholdFilter(key, map, l, onMatch, onMismatch);
    }
}
