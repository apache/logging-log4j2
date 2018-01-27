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
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.PersistenceException;

import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A JPA 2.1 attribute converter for {@link ReadOnlyStringMap}s in
 * {@link org.apache.logging.log4j.core.LogEvent}s. This converter is capable of converting both to and from
 * {@link String}s.
 *
 * In addition to other optional dependencies required by the JPA appender, this converter requires the Jackson Data
 * Processor.
 */
@Converter(autoApply = false)
public class ContextDataJsonAttributeConverter implements AttributeConverter<ReadOnlyStringMap, String> {
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final ReadOnlyStringMap contextData) {
        if (contextData == null) {
            return null;
        }

        try {
            final JsonNodeFactory factory = OBJECT_MAPPER.getNodeFactory();
            final ObjectNode root = factory.objectNode();
            contextData.forEach(new BiConsumer<String, Object>() {
                @Override
                public void accept(final String key, final Object value) {
                    // we will cheat here and write the toString of the Object... meh, but ok.
                    root.put(key, String.valueOf(value));
                }
            });
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (final Exception e) {
            throw new PersistenceException("Failed to convert contextData to JSON string.", e);
        }
    }

    @Override
    public ReadOnlyStringMap convertToEntityAttribute(final String s) {
        if (Strings.isEmpty(s)) {
            return null;
        }
        try {
            final StringMap result = ContextDataFactory.createContextData();
            final ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(s);
            final Iterator<Map.Entry<String, JsonNode>> entries = root.fields();
            while (entries.hasNext()) {
                final Map.Entry<String, JsonNode> entry = entries.next();

                // Don't know what to do with non-text values.
                // Maybe users who need this need to provide custom converter?
                final Object value = entry.getValue().textValue();
                result.putValue(entry.getKey(), value);
            }
            return result;
        } catch (final IOException e) {
            throw new PersistenceException("Failed to convert JSON string to map.", e);
        }
    }
}
