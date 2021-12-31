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
            @JsonProperty(ATTR_CLASS_LOADER_NAME) final String classLoaderName,
            @JsonProperty(ATTR_MODULE) final String moduleName,
            @JsonProperty(ATTR_MODULE_VERSION) final String moduleVersion,
            @JsonProperty(ATTR_CLASS) final String declaringClass,
            @JsonProperty(ATTR_METHOD) final String methodName,
            @JsonProperty(ATTR_FILE) final String fileName,
            @JsonProperty(ATTR_LINE) final int lineNumber,
            @JsonProperty(ATTR_EXACT) final boolean exact,
            @JsonProperty(ATTR_LOCATION) final String location,
            @JsonProperty(ATTR_VERSION) final String version
   // @formatter:on
    ) {
        super(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber, exact,
                location, version);
    }

    @Override
    @JacksonXmlProperty(localName = ATTR_CLASS_LOADER_NAME, isAttribute = true)
    public abstract String getClassLoaderName();

    @Override
    @JacksonXmlProperty(localName = ATTR_MODULE, isAttribute = true)
    public abstract String getModuleName();

    @Override
    @JacksonXmlProperty(localName = ATTR_MODULE_VERSION, isAttribute = true)
    public abstract String getModuleVersion();

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
