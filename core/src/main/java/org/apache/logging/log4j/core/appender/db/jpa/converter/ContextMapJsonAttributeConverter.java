package org.apache.logging.log4j.core.appender.db.jpa.converter;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;
import java.io.IOException;
import java.util.Map;

/**
 * A JPA 2.1 attribute converter for {@link Map Map&lt;String, String&gt;}s in
 * {@link org.apache.logging.log4j.core.LogEvent}s. This converter is capable of converting both to and from
 * {@link String}s.
 *
 * In addition to other optional dependencies required by the JPA appender, this converter requires the Jackson Mapper.
 */
public class ContextMapJsonAttributeConverter implements AttributeConverter<Map<String, String>, String> {
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(final Map<String, String> contextMap) {
        try {
            return OBJECT_MAPPER.writeValueAsString(contextMap);
        } catch (IOException e) {
            throw new PersistenceException("Failed to convert map to JSON string.", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(final String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(s, new TypeReference<Map<String, String>>() { });
        } catch (IOException e) {
            throw new PersistenceException("Failed to convert JSON string to map.", e);
        }
    }
}
