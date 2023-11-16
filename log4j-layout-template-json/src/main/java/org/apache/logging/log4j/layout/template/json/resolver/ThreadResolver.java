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
 * Thread resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config = "field" -> ( "name" | "id" | "priority" )
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the thread name:
 *
 * <pre>
 * {
 *   "$resolver": "thread",
 *   "field": "name"
 * }
 * </pre>
 */
public final class ThreadResolver implements EventResolver {

    private static final EventResolver NAME_RESOLVER = (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
        final String threadName = logEvent.getThreadName();
        jsonWriter.writeString(threadName);
    };

    private static final EventResolver ID_RESOLVER = (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
        final long threadId = logEvent.getThreadId();
        jsonWriter.writeNumber(threadId);
    };

    private static final EventResolver PRIORITY_RESOLVER = (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
        final int threadPriority = logEvent.getThreadPriority();
        jsonWriter.writeNumber(threadPriority);
    };

    private final EventResolver internalResolver;

    ThreadResolver(final TemplateResolverConfig config) {
        this.internalResolver = createInternalResolver(config);
    }

    private static EventResolver createInternalResolver(final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");
        if ("name".equals(fieldName)) {
            return NAME_RESOLVER;
        } else if ("id".equals(fieldName)) {
            return ID_RESOLVER;
        } else if ("priority".equals(fieldName)) {
            return PRIORITY_RESOLVER;
        }
        throw new IllegalArgumentException("unknown field: " + config);
    }

    static String getName() {
        return "thread";
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }
}
