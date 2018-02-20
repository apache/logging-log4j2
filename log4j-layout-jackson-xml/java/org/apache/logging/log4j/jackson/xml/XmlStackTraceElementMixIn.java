package org.apache.logging.log4j.jackson.xml;

import org.apache.logging.log4j.core.jackson.StackTraceElementMixIn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

abstract class XmlStackTraceElementMixIn extends StackTraceElementMixIn {

    @JsonCreator
    protected XmlStackTraceElementMixIn(
    // @formatter:off
            @JsonProperty(ATTR_CLASS) final String declaringClass,
            @JsonProperty(ATTR_METHOD) final String methodName,
            @JsonProperty(ATTR_FILE) final String fileName,
            @JsonProperty(ATTR_LINE) final int lineNumber)
            // @formatter:on
    {
        super(declaringClass, methodName, fileName, lineNumber);
    }

    @Override
    @JacksonXmlProperty(localName = ATTR_CLASS, isAttribute = true)
    protected abstract String getClassName();

    @Override
    @JacksonXmlProperty(localName = ATTR_FILE, isAttribute = true)
    protected abstract String getFileName();

    @Override
    @JacksonXmlProperty(localName = ATTR_LINE, isAttribute = true)
    protected abstract int getLineNumber();

    @Override
    @JacksonXmlProperty(localName = ATTR_METHOD, isAttribute = true)
    protected abstract String getMethodName();

}
