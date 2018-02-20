package org.apache.logging.log4j.jackson.json.layout;

import org.apache.logging.log4j.jackson.AbstractJacksonFactory;
import org.apache.logging.log4j.jackson.JsonConstants;
import org.apache.logging.log4j.jackson.json.Log4jJsonObjectMapper;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonJacksonFactory extends AbstractJacksonFactory {

    private final boolean encodeThreadContextAsList;
    private final boolean objectMessageAsJsonObject;

    public JsonJacksonFactory(final boolean encodeThreadContextAsList, final boolean includeStacktrace,
            final boolean stacktraceAsString, final boolean objectMessageAsJsonObject) {
        super(includeStacktrace, stacktraceAsString);
        this.encodeThreadContextAsList = encodeThreadContextAsList;
        this.objectMessageAsJsonObject = objectMessageAsJsonObject;
    }

    @Override
    protected String getPropertyNameForContextMap() {
        return JsonConstants.ELT_CONTEXT_MAP;
    }

    @Override
    protected String getPropertyNameForNanoTime() {
        return JsonConstants.ELT_NANO_TIME;
    }

    @Override
    protected String getPropertyNameForSource() {
        return JsonConstants.ELT_SOURCE;
    }

    @Override
    protected String getPropertyNameForStackTrace() {
        return JsonConstants.ELT_EXTENDED_STACK_TRACE;
    }

    @Override
    protected PrettyPrinter newCompactPrinter() {
        return new MinimalPrettyPrinter();
    }

    @Override
    protected ObjectMapper newObjectMapper() {
        return new Log4jJsonObjectMapper(encodeThreadContextAsList, includeStacktrace, stacktraceAsString,
                objectMessageAsJsonObject);
    }

    @Override
    protected PrettyPrinter newPrettyPrinter() {
        return new DefaultPrettyPrinter();
    }

}