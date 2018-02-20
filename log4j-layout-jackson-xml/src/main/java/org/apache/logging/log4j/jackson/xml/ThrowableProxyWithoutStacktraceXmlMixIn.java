package org.apache.logging.log4j.jackson.xml;

import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.jackson.ThrowableProxyWithoutStacktraceMixIn;
import org.apache.logging.log4j.jackson.XmlConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public abstract class ThrowableProxyWithoutStacktraceXmlMixIn extends ThrowableProxyWithoutStacktraceMixIn {

    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CAUSE)
    private ThrowableProxyWithoutStacktraceMixIn causeProxy;

    @JacksonXmlProperty(isAttribute = true)
    private int commonElementCount;

    @JsonIgnore
    private ExtendedStackTraceElement[] extendedStackTrace;

    @JacksonXmlProperty(isAttribute = true)
    private String localizedMessage;

    @JacksonXmlProperty(isAttribute = true)
    private String message;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JsonIgnore
    private transient Throwable throwable;

    @Override
    @JsonIgnore
    public abstract String getCauseStackTraceAsString();

    @Override
    @JsonIgnore
    public abstract String getExtendedStackTraceAsString();

    @Override
    @JsonIgnore
    public abstract StackTraceElement[] getStackTrace();

    @Override
    @JacksonXmlElementWrapper(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_SUPPRESSED)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_SUPPRESSED_ITEM)
    public abstract ThrowableProxy[] getSuppressedProxies();

    @Override
    @JsonIgnore
    public abstract String getSuppressedStackTrace();

    @Override
    @JsonIgnore
    public abstract Throwable getThrowable();

}
