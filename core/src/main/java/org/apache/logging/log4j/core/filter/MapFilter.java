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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A Filter that operates on a Map.
 */
@Plugin(name = "MapFilter", type = "Core", elementType = "filter", printObject = true)
public class MapFilter extends AbstractFilter {
    private final Map<String, List<String>> map;

    private final boolean isAnd;

    protected MapFilter(Map<String, List<String>> map, boolean oper, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        if (map == null) {
            throw new NullPointerException("key cannot be null");
        }
        this.isAnd = oper;
        this.map = map;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (msg instanceof MapMessage) {
            return filter(((MapMessage) msg).getData()) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(LogEvent event) {
        Message msg = event.getMessage();
        if (msg instanceof MapMessage) {
            return filter(((MapMessage) msg).getData()) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;
    }

    protected boolean filter(Map<String, String> data) {
        boolean match = false;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String toMatch = data.get(entry.getKey());
            if (toMatch != null) {
                match = entry.getValue().contains(toMatch);
            } else {
                match = false;
            }
            if ((!isAnd && match) || (isAnd && !match)) {
                break;
            }
        }
        return match;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("isAnd=").append(isAnd);
        if (map.size() > 0) {
            sb.append(", {");
            boolean first = true;
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                List<String> list = entry.getValue();
                String value = list.size() > 1 ? list.get(0) : list.toString();
                sb.append(entry.getKey()).append("=").append(value);
            }
            sb.append("}");
        }
        return sb.toString();
    }

    protected boolean isAnd() {
        return isAnd;
    }

    protected Map<String, List<String>> getMap() {
        return map;
    }

    @PluginFactory
    public static MapFilter createFilter(@PluginElement("pairs") KeyValuePair[] pairs,
                                         @PluginAttr("operator") String oper,
                                         @PluginAttr("onmatch") String match,
                                         @PluginAttr("onmismatch") String mismatch) {
        if (pairs == null || pairs.length == 0) {
            LOGGER.error("keys and values must be specified for the MapFilter");
            return null;
        }
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (KeyValuePair pair : pairs) {
            String key = pair.getKey();
            if (key == null) {
                LOGGER.error("A null key is not valid in MapFilter");
                continue;
            }
            String value = pair.getValue();
            if (value == null) {
                LOGGER.error("A null value for key " + key + " is not allowed in MapFilter");
                continue;
            }
            List<String> list = map.get(pair.getKey());
            if (list != null) {
                list.add(value);
            } else {
                list = new ArrayList<String>();
                list.add(value);
                map.put(pair.getKey(), list);
            }
        }
        if (map.size() == 0) {
            LOGGER.error("MapFilter is not configured with any valid key value pairs");
            return null;
        }
        boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        Result onMatch = match == null ? null : Result.valueOf(match.toUpperCase(Locale.ENGLISH));
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch.toUpperCase(Locale.ENGLISH));
        return new MapFilter(map, isAnd, onMatch, onMismatch);
    }
}
