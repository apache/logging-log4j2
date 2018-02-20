package org.apache.logging.log4j.jackson.xml;

import org.apache.logging.log4j.core.impl.ExtendedClassInfo;
import org.apache.logging.log4j.jackson.ExtendedStackTraceElementMixIn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public abstract class ExtendedStackTraceElementXmlMixIn extends ExtendedStackTraceElementMixIn {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public ExtendedStackTraceElementXmlMixIn(
    // @formatter:off
            @JsonProperty(ATTR_CLASS) final String declaringClass,
            @JsonProperty(ATTR_METHOD) final String methodName,
            @JsonProperty(ATTR_FILE) final String fileName,
            @JsonProperty(ATTR_LINE) final int lineNumber,
            @JsonProperty(ATTR_EXACT) final boolean exact,
            @JsonProperty(ATTR_LOCATION) final String location,
            @JsonProperty(ATTR_VERSION) final String version
   // @formatter:on
    ) {
        super(declaringClass, methodName, fileName, lineNumber, exact, location, version);
    }

    @Override
    @JacksonXmlProperty(localName = ATTR_CLASS, isAttribute = true)
    public abstract String getClassName();

    @Override
    @JacksonXmlProperty(isAttribute = true)
    public abstract boolean getExact();

    @Override
    @JsonIgnore
    public abstract ExtendedClassInfo getExtraClassInfo();

    @Override
    @JacksonXmlProperty(localName = ATTR_FILE, isAttribute = true)
    public abstract String getFileName();

    @Override
    @JacksonXmlProperty(localName = ATTR_LINE, isAttribute = true)
    public abstract int getLineNumber();

    @Override
    @JacksonXmlProperty(isAttribute = true)
    public abstract String getLocation();

    @Override
    @JacksonXmlProperty(localName = ATTR_METHOD, isAttribute = true)
    public abstract String getMethodName();

    @JsonIgnore
    abstract StackTraceElement getStackTraceElement();

    @Override
    @JacksonXmlProperty(isAttribute = true)
    public abstract String getVersion();

    @Override
    @JsonIgnore
    public abstract boolean isNativeMethod();
}
