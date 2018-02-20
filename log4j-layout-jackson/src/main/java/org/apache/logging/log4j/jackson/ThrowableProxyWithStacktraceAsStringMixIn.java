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

import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mix-in for {@link org.apache.logging.log4j.core.impl.ThrowableProxy}.
 */
public abstract class ThrowableProxyWithStacktraceAsStringMixIn {

    @JsonProperty(JsonConstants.ELT_CAUSE)
    private ThrowableProxyWithStacktraceAsStringMixIn causeProxy;

    @JsonProperty
    private int commonElementCount;

    @JsonIgnore
    private ExtendedStackTraceElement[] extendedStackTrace;

    @JsonProperty
    private String localizedMessage;

    @JsonProperty
    private String message;

    @JsonProperty
    private String name;

    @JsonIgnore
    private transient Throwable throwable;

    @JsonIgnore
    public abstract String getCauseStackTraceAsString();

    @JsonProperty(JsonConstants.ELT_EXTENDED_STACK_TRACE)
    public abstract String getExtendedStackTraceAsString();

    @JsonIgnore
    public abstract StackTraceElement[] getStackTrace();

    @JsonProperty(JsonConstants.ELT_SUPPRESSED)
    public abstract ThrowableProxy[] getSuppressedProxies();

    @JsonIgnore
    public abstract String getSuppressedStackTrace();

    @JsonIgnore
    public abstract Throwable getThrowable();

}
