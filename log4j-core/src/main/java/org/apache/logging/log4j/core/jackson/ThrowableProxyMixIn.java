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
package org.apache.logging.log4j.core.jackson;

import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Mix-in for {@link ThrowableProxy}.
 */
abstract class ThrowableProxyMixIn {

    @JsonProperty(JSONConstants.ELT_CAUSE)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_CAUSE)
    private ThrowableProxyMixIn causeProxy;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private int commonElementCount;

    @JsonProperty(JSONConstants.ELT_EXTENDED_STACK_TRACE)
    @JacksonXmlElementWrapper(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_EXTENDED_STACK_TRACE)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_EXTENDED_STACK_TRACE_ITEM)
    private ExtendedStackTraceElement[] extendedStackTrace;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String localizedMessage;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String message;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JsonIgnore
    private transient Throwable throwable;

    @JsonIgnore
    public abstract String getCauseStackTraceAsString();

    @JsonIgnore
    public abstract String getExtendedStackTraceAsString();

    @JsonIgnore
    public abstract StackTraceElement[] getStackTrace();

    @JsonProperty(JSONConstants.ELT_SUPPRESSED)
    @JacksonXmlElementWrapper(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_SUPPRESSED)
    @JacksonXmlProperty(namespace = XMLConstants.XML_NAMESPACE, localName = XMLConstants.ELT_SUPPRESSED_ITEM)
    public abstract ThrowableProxy[] getSuppressedProxies();

    @JsonIgnore
    public abstract String getSuppressedStackTrace();

    @JsonIgnore
    public abstract Throwable getThrowable();

}
