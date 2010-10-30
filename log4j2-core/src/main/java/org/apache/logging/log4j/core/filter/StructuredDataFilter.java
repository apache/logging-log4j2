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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Plugin(name="StructuredData", type="Core", elementType="filter")
public class StructuredDataFilter extends FilterBase {
    private final Map<String, Object> map;

    private final boolean isAnd;

    private StructuredDataFilter(Map<String, Object> map, boolean oper, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        if (map == null) {
            throw new NullPointerException("key cannot be null");
        }
        this.isAnd = oper;
        this.map = map;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (msg instanceof StructuredDataMessage) {
            return filter((StructuredDataMessage)msg);
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(LogEvent event) {
        Message msg = event.getMessage();
        if (msg instanceof StructuredDataMessage) {
            return filter((StructuredDataMessage) msg);
        }
        return Result.NEUTRAL;
    }

    private Result filter(StructuredDataMessage msg) {
        boolean match = false;
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase("id")) {
                match = map.get(key).equals(msg.getId().toString());
            } else if (key.equalsIgnoreCase("id.name")) {
                match = map.get(key).equals(msg.getId().getName());
            } else if (key.equalsIgnoreCase("type")) {
                match = map.get(key).equals(msg.getType());
            } else if (key.equalsIgnoreCase("message")) {
                match = map.get(key).equals(msg.getFormattedMessage().toString());
            } else {
                String data = msg.getData().get(key).toString();
                match = map.get(key).equals(data);
            }
            if ((!isAnd && match) || (isAnd && !match)) {
                break;
            }
        }
        return match ? onMatch : onMismatch;
    }

    @PluginFactory
    public static StructuredDataFilter createFilter(@PluginAttr("pairs") KeyValuePair[] pairs,
                                                    @PluginAttr("operator") String oper,
                                                    @PluginAttr("onmatch") String match,
                                                    @PluginAttr("onmismatch") String mismatch) {
        if (pairs == null || pairs.length == 0) {
            logger.error("keys and values must be specified for the ThreadContextMapFilter");
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (KeyValuePair pair : pairs) {
            String key = pair.getKey();
            if (key == null) {
                logger.error("A null key is not valid in StructuredDataFilter");
                continue;
            }
            String value = pair.getValue();
            if (value == null) {
                logger.error("A null value for key " + key + " is not allowed in StructuredDataFilter");
                continue;
            }
            map.put(pair.getKey(), pair.getValue());
        }
        if (map.size() == 0) {
            logger.error("StructuredDataFilter is not configured with any valid key value pairs");
            return null;
        }
        boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        Result onMatch = match == null ? null : Result.valueOf(match);
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch);
        return new StructuredDataFilter(map, isAnd, onMatch, onMismatch);
    }
}
