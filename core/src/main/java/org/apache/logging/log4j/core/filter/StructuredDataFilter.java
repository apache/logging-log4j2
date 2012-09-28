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
import org.apache.logging.log4j.message.StructuredDataMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Filter based on data in a StructuredDataMessage.
 */
@Plugin(name = "StructuredDataFilter", type = "Core", elementType = "filter", printObject = true)
public final class StructuredDataFilter extends MapFilter {

    private StructuredDataFilter(Map<String, String> map, boolean oper, Result onMatch, Result onMismatch) {
        super(map, oper, onMatch, onMismatch);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        if (msg instanceof StructuredDataMessage) {
            return filter((StructuredDataMessage) msg);
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

    @Override
    protected Result filter(MapMessage message) {
        if (!(message instanceof StructuredDataMessage)) {
            return super.filter(message);
        }
        StructuredDataMessage msg = (StructuredDataMessage) message;
        boolean match = false;
        Map<String, String> map = getMap();
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
            if ((!isAnd() && match) || (isAnd() && !match)) {
                break;
            }
        }
        return match ? onMatch : onMismatch;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("isAnd=").append(isAnd());
        if (getMap().size() > 0) {
            sb.append(", {");
            boolean first = true;
            for (Map.Entry<String, String> entry : getMap().entrySet()) {
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

    /**
     * Create the StructuredDataFilter.
     * @param pairs Key and value pairs.
     * @param oper The operator to perform. If not "or" the operation will be an "and".
     * @param match The action to perform on a match.
     * @param mismatch The action to perform on a mismatch.
     * @return The StructuredDataFilter.
     */
    @PluginFactory
    public static StructuredDataFilter createFilter(@PluginAttr("pairs") KeyValuePair[] pairs,
                                                    @PluginAttr("operator") String oper,
                                                    @PluginAttr("onmatch") String match,
                                                    @PluginAttr("onmismatch") String mismatch) {
        if (pairs == null || pairs.length == 0) {
            LOGGER.error("keys and values must be specified for the StructuredDataFilter");
            return null;
        }
        Map<String, String> map = new HashMap<String, String>();
        for (KeyValuePair pair : pairs) {
            String key = pair.getKey();
            if (key == null) {
                LOGGER.error("A null key is not valid in StructuredDataFilter");
                continue;
            }
            String value = pair.getValue();
            if (value == null) {
                LOGGER.error("A null value for key " + key + " is not allowed in StructuredDataFilter");
                continue;
            }
            map.put(pair.getKey(), pair.getValue());
        }
        if (map.size() == 0) {
            LOGGER.error("StructuredDataFilter is not configured with any valid key value pairs");
            return null;
        }
        boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        Result onMatch = match == null ? null : Result.valueOf(match.toUpperCase());
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch.toUpperCase());
        return new StructuredDataFilter(map, isAnd, onMatch, onMismatch);
    }
}
