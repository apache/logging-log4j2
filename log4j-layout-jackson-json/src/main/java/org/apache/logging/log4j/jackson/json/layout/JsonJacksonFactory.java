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
package org.apache.logging.log4j.jackson.json.layout;

import org.apache.logging.log4j.jackson.AbstractJacksonFactory;
import org.apache.logging.log4j.jackson.JsonConstants;
import org.apache.logging.log4j.jackson.json.Log4jJsonObjectMapper;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonJacksonFactory extends AbstractJacksonFactory {

    private final boolean encodeThreadContextAsList;
    private final boolean objectMessageAsJsonObject;

    public JsonJacksonFactory(final boolean encodeThreadContextAsList, final boolean includeStacktrace,
            final boolean stacktraceAsString, final boolean objectMessageAsJsonObject) {
        super(includeStacktrace, stacktraceAsString);
        this.encodeThreadContextAsList = encodeThreadContextAsList;
        this.objectMessageAsJsonObject = objectMessageAsJsonObject;
    }

    @Override
    protected String getPropertyNameForContextMap() {
        return JsonConstants.ELT_CONTEXT_MAP;
    }

    @Override
    protected String getPropertyNameForTimeMillis() {
        return JsonConstants.ELT_TIME_MILLIS;
    }

    @Override
    protected String getPropertyNameForInstant() {
        return JsonConstants.ELT_INSTANT;
    }

    @Override
    protected String getPropertyNameForNanoTime() {
        return JsonConstants.ELT_NANO_TIME;
    }

    @Override
    protected String getPropertyNameForSource() {
        return JsonConstants.ELT_SOURCE;
    }

    @Override
    protected String getPropertyNameForStackTrace() {
        return JsonConstants.ELT_EXTENDED_STACK_TRACE;
    }

    @Override
    protected PrettyPrinter newCompactPrinter() {
        return new MinimalPrettyPrinter();
    }

    @Override
    protected ObjectMapper newObjectMapper() {
        return new Log4jJsonObjectMapper(encodeThreadContextAsList, includeStacktrace, stacktraceAsString,
                objectMessageAsJsonObject);
    }

    @Override
    protected PrettyPrinter newPrettyPrinter() {
        return new DefaultPrettyPrinter();
    }

}
