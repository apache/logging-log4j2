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
package org.apache.logging.log4j.core.layout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.KeyValuePair;

/**
 * A LoggerFields container.
 */
@Plugin(name = "LoggerFields", category = "Core", printObject = true)
public final class LoggerFields {

    private Map<String, String> map;

    private LoggerFields(Map<String, String> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    public Map<String, String> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * Create a LoggerFields from KeyValuePairs.
     *
     * @param keyValuePairs An array of KeyValuePairs.
     * @return A LoggerFields instance containing a Map<String, String>.
     */
    @PluginFactory
    public static LoggerFields createLoggerFields(
        @PluginElement("LoggerFields") final KeyValuePair[] keyValuePairs) {
        final Map<String, String> map =
            new HashMap<String, String>();

        for (final KeyValuePair keyValuePair : keyValuePairs) {
            map.put(keyValuePair.getKey(), keyValuePair.getValue());
        }

        return new LoggerFields(map);
    }
}
