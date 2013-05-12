package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.apache.logging.log4j.ThreadContext;
import org.codehaus.jackson.type.TypeReference;

import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;
import java.io.IOException;
import java.util.List;

/**
 * A JPA 2.1 attribute converter for {@link ThreadContext.ContextStack}s in
 * {@link org.apache.logging.log4j.core.LogEvent}s. This converter is capable of converting both to and from
 * {@link String}s.
 *
 * In addition to other optional dependencies required by the JPA appender, this converter requires the Jackson Mapper.
 */
public class ContextStackJsonAttributeConverter implements AttributeConverter<ThreadContext.ContextStack, String> {
    @Override
    public String convertToDatabaseColumn(final ThreadContext.ContextStack contextStack) {
        try {
            return ContextMapJsonAttributeConverter.OBJECT_MAPPER.writeValueAsString(contextStack.asList());
        } catch (IOException e) {
            throw new PersistenceException("Failed to convert stack list to JSON string.", e);
        }
    }

    @Override
    public ThreadContext.ContextStack convertToEntityAttribute(final String s) {
        if (s == null || s.length() == 0) {
            return null;
        }

        List<String> list;
        try {
            list = ContextMapJsonAttributeConverter.OBJECT_MAPPER.readValue(s, new TypeReference<List<String>>(){ });
        } catch (IOException e) {
            throw new PersistenceException("Failed to convert JSON string to list for stack.", e);
        }

        return new ThreadContext.ImmutableStack(list);
    }
}
