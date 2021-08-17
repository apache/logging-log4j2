package org.apache.logging.log4j.jackson;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractEnhancedJacksonFactory {

    public AbstractEnhancedJacksonFactory() {
    }

    protected abstract ObjectMapper newObjectMapper();

    protected abstract PrettyPrinter newCompactPrinter();

    protected abstract PrettyPrinter newPrettyPrinter();

    public ObjectWriter newWriter(boolean compact) {
        SimpleFilterProvider filters = new SimpleFilterProvider();
        Set<String> except = new HashSet(3);
        filters.addFilter(Log4jLogEvent.class.getName(), SimpleBeanPropertyFilter.serializeAllExcept(except));
        ObjectWriter writer = this.newObjectMapper().writer(compact ? this.newCompactPrinter() : this.newPrettyPrinter());
        return writer.with(filters);
    }
}

