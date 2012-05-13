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
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * A Filter that operates on a Map.
 */
@Plugin(name = "MapFilter", type = "Core", elementType = "filter", printObject = true)
public class MapFilter extends FilterBase {
    private final Map<String, String> map;

    private final boolean isAnd;

    protected MapFilter(Map<String, String> map, boolean oper, Result onMatch, Result onMismatch) {
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
            return filter((MapMessage) msg);
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(LogEvent event) {
        Message msg = event.getMessage();
        if (msg instanceof MapMessage) {
            return filter((MapMessage) msg);
        }
        return Result.NEUTRAL;
    }

    protected Result filter(MapMessage msg) {
        boolean match = false;
        for (String key : map.keySet()) {
            String data = msg.getData().get(key);
            match = map.get(key).equals(data);
            if ((!isAnd && match) || (isAnd && !match)) {
                break;
            }
        }
        return match ? onMatch : onMismatch;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("isAnd=").append(isAnd);
        if (map.size() > 0) {
            sb.append(", {");
            boolean first = true;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            sb.append("}");
        }
        return sb.toString();
    }

    protected boolean isAnd() {
        return isAnd;
    }

    protected Map<String, String> getMap() {
        return map;
    }

    @PluginFactory
    public static MapFilter createFilter(@PluginAttr("pairs") KeyValuePair[] pairs,
                                                    @PluginAttr("operator") String oper,
                                                    @PluginAttr("onmatch") String match,
                                                    @PluginAttr("onmismatch") String mismatch) {
        if (pairs == null || pairs.length == 0) {
            LOGGER.error("keys and values must be specified for the MapFilter");
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
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
            map.put(pair.getKey(), pair.getValue());
        }
        if (map.size() == 0) {
            LOGGER.error("MapFilter is not configured with any valid key value pairs");
            return null;
        }
        boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        Result onMatch = match == null ? null : Result.valueOf(match.toUpperCase());
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch.toUpperCase());
        return new MapFilter(map, isAnd, onMatch, onMismatch);
    }
}
