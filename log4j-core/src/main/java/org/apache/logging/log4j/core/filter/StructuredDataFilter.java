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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;

/**
 * Filter based on data in a StructuredDataMessage.
 */
@Plugin(name = "StructuredDataFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public final class StructuredDataFilter extends MapFilter {

    private static final long serialVersionUID = 1L;

    private StructuredDataFilter(final Map<String, List<String>> map, final boolean oper, final Result onMatch,
                                 final Result onMismatch) {
        super(map, oper, onMatch, onMismatch);
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        if (msg instanceof StructuredDataMessage) {
            return filter((StructuredDataMessage) msg);
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(final LogEvent event) {
        final Message msg = event.getMessage();
        if (msg instanceof StructuredDataMessage) {
            return filter((StructuredDataMessage) msg);
        }
        return super.filter(event);
    }

    protected Result filter(final StructuredDataMessage message) {
        boolean match = false;
        for (final Map.Entry<String, List<String>> entry : getMap().entrySet()) {
            final String toMatch = getValue(message, entry.getKey());
            if (toMatch != null) {
                match = entry.getValue().contains(toMatch);
            } else {
                match = false;
            }
            if ((!isAnd() && match) || (isAnd() && !match)) {
                break;
            }
        }
        return match ? onMatch : onMismatch;
    }

    private String getValue(final StructuredDataMessage data, final String key) {
        if (key.equalsIgnoreCase("id")) {
            return data.getId().toString();
        } else if (key.equalsIgnoreCase("id.name")) {
            return data.getId().getName();
        } else if (key.equalsIgnoreCase("type")) {
            return data.getType();
        } else if (key.equalsIgnoreCase("message")) {
            return data.getFormattedMessage();
        } else {
            return data.getData().get(key);
        }
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
    public static StructuredDataFilter createFilter(
            @PluginElement("Pairs") final KeyValuePair[] pairs,
            @PluginAttribute("operator") final String oper,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch) {
        if (pairs == null || pairs.length == 0) {
            LOGGER.error("keys and values must be specified for the StructuredDataFilter");
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
            LOGGER.error("StructuredDataFilter is not configured with any valid key value pairs");
            return null;
        }
        final boolean isAnd = oper == null || !oper.equalsIgnoreCase("or");
        return new StructuredDataFilter(map, isAnd, match, mismatch);
    }
}
