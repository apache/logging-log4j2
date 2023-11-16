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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;

/**
 * A Filter that operates on a Map.
 */
@Plugin(name = "MapFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
@PerformanceSensitive("allocation")
public class MapFilter extends AbstractFilter {

    private final IndexedStringMap map;
    private final boolean isAnd;

    protected MapFilter(
            final Map<String, List<String>> map, final boolean oper, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.isAnd = oper;
        Objects.requireNonNull(map, "map cannot be null");

        this.map = new SortedArrayStringMap(map.size());
        for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
            this.map.putValue(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        if (msg instanceof MapMessage) {
            return filter((MapMessage<?, ?>) msg) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(final LogEvent event) {
        final Message msg = event.getMessage();
        if (msg instanceof MapMessage) {
            return filter((MapMessage<?, ?>) msg) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;
    }

    protected boolean filter(final MapMessage<?, ?> mapMessage) {
        boolean match = false;
        for (int i = 0; i < map.size(); i++) {
            final String toMatch = mapMessage.get(map.getKeyAt(i));
            match = toMatch != null && ((List<String>) map.getValueAt(i)).contains(toMatch);

            if ((!isAnd && match) || (isAnd && !match)) {
                break;
            }
        }
        return match;
    }

    protected boolean filter(final Map<String, String> data) {
        boolean match = false;
        for (int i = 0; i < map.size(); i++) {
            final String toMatch = data.get(map.getKeyAt(i));
            match = toMatch != null && ((List<String>) map.getValueAt(i)).contains(toMatch);

            if ((!isAnd && match) || (isAnd && !match)) {
                break;
            }
        }
        return match;
    }

    protected boolean filter(final ReadOnlyStringMap data) {
        boolean match = false;
        for (int i = 0; i < map.size(); i++) {
            final String toMatch = data.getValue(map.getKeyAt(i));
            match = toMatch != null && ((List<String>) map.getValueAt(i)).contains(toMatch);

            if ((!isAnd && match) || (isAnd && !match)) {
                break;
            }
        }
        return match;
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1) {
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
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
        return Result.NEUTRAL;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("isAnd=").append(isAnd);
        if (map.size() > 0) {
            sb.append(", {");
            for (int i = 0; i < map.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                final List<String> list = map.getValueAt(i);
                final String value = list.size() > 1 ? list.get(0) : list.toString();
                sb.append(map.getKeyAt(i)).append('=').append(value);
            }
            sb.append('}');
        }
        return sb.toString();
    }

    protected boolean isAnd() {
        return isAnd;
    }

    /** @deprecated  use {@link #getStringMap()} instead */
    @Deprecated
    protected Map<String, List<String>> getMap() {
        final Map<String, List<String>> result = new HashMap<>(map.size());
        map.forEach((key, value) -> result.put(key, (List<String>) value));
        return result;
    }

    /**
     * Returns the IndexedStringMap with {@code List<String>} values that this MapFilter was constructed with.
     * @return the IndexedStringMap with {@code List<String>} values to match against
     * @since 2.8
     */
    protected IndexedReadOnlyStringMap getStringMap() {
        return map;
    }

    // TODO Consider refactoring to use AbstractFilter.AbstractFilterBuilder
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
        final Map<String, List<String>> map = new HashMap<>();
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
                list = new ArrayList<>();
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
