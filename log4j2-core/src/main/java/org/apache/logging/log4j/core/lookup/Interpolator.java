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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.internal.StatusLogger;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Interpolator implements StrLookup {

    protected final static Logger logger = StatusLogger.getLogger();

    /** Constant for the prefix separator. */
    private static final char PREFIX_SEPARATOR = ':';

    private final Map<String, StrLookup> lookups = new HashMap<String, StrLookup>();

    private final StrLookup defaultLookup;

    public Interpolator(StrLookup defaultLookup) {
        this.defaultLookup = defaultLookup;
        PluginManager manager = new PluginManager("Lookup");
        manager.collectPlugins();
        Map<String, PluginType> plugins = manager.getPlugins();
        Map<String, StrLookup> lookups = new HashMap<String, StrLookup>();

        for (Map.Entry<String, PluginType> entry : plugins.entrySet()) {
            Class<StrLookup> clazz = entry.getValue().getPluginClass();
            try {
                lookups.put(entry.getKey(), clazz.newInstance());
            } catch (Exception ex) {
                logger.error("Unable to create Lookup for " + entry.getKey(), ex);
            }
        }
    }

     /**
     * Resolves the specified variable. This implementation will try to extract
     * a variable prefix from the given variable name (the first colon (':') is
     * used as prefix separator). It then passes the name of the variable with
     * the prefix stripped to the lookup object registered for this prefix. If
     * no prefix can be found or if the associated lookup object cannot resolve
     * this variable, the default lookup object will be used.
     *
     * @param var the name of the variable whose value is to be looked up
     * @return the value of this variable or <b>null</b> if it cannot be
     * resolved
     */
    public String lookup(String var)
    {
        if (var == null)
        {
            return null;
        }

        int prefixPos = var.indexOf(PREFIX_SEPARATOR);
        if (prefixPos >= 0)
        {
            String prefix = var.substring(0, prefixPos);
            String name = var.substring(prefixPos + 1);
            StrLookup lookup = lookups.get(prefix);
            String value = null;
            if (lookup != null) {
                value = lookup.lookup(name);
            }

            if (value != null)
            {
                return value;
            }
            var = var.substring(prefixPos);
        }
        if (defaultLookup != null) {
            return defaultLookup.lookup(var);
        }
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String name : lookups.keySet()) {
            if (sb.length() == 0) {
                sb.append("{");
            } else {
                sb.append(", ");
            }

            sb.append(name);
        }
        if (sb.length() > 0) {
            sb.append("}");
        }
        return sb.toString();
    }
}
