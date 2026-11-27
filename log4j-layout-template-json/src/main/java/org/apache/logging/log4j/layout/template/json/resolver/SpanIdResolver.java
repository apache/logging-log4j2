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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Resolves the W3C standard span ID.
 *
 * <h3>Examples</h3>
 *
 * Resolve the span ID:
 *
 * <pre>
 * {
 *   "$resolver": "spanId"
 * }
 * </pre>
 */
public final class SpanIdResolver implements EventResolver {

    private static final SpanIdResolver INSTANCE = new SpanIdResolver();

    SpanIdResolver() {}

    static SpanIdResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "spanId";
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        jsonWriter.writeString(logEvent.getSpanId());
    }
}
