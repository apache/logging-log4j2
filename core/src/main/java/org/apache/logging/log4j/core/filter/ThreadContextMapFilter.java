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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.KeyValuePair;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Filter based on a value in the Thread Context Map (MDC).
 */
@Plugin(name = "ThreadContextMapFilter", type = "Core", elementType = "filter", printObject = true)
public class ThreadContextMapFilter extends MapFilter {

    private final String key;
    private final String value;

    private final boolean useMap;

    public ThreadContextMapFilter(final Map<String, List<String>> pairs, final boolean oper, final Result onMatch, final Result onMismatch) {
        super(pairs, oper, onMatch, onMismatch);
        if (pairs.size() == 1) {
            final Iterator<Map.Entry<String, List<String>>> iter = pairs.entrySet().iterator();
            final Map.Entry<String, List<String>> entry = iter.next();
            if (entry.getValue().size() == 1) {
                this.key = entry.getKey();
                this.value = entry.getValue().get(0);
                this.useMap = false;
            } else {
                this.key = null;
                this.value = null;
                this.useMap = true;
            }
        } else {
            this.key = null;
            this.value = null;
            this.useMap = true;
        }
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        return filter();
    }

    private Result filter() {
        boolean match = false;
        if (useMap) {
            for (final Map.Entry<String, List<String>> entry : getMap().entrySet()) {
                final String toMatch = ThreadContext.get(entry.getKey());
                if (toMatch != null) {
                    match = entry.getValue().contains(toMatch);
                } else {
                    match = false;
                }
                if ((!isAnd() && match) || (isAnd() && !match)) {
                    break;
                }
            }
        } else {
            match = value.equals(ThreadContext.get(key));
        }
        return match ? onMatch : onMismatch;
    }

    @Override
    public Result filter(final LogEvent event) {
        return super.filter(event.getContextMap()) ? onMatch : onMismatch;
    }

    @PluginFactory
    public static ThreadContextMapFilter createFilter(@PluginElement("pairs") final KeyValuePair[] pairs,
                                                      @PluginAttr("operator") final String oper,
                                                      @PluginAttr("onmatch") final String match,
                                                      @PluginAttr("onmismatch") final String mismatch) {
        if (pairs == null || pairs.length == 0) {
            LOGGER.error("key and value pairs must be specified for the ThreadContextMapFilter");
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
        if (map.size() == 0) {
            LOGGER.error("ThreadContextMapFilter is not configured with any valid key value pairs");
            return null;
        }
        final boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        final Result onMatch = Result.toResult(match);
        final Result onMismatch = Result.toResult(mismatch);
        return new ThreadContextMapFilter(map, isAnd, onMatch, onMismatch);
    }
}
