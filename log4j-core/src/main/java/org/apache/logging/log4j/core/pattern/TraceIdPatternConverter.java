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
package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

@Plugin(name = "TraceIdPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"traceId"})
public final class TraceIdPatternConverter extends LogEventPatternConverter {

    private static final TraceIdPatternConverter INSTANCE = new TraceIdPatternConverter();

    private TraceIdPatternConverter() {
        super("TraceId", "traceId");
    }

    public static TraceIdPatternConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final String traceId = event.getTraceId();
        if (traceId != null) {
            toAppendTo.append(traceId);
        }
    }
}
