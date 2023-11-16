/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

/**
 * Mix-in for {@link ThrowableProxy}.
 */
abstract class ThrowableProxyWithoutStacktraceMixIn {

    @JsonProperty(JsonConstants.ELT_CAUSE)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CAUSE)
    private ThrowableProxyWithoutStacktraceMixIn causeProxy;

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    private int commonElementCount;

    @JsonIgnore
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

    @JsonProperty(JsonConstants.ELT_SUPPRESSED)
    @JacksonXmlElementWrapper(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_SUPPRESSED)
    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_SUPPRESSED_ITEM)
    public abstract ThrowableProxy[] getSuppressedProxies();

    @JsonIgnore
    public abstract String getSuppressedStackTrace();

    @JsonIgnore
    public abstract Throwable getThrowable();
}
