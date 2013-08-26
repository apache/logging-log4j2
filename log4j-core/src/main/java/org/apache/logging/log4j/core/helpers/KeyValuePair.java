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
package org.apache.logging.log4j.core.helpers;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Key/Value pair configuration item.
 */
@Plugin(name = "KeyValuePair", category = "Core", printObject = true)
public class KeyValuePair {

    private final String key;
    private final String value;

    /**
     * Constructs a key/value pair. The constructor should only be called from test classes.
     * @param key The key.
     * @param value The value.
     */
    public KeyValuePair(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the key.
     * @return the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value.
     * @return The value.
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    /**
     * Create a Key/Value pair.
     * @param key The key.
     * @param value The value.
     * @return A KeyValuePair.
     */
    @PluginFactory
    public static KeyValuePair createPair(
            @PluginAttribute("key") final String key,
            @PluginAttribute("value") final  String value) {

        return new KeyValuePair(key, value);
    }
}
