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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;

/**
 * {@link MapMessage} field resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config      = key , [ stringified ]
 * key         = "key" -> string
 * stringified = "stringified" -> boolean
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the <tt>userRole</tt> field of the message:
 *
 * <pre>
 * {
 *   "$resolver": "map",
 *   "key": "userRole"
 * }
 * </pre>
 */
final class MapResolver implements EventResolver {

    private final String key;

    private final boolean stringified;

    static String getName() {
        return "map";
    }

    MapResolver(final TemplateResolverConfig config) {
        this.key = config.getString("key");
        this.stringified = config.getBoolean("stringified", false);
        if (key == null) {
            throw new IllegalArgumentException("missing key: " + config);
        }
    }

    @Override
    public boolean isResolvable(final LogEvent logEvent) {
        return logEvent.getMessage() instanceof MapMessage;
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        final Message message = logEvent.getMessage();
        if (!(message instanceof MapMessage)) {
            jsonWriter.writeNull();
        } else {
            @SuppressWarnings("unchecked")
            MapMessage<?, Object> mapMessage = (MapMessage<?, Object>) message;
            final IndexedReadOnlyStringMap map = mapMessage.getIndexedReadOnlyStringMap();
            final Object value = map.getValue(key);
            if (stringified) {
                final String stringifiedValue = String.valueOf(value);
                jsonWriter.writeString(stringifiedValue);
            } else {
                jsonWriter.writeValue(value);
            }
        }
    }

}
