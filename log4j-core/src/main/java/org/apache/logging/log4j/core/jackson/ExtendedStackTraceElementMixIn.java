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

import java.io.Serializable;

import org.apache.logging.log4j.core.impl.ExtendedClassInfo;
import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Mix-in for {@link ExtendedStackTraceElement}.
 */
@JsonPropertyOrder({ "class", "method", "file", "line", "exact", "location", "version" })
abstract class ExtendedStackTraceElementMixIn implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public ExtendedStackTraceElementMixIn(
            // @formatter:off
            @JsonProperty("class") final String declaringClass,
            @JsonProperty("method") final String methodName,
            @JsonProperty("file") final String fileName,
            @JsonProperty("line") final int lineNumber,
            @JsonProperty("exact") final boolean exact,
            @JsonProperty("location") final String location,
            @JsonProperty("version") final String version
            // @formatter:on
    ) {
        // empty
    }

    @JsonProperty("class")
    @JacksonXmlProperty(localName = "class", isAttribute = true)
    public abstract String getClassName();

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public abstract boolean getExact();

    @JsonIgnore
    public abstract ExtendedClassInfo getExtraClassInfo();

    @JsonProperty("file")
    @JacksonXmlProperty(localName = "file", isAttribute = true)
    public abstract String getFileName();

    @JsonProperty("line")
    @JacksonXmlProperty(localName = "line", isAttribute = true)
    public abstract int getLineNumber();

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public abstract String getLocation();

    @JsonProperty("method")
    @JacksonXmlProperty(localName = "method", isAttribute = true)
    public abstract String getMethodName();
    
    @JsonIgnore
    abstract StackTraceElement getStackTraceElement();

    @JsonProperty
    @JacksonXmlProperty(isAttribute = true)
    public abstract String getVersion();

    @JsonIgnore
    public abstract boolean isNativeMethod();

}
