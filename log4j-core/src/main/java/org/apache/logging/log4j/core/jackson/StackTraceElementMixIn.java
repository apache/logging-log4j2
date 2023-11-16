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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Jackson mix-in used to serialize a {@link StackTraceElement}. Deserialization
 * is performed by {@link StackTraceElementMixIn}.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 *
 * @see StackTraceElement
 */
@JsonIgnoreProperties("nativeMethod")
abstract class StackTraceElementMixIn {
    @JsonCreator
    StackTraceElementMixIn(
            // @formatter:off
            @JsonProperty(StackTraceElementConstants.ATTR_CLASS) final String declaringClass,
            @JsonProperty(StackTraceElementConstants.ATTR_METHOD) final String methodName,
            @JsonProperty(StackTraceElementConstants.ATTR_FILE) final String fileName,
            @JsonProperty(StackTraceElementConstants.ATTR_LINE) final int lineNumber)
                // @formatter:on
            {
        // empty
    }

    @JsonProperty(StackTraceElementConstants.ATTR_CLASS)
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_CLASS, isAttribute = true)
    abstract String getClassName();

    @JsonProperty(StackTraceElementConstants.ATTR_FILE)
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_FILE, isAttribute = true)
    abstract String getFileName();

    @JsonProperty(StackTraceElementConstants.ATTR_LINE)
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_LINE, isAttribute = true)
    abstract int getLineNumber();

    @JsonProperty(StackTraceElementConstants.ATTR_METHOD)
    @JacksonXmlProperty(localName = StackTraceElementConstants.ATTR_METHOD, isAttribute = true)
    abstract String getMethodName();
}
