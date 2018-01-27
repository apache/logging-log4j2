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
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.PersistenceException;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * A JPA 2.1 attribute converter for
 * {@link org.apache.logging.log4j.ThreadContext.ContextStack ThreadContext.ContextStack}s in
 * {@link org.apache.logging.log4j.core.LogEvent}s. This converter is capable of converting both to and from
 * {@link String}s.
 *
 * In addition to other optional dependencies required by the JPA appender, this converter requires the Jackson Data
 * Processor.
 */
@Converter(autoApply = false)
public class ContextStackJsonAttributeConverter implements AttributeConverter<ThreadContext.ContextStack, String> {
    @Override
    public String convertToDatabaseColumn(final ThreadContext.ContextStack contextStack) {
        if (contextStack == null) {
            return null;
        }

        try {
            return ContextMapJsonAttributeConverter.OBJECT_MAPPER.writeValueAsString(contextStack.asList());
        } catch (final IOException e) {
            throw new PersistenceException("Failed to convert stack list to JSON string.", e);
        }
    }

    @Override
    public ThreadContext.ContextStack convertToEntityAttribute(final String s) {
        if (Strings.isEmpty(s)) {
            return null;
        }

        List<String> list;
        try {
            list = ContextMapJsonAttributeConverter.OBJECT_MAPPER.readValue(s, new TypeReference<List<String>>() { });
        } catch (final IOException e) {
            throw new PersistenceException("Failed to convert JSON string to list for stack.", e);
        }

        final DefaultThreadContextStack result = new DefaultThreadContextStack(true);
        result.addAll(list);
        return result;
    }
}
