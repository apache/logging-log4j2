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

import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * A map-based lookup for main arguments.
 *
 * See {@link #setMainArguments(String[])}.
 *
 * @since 2.4
 */
@Plugin(name = "main", category = StrLookup.CATEGORY)
public class MainMapLookup extends MapLookup {

    /**
     * A singleton used by a main method to save its arguments.
     */
    static final MapLookup MAIN_SINGLETON = new MapLookup(MapLookup.newMap(0));

    /**
     * Constructor when used directly as a plugin.
     */
    public MainMapLookup() {
        // no-init
    }

    public MainMapLookup(final Map<String, String> map) {
        super(map);
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
     * <li>{@code "main:\--file"} = {@code "path/file.txt"}</li>
     * <li>{@code "main:\-x"} = {@code "2"}</li>
     * </ul>
     *<p>Note: Many applications use leading dashes to identify command arguments. Specifying {@code "main:--file}
     * would result in the lookup failing because it would look for a variable named "main" with a default
     * value of "-file". To avoid this the ":" separating the Lookup name from the key must be followed by
     * a backslash as an escape character.</p>
     *
     * @param args
     *        An application's {@code public static main(String[])} arguments.
     */
    public static void setMainArguments(final String... args) {
        if (args == null) {
            return;
        }
        initMap(args, MainMapLookup.MAIN_SINGLETON.getMap());
    }

    @Override
    public String lookup(final LogEvent event, final String key) {
        return MAIN_SINGLETON.getMap().get(key);
    }

    @Override
    public String lookup(final String key) {
        return MAIN_SINGLETON.getMap().get(key);
    }
}
