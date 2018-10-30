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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Jackson mix-in for {@link StackTraceElement}.
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
            @JsonProperty("class") final String declaringClass, 
            @JsonProperty("method") final String methodName,
            @JsonProperty("file") final String fileName, 
            @JsonProperty("line") final int lineNumber)
            // @formatter:on
    {
        // empty
    }

    @JsonProperty("class")
    @JacksonXmlProperty(localName = "class", isAttribute = true)
    abstract String getClassName();

    @JsonProperty("file")
    @JacksonXmlProperty(localName = "file", isAttribute = true)
    abstract String getFileName();

    @JsonProperty("line")
    @JacksonXmlProperty(localName = "line", isAttribute = true)
    abstract int getLineNumber();

    @JsonProperty("method")
    @JacksonXmlProperty(localName = "method", isAttribute = true)
    abstract String getMethodName();

}
