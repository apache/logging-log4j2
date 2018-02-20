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
package org.apache.logging.log4j.jackson;

import java.io.Serializable;

import org.apache.logging.log4j.core.impl.ExtendedClassInfo;
import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Mix-in for {@link ExtendedStackTraceElement}.
 */
@JsonPropertyOrder({
    //@formatter:off
    ExtendedStackTraceElementMixIn.ATTR_CLASS,
    ExtendedStackTraceElementMixIn.ATTR_METHOD,
    ExtendedStackTraceElementMixIn.ATTR_FILE,
    ExtendedStackTraceElementMixIn.ATTR_LINE,
    ExtendedStackTraceElementMixIn.ATTR_EXACT,
    ExtendedStackTraceElementMixIn.ATTR_LOCATION,
    ExtendedStackTraceElementMixIn.ATTR_VERSION
    //@formatter:on
})
public abstract class ExtendedStackTraceElementMixIn implements Serializable {

    protected static final String ATTR_CLASS = StackTraceElementConstants.ATTR_CLASS;
    protected static final String ATTR_METHOD = StackTraceElementConstants.ATTR_METHOD;
    protected static final String ATTR_FILE = StackTraceElementConstants.ATTR_FILE;
    protected static final String ATTR_LINE = StackTraceElementConstants.ATTR_LINE;
    protected static final String ATTR_EXACT = "exact";
    protected static final String ATTR_LOCATION = "location";
    protected static final String ATTR_VERSION = "version";

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public ExtendedStackTraceElementMixIn(
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
        // empty
    }

    @JsonProperty(ATTR_CLASS)
    public abstract String getClassName();

    @JsonProperty(ATTR_EXACT)
    public abstract boolean getExact();

    @JsonIgnore
    public abstract ExtendedClassInfo getExtraClassInfo();

    @JsonProperty(ATTR_FILE)
    public abstract String getFileName();

    @JsonProperty(ATTR_LINE)
    public abstract int getLineNumber();

    @JsonProperty(ATTR_LOCATION)
    public abstract String getLocation();

    @JsonProperty(ATTR_METHOD)
    public abstract String getMethodName();

    @JsonIgnore
    abstract StackTraceElement getStackTraceElement();

    @JsonProperty(ATTR_VERSION)
    public abstract String getVersion();

    @JsonIgnore
    public abstract boolean isNativeMethod();

}
