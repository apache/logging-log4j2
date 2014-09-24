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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;

/**
 * A Filter that operates on a Map.
 */
@Plugin(name = "MapFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class MapFilter extends AbstractFilter {

    private static final long serialVersionUID = 1L;

    private final Map<String, List<String>> map;

    private final boolean isAnd;

    protected MapFilter(final Map<String, List<String>> map, final boolean oper, final Result onMatch,
                        final Result onMismatch) {
        super(onMatch, onMismatch);
        if (map == null) {
            throw new NullPointerException("key cannot be null");
        }
        this.isAnd = oper;
        this.map = map;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        if (msg instanceof MapMessage) {
            return filter(((MapMessage) msg).getData()) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(final LogEvent event) {
        final Message msg = event.getMessage();
        if (msg instanceof MapMessage) {
            return filter(((MapMessage) msg).getData()) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;
    }

    protected boolean filter(final Map<String, String> data) {
        boolean match = false;
        for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
            final String toMatch = data.get(entry.getKey());
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
        final StringBuilder sb = new StringBuilder();
        sb.append("isAnd=").append(isAnd);
        if (map.size() > 0) {
            sb.append(", {");
            boolean first = true;
            for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                final List<String> list = entry.getValue();
                final String value = list.size() > 1 ? list.get(0) : list.toString();
                sb.append(entry.getKey()).append('=').append(value);
            }
            sb.append('}');
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
    public static MapFilter createFilter(
            @PluginElement("Pairs") final KeyValuePair[] pairs,
            @PluginAttribute("operator") final String oper,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch) {
        if (pairs == null || pairs.length == 0) {
            LOGGER.error("keys and values must be specified for the MapFilter");
            return null;
        }
        final Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (final KeyValuePair pair : pairs) {
            final String key = pair.getKey();
            if (key == null) {
                LOGGER.error("A null key is not valid in MapFilter");
                continue;
            }
            final String value = pair.getValue();
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
        if (map.isEmpty()) {
            LOGGER.error("MapFilter is not configured with any valid key value pairs");
            return null;
        }
        final boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        return new MapFilter(map, isAnd, match, mismatch);
    }
}
