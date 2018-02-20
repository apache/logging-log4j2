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

package org.apache.logging.log4j.jackson.xml;

import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.jackson.ThrowableProxyWithStacktraceAsStringMixIn;
import org.apache.logging.log4j.jackson.XmlConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public abstract class ThrowableProxyWithStacktraceAsStringXmlMixIn extends ThrowableProxyWithStacktraceAsStringMixIn {

    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CAUSE)
    private ThrowableProxyWithStacktraceAsStringMixIn causeProxy;

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
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_EXTENDED_STACK_TRACE)
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
