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
package org.apache.logging.log4j.layout.template.json;

import static org.apache.logging.log4j.kit.env.Log4jProperty.SYSTEM;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Log4jProperty(name = "layout.jsonTemplate")
@NullMarked
public record JsonTemplateLayoutProperties(
        @Log4jProperty(defaultValue = "UTF-8") Charset charset,
        @Log4jProperty(defaultValue = "false") boolean locationInfoEnabled,
        @Log4jProperty(defaultValue = "true") boolean stackTraceEnabled,
        @Log4jProperty(defaultValue = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ") String timestampFormatPattern,
        @Log4jProperty(defaultValue = SYSTEM) TimeZone timeZone,
        @Log4jProperty(defaultValue = SYSTEM) Locale locale,
        @Nullable String eventTemplate,
        @Log4jProperty(defaultValue = "classpath:EcsLayout.json") String eventTemplateUri,
        @Nullable String stackTraceElementTemplate,
        @Log4jProperty(defaultValue = "classpath:StackTraceElementLayout.json") String stackTraceElementTemplateUri,
        @Nullable String eventTemplateRootObjectKey,
        @Nullable String eventDelimiter,
        boolean nullEventDelimiterEnabled,
        @Log4jProperty(defaultValue = "16384") int maxStringLength,
        @Log4jProperty(defaultValue = "…") String truncatedStringSuffix) {

    @Override
    public String eventDelimiter() {
        return eventDelimiter != null ? eventDelimiter : System.lineSeparator();
    }

    @Override
    public int maxStringLength() {
        if (maxStringLength <= 0) {
            throw new IllegalArgumentException("was expecting a non-zero positive maxStringLength: " + maxStringLength);
        }
        return maxStringLength;
    }
}
