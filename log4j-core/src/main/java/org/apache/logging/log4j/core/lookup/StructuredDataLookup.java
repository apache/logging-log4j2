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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.message.StructuredDataMessage;

/**
 * Looks up keys from {@link org.apache.logging.log4j.message.StructuredDataMessage} log messages.
 */
@Plugin(name = "sd", category = StrLookup.CATEGORY)
public class StructuredDataLookup implements StrLookup {

    /**
     * Key to obtain the id of a structured message.
     */
    public static final String ID_KEY = "id";

    /**
     * Key to obtain the type of a structured message.
     */
    public static final String TYPE_KEY = "type";

    /**
     * Returns {@code null}. This Lookup plugin does not make sense outside the context of a LogEvent.
     * @param key  The key to be looked up, may be null.
     * @return {@code null}
     */
    @Override
    public String lookup(final String key) {
        return null;
    }

    /**
     * Looks up the value for the key using the data in the LogEvent.
     * @param event The current LogEvent.
     * @param key  The key to be looked up, may be null.
     * @return The value associated with the key.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        if (event == null || !(event.getMessage() instanceof StructuredDataMessage)) {
            return null;
        }
        final StructuredDataMessage msg = (StructuredDataMessage) event.getMessage();
        if (ID_KEY.equalsIgnoreCase(key)) {
            return msg.getId().getName();
        } else if (TYPE_KEY.equalsIgnoreCase(key)) {
            return msg.getType();
        }
        return msg.get(key);
    }
}
