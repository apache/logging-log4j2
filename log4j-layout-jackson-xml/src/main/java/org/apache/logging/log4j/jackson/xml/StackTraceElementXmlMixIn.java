package org.apache.logging.log4j.jackson.xml;

import org.apache.logging.log4j.jackson.StackTraceElementConstants;
import org.apache.logging.log4j.jackson.StackTraceElementMixIn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

abstract class StackTraceElementXmlMixIn extends StackTraceElementMixIn {

    @JsonCreator
    protected StackTraceElementXmlMixIn(
    // @formatter:off
            @JsonProperty(StackTraceElementConstants.ATTR_CLASS) final String declaringClass,
            @JsonProperty(StackTraceElementConstants.ATTR_METHOD) final String methodName,
            @JsonProperty(StackTraceElementConstants.ATTR_FILE) final String fileName,
            @JsonProperty(StackTraceElementConstants.ATTR_LINE) final int lineNumber)
    // @formatter:on
    {
        super(declaringClass, methodName, fileName, lineNumber);
    }

    @Override
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_CLASS, isAttribute = true)
    protected abstract String getClassName();

    @Override
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_FILE, isAttribute = true)
    protected abstract String getFileName();

    @Override
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_LINE, isAttribute = true)
    protected abstract int getLineNumber();

    @Override
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_METHOD, isAttribute = true)
    protected abstract String getMethodName();

}
