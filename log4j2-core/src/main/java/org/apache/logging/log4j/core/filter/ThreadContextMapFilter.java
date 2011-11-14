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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
@Plugin(name="ThreadContextMapFilter", type="Core", elementType="filter", printObject = true)
public class ThreadContextMapFilter extends FilterBase {
    private final Map<String, String> map;

    private final String key;
    private final String value;

    private final boolean isAnd;

    private final boolean useMap;

    public ThreadContextMapFilter(Map<String, String> pairs, boolean oper, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        if (pairs.size() == 1) {
            Iterator<Map.Entry<String, String>> iter = pairs.entrySet().iterator();
            Map.Entry<String, String> entry = iter.next();
            this.key = entry.getKey();
            this.value= entry.getValue();
            this.map = null;
            this.isAnd = false;
            this.useMap = false;
        } else {
            this.map = pairs;
            this.isAnd = oper;
            this.key = null;
            this.value = null;
            this.useMap = true;
        }
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object[] params) {
        return filter();
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter();
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter();
    }

    private Result filter() {
        boolean match = false;
        if (useMap) {
            for (String key : map.keySet()) {
                match = map.get(key).equals(ThreadContext.get(key));
                if ((!isAnd && match) || (isAnd && !match)) {
                    break;
                }
            }
        } else {
            match = key.equals(ThreadContext.get(key));
        }
        return match ? onMatch : onMismatch;
    }

    @Override
    public Result filter(LogEvent event) {
        Map<String, String> ctx = event.getContextMap();
        boolean match = false;
        for (String key : map.keySet()) {
            match = map.get(key).equals(ctx.get(key));
            if ((!isAnd && match) || (isAnd && !match)) {
                break;
            }
        }
        return match ? onMatch : onMismatch;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("isAnd=").append(isAnd);
        if (useMap) {
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
        } else {
            sb.append(", {").append(key).append("=").append(value).append("}");
        }
        return sb.toString();
    }

    @PluginFactory
    public static ThreadContextMapFilter createFilter(@PluginElement("pairs") KeyValuePair[] pairs,
                                                      @PluginAttr("operator") String oper,
                                                      @PluginAttr("onmatch") String match,
                                                      @PluginAttr("onmismatch") String mismatch) {
        if (pairs == null || pairs.length == 0) {
            logger.error("key and value pairs must be specified for the ThreadContextMapFilter");
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
        for (KeyValuePair pair : pairs) {
            String key = pair.getKey();
            if (key == null) {
                logger.error("A null key is not valid in ThreadContextMapFilter");
                continue;
            }
            String value = pair.getValue();
            if (value == null) {
                logger.error("A null value for key " + key + " is not allowed in ThreadContextMapFilter");
                continue;
            }
            map.put(pair.getKey(), pair.getValue());
        }
        if (map.size() == 0) {
            logger.error("ThreadContextMapFilter is not configured with any valid key value pairs");
            return null;
        }
        boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        Result onMatch = match == null ? null : Result.valueOf(match);
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch);
        return new ThreadContextMapFilter(map, isAnd, onMatch, onMismatch);
    }
}
