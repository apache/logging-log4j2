package org.apache.logging.log4j.jackson.xml.layout;

import org.apache.logging.log4j.jackson.AbstractJacksonFactory;
import org.apache.logging.log4j.jackson.JsonConstants;
import org.apache.logging.log4j.jackson.XmlConstants;
import org.apache.logging.log4j.jackson.xml.Log4jXmlObjectMapper;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

class XmlJacksonFactory extends AbstractJacksonFactory {

    static final int DEFAULT_INDENT = 1;

    public XmlJacksonFactory(final boolean includeStacktrace, final boolean stacktraceAsString) {
        super(includeStacktrace, stacktraceAsString);
    }

    @Override
    protected String getPropertyNameForContextMap() {
        return XmlConstants.ELT_CONTEXT_MAP;
    }

    @Override
    protected String getPropertyNameForNanoTime() {
        return JsonConstants.ELT_NANO_TIME;
    }

    @Override
    protected String getPropertyNameForSource() {
        return XmlConstants.ELT_SOURCE;
    }

    @Override
    protected String getPropertyNameForStackTrace() {
        return XmlConstants.ELT_EXTENDED_STACK_TRACE;
    }

    @Override
    protected PrettyPrinter newCompactPrinter() {
        // Yes, null is the proper answer.
        return null;
    }

    @Override
    protected ObjectMapper newObjectMapper() {
        return new Log4jXmlObjectMapper(includeStacktrace, stacktraceAsString);
    }

    @Override
    protected PrettyPrinter newPrettyPrinter() {
        return new Log4jXmlPrettyPrinter(DEFAULT_INDENT);
    }
}