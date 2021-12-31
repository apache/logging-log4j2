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

import org.apache.logging.log4j.jackson.StackTraceElementConstants;
import org.apache.logging.log4j.jackson.StackTraceElementMixIn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

abstract class StackTraceElementXmlMixIn extends StackTraceElementMixIn {

    @JsonCreator
    protected StackTraceElementXmlMixIn(
    // @formatter:off
            @JsonProperty(StackTraceElementConstants.ATTR_CLASS_LOADER_NAME) final String classLoaderName,
            @JsonProperty(StackTraceElementConstants.ATTR_MODULE) final String moduleName,
            @JsonProperty(StackTraceElementConstants.ATTR_MODULE_VERSION) final String moduleVersion,
            @JsonProperty(StackTraceElementConstants.ATTR_CLASS) final String declaringClass,
            @JsonProperty(StackTraceElementConstants.ATTR_METHOD) final String methodName,
            @JsonProperty(StackTraceElementConstants.ATTR_FILE) final String fileName,
            @JsonProperty(StackTraceElementConstants.ATTR_LINE) final int lineNumber)
    // @formatter:on
    {
        super(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber);
    }

    @Override
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_CLASS_LOADER_NAME, isAttribute = true)
    protected abstract String getClassLoaderName();

    @Override
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_MODULE, isAttribute = true)
    protected abstract String getModuleName();

    @Override
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_MODULE_VERSION, isAttribute = true)
    protected abstract String getModuleVersion();

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
