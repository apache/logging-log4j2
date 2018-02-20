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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mix-in for {@link StackTraceElement}.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 *
 * @see StackTraceElement
 */
@JsonIgnoreProperties("nativeMethod")
public abstract class StackTraceElementMixIn {

    @JsonCreator
    protected StackTraceElementMixIn(
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
    protected abstract String getClassName();

    @JsonProperty(StackTraceElementConstants.ATTR_FILE)
    protected abstract String getFileName();

    @JsonProperty(StackTraceElementConstants.ATTR_LINE)
    protected abstract int getLineNumber();

    @JsonProperty(StackTraceElementConstants.ATTR_METHOD)
    protected abstract String getMethodName();

}
