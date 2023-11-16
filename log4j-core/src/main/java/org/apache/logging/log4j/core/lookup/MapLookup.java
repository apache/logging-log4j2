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
package org.apache.logging.log4j.core.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.util.Strings;

/**
 * A map-based lookup.
 */
@Plugin(name = "map", category = StrLookup.CATEGORY)
public class MapLookup implements StrLookup {

    /**
     * Map keys are variable names and value.
     */
    private final Map<String, String> map;

    /**
     * Constructor when used directly as a plugin.
     */
    public MapLookup() {
        this.map = null;
    }

    /**
     * Creates a new instance backed by a Map.
     *
     * @param map
     *        the map of keys to values, may be null
     */
    public MapLookup(final Map<String, String> map) {
        this.map = map;
    }

    static Map<String, String> initMap(final String[] srcArgs, final Map<String, String> destMap) {
        for (int i = 0; i < srcArgs.length; i++) {
            final int next = i + 1;
            final String value = srcArgs[i];
            destMap.put(Integer.toString(i), value);
            destMap.put(value, next < srcArgs.length ? srcArgs[next] : null);
        }
        return destMap;
    }

    static HashMap<String, String> newMap(final int initialCapacity) {
        return new HashMap<>(initialCapacity);
    }

    /**
     * An application's {@code public static main(String[])} method calls this method to make its main arguments
     * available for lookup with the prefix {@code main}.
     * <p>
     * The map provides two kinds of access: First by index, starting at {@code "0"}, {@code "1"} and so on. For
     * example, the command line {@code --file path/file.txt -x 2} can be accessed from a configuration file with:
     * </p>
     * <ul>
     * <li>{@code "main:0"} = {@code "--file"}</li>
     * <li>{@code "main:1"} = {@code "path/file.txt"}</li>
     * <li>{@code "main:2"} = {@code "-x"}</li>
     * <li>{@code "main:3"} = {@code "2"}</li>
     * </ul>
     * <p>
     * Second using the argument at position n as the key to access the value at n+1.
     * </p>
     * <ul>
     * <li>{@code "main:--file"} = {@code "path/file.txt"}</li>
     * <li>{@code "main:-x"} = {@code "2"}</li>
     * </ul>
     *
     * @param args
     *        An application's {@code public static main(String[])} arguments.
     * @since 2.1
     * @deprecated As of 2.4, use {@link MainMapLookup#setMainArguments(String[])}
     */
    @Deprecated
    public static void setMainArguments(final String... args) {
        MainMapLookup.setMainArguments(args);
    }

    static Map<String, String> toMap(final List<String> args) {
        if (args == null) {
            return null;
        }
        final int size = args.size();
        return initMap(args.toArray(Strings.EMPTY_ARRAY), newMap(size));
    }

    static Map<String, String> toMap(final String[] args) {
        if (args == null) {
            return null;
        }
        return initMap(args, newMap(args.length));
    }

    protected Map<String, String> getMap() {
        return map;
    }

    @Override
    public String lookup(final LogEvent event, final String key) {
        final boolean isMapMessage = event != null && event.getMessage() instanceof MapMessage;
        if (isMapMessage) {
            final String obj = ((MapMessage) event.getMessage()).get(key);
            if (obj != null) {
                return obj;
            }
        }
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    /**
     * Looks up a String key to a String value using the map.
     * <p>
     * If the map is null, then null is returned. The map result object is converted to a string using toString().
     * </p>
     *
     * @param key
     *        the key to be looked up, may be null
     * @return the matching value, null if no match
     */
    @Override
    public String lookup(final String key) {
        if (key == null || map == null) {
            return null;
        }
        return map.get(key);
    }
}
