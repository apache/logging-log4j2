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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.message.MapMessage;

/**
 * A map-based lookup.
 */
@Plugin(name = "map", category = StrLookup.CATEGORY)
public class MapLookup implements StrLookup {

    /**
     * A singleton used by a main method to save its arguments.
     */
    static final MapLookup MAIN_SINGLETON = new MapLookup(newMap(0));

    static Map<String, String> initMap(final String[] srcArgs, final Map<String, String> destMap) {
        for (int i = 0; i < srcArgs.length; i++) {
            final int next = i + 1;
            final String value = srcArgs[i];
            destMap.put(Integer.toString(i), value);
            destMap.put(value, next < srcArgs.length ? srcArgs[next] : null);
        }
        return destMap;
    }

    private static HashMap<String, String> newMap(final int initialCapacity) {
        return new HashMap<String, String>(initialCapacity);
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
     */
    public static void setMainArguments(final String[] args) {
        if (args == null) {
            return;
        }
        initMap(args, MAIN_SINGLETON.map);
    }

    static Map<String, String> toMap(final List<String> args) {
        if (args == null) {
            return null;
        }
        final int size = args.size();
        return initMap(args.toArray(new String[size]), newMap(size));
    }

    static Map<String, String> toMap(final String[] args) {
        if (args == null) {
            return null;
        }
        return initMap(args, newMap(args.length));
    }

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
     * Creates a new instance backed by a Map. Used by the default lookup.
     *
     * @param map
     *        the map of keys to values, may be null
     */
    public MapLookup(final Map<String, String> map) {
        this.map = map;
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
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

}
