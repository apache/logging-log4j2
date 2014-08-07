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
package org.apache.logging.log4j.core.lookup;

import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.message.MapMessage;

/**
 * The basis for a lookup based on a Map.
 */
@Plugin(name = "map", category = "Lookup")
public class MapLookup implements StrLookup {
    
    /**
     * Map keys are variable names and value.
     */
    private final Map<String, String> map;

    /**
     * Creates a new instance backed by a Map. Used by the default lookup.
     *
     * @param map the map of keys to values, may be null
     */
    public MapLookup(final Map<String, String> map) {
        this.map = map;
    }

    /**
     * Constructor when used directly as a plugin.
     */
    public MapLookup() {
        this.map = null;
    }

    /**
     * Looks up a String key to a String value using the map.
     * <p>
     * If the map is null, then null is returned.
     * The map result object is converted to a string using toString().
     * </p>
     *
     * @param key the key to be looked up, may be null
     * @return the matching value, null if no match
     */
    @Override
    public String lookup(final String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    @Override
    public String lookup(final LogEvent event, final String key) {
        if (map == null && !(event.getMessage() instanceof MapMessage)) {
            return null;
        }
        if (map != null && map.containsKey(key)) {
            final String obj = map.get(key);
            if (obj != null) {
                return obj;
            }
        }
        if (event.getMessage() instanceof MapMessage) {
            return ((MapMessage) event.getMessage()).get(key);
        }
        return null;
    }
}
