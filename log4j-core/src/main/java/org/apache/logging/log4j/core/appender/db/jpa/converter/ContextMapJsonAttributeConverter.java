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
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.PersistenceException;

import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A JPA 2.1 attribute converter for {@link Map Map&lt;String, String&gt;}s in
 * {@link org.apache.logging.log4j.core.LogEvent}s. This converter is capable of converting both to and from
 * {@link String}s.
 *
 * In addition to other optional dependencies required by the JPA appender, this converter requires the Jackson Data
 * Processor.
 */
@Converter(autoApply = false)
public class ContextMapJsonAttributeConverter implements AttributeConverter<Map<String, String>, String> {
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final Map<String, String> contextMap) {
        if (contextMap == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(contextMap);
        } catch (final IOException e) {
            throw new PersistenceException("Failed to convert map to JSON string.", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(final String s) {
        if (Strings.isEmpty(s)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(s, new TypeReference<Map<String, String>>() { });
        } catch (final IOException e) {
            throw new PersistenceException("Failed to convert JSON string to map.", e);
        }
    }
}
