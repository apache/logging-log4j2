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
package org.apache.logging.log4j.core.appender.rewrite;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This policy modifies events by replacing or possibly adding keys and values to the MapMessage.
 */
@Plugin(name = "MapRewritePolicy", category = Core.CATEGORY_NAME, elementType = "rewritePolicy", printObject = true)
public final class MapRewritePolicy implements RewritePolicy {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private final Map<String, Object> map;

    private final Mode mode;

    private MapRewritePolicy(final Map<String, Object> map, final Mode mode) {
        this.map = map;
        this.mode = mode;
    }

    /**
     * Rewrite the event.
     * @param source a logging event that may be returned or
     * used to create a new logging event.
     * @return The LogEvent after rewriting.
     */
    @Override
    public LogEvent rewrite(final LogEvent source) {
        final Message msg = source.getMessage();
        if (msg == null || !(msg instanceof MapMessage)) {
            return source;
        }

        @SuppressWarnings("unchecked")
        final MapMessage<?, Object> mapMsg = (MapMessage<?, Object>) msg;
        final Map<String, Object> newMap = new HashMap<>(mapMsg.getData());
        switch (mode) {
            case Add: {
                newMap.putAll(map);
                break;
            }
            default: {
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    if (newMap.containsKey(entry.getKey())) {
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        final Message message = mapMsg.newInstance(newMap);
        return new Log4jLogEvent.Builder(source).setMessage(message).build();
    }

    /**
     * An enumeration to identify whether keys not in the MapMessage should be added or whether only existing
     * keys should be updated.
     */
    public enum Mode {

        /**
         * Keys should be added.
         */
        Add,

        /**
         * Keys should be updated.
         */
        Update
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("mode=").append(mode);
        sb.append(" {");
        boolean first = true;
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * The factory method to create the MapRewritePolicy.
     * @param mode The string representation of the Mode.
     * @param pairs key/value pairs for the new Map keys and values.
     * @return The MapRewritePolicy.
     */
    @PluginFactory
    public static MapRewritePolicy createPolicy(
            @PluginAttribute("mode") final String mode, @PluginElement("KeyValuePair") final KeyValuePair[] pairs) {
        Mode op = mode == null ? op = Mode.Add : Mode.valueOf(mode);
        if (pairs == null || pairs.length == 0) {
            LOGGER.error("keys and values must be specified for the MapRewritePolicy");
            return null;
        }
        final Map<String, Object> map = new HashMap<>();
        for (final KeyValuePair pair : pairs) {
            final String key = pair.getKey();
            if (key == null) {
                LOGGER.error("A null key is not valid in MapRewritePolicy");
                continue;
            }
            final String value = pair.getValue();
            if (value == null) {
                LOGGER.error("A null value for key " + key + " is not allowed in MapRewritePolicy");
                continue;
            }
            map.put(pair.getKey(), pair.getValue());
        }
        if (map.isEmpty()) {
            LOGGER.error("MapRewritePolicy is not configured with any valid key value pairs");
            return null;
        }
        return new MapRewritePolicy(map, op);
    }
}
