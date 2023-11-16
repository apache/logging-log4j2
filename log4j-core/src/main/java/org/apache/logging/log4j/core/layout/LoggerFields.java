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
package org.apache.logging.log4j.core.layout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.StructuredDataId;

/**
 * A LoggerFields container.
 */
@Plugin(name = "LoggerFields", category = Node.CATEGORY, printObject = true)
public final class LoggerFields {

    private final Map<String, String> map;
    private final String sdId;
    private final String enterpriseId;
    private final boolean discardIfAllFieldsAreEmpty;

    private LoggerFields(
            final Map<String, String> map,
            final String sdId,
            final String enterpriseId,
            final boolean discardIfAllFieldsAreEmpty) {
        this.sdId = sdId;
        this.enterpriseId = enterpriseId;
        this.map = Collections.unmodifiableMap(map);
        this.discardIfAllFieldsAreEmpty = discardIfAllFieldsAreEmpty;
    }

    public Map<String, String> getMap() {
        return this.map;
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

    /**
     * Create a LoggerFields from KeyValuePairs.
     *
     * @param keyValuePairs
     *            An array of KeyValuePairs.
     * @param sdId
     *            The SD-ID in an SD-ELEMENT
     * @param enterpriseId
     *            The IANA assigned enterprise number
     * @param discardIfAllFieldsAreEmpty
     *            this SD-ELEMENT should be discarded if all fields are empty
     * @return A LoggerFields instance containing a Map&lt;String, String&gt;.
     */
    @PluginFactory
    public static LoggerFields createLoggerFields(
            @PluginElement("LoggerFields") final KeyValuePair[] keyValuePairs,
            @PluginAttribute("sdId") final String sdId,
            @PluginAttribute("enterpriseId") final String enterpriseId,
            @PluginAttribute(value = "discardIfAllFieldsAreEmpty") final boolean discardIfAllFieldsAreEmpty) {
        final Map<String, String> map = new HashMap<>();

        for (final KeyValuePair keyValuePair : keyValuePairs) {
            map.put(keyValuePair.getKey(), keyValuePair.getValue());
        }

        return new LoggerFields(map, sdId, enterpriseId, discardIfAllFieldsAreEmpty);
    }

    public StructuredDataId getSdId() {
        if (enterpriseId == null || sdId == null) {
            return null;
        }
        return new StructuredDataId(sdId, enterpriseId, null, null);
    }

    public boolean getDiscardIfAllFieldsAreEmpty() {
        return discardIfAllFieldsAreEmpty;
    }
}
