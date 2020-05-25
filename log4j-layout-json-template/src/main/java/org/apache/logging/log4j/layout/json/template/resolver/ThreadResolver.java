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
package org.apache.logging.log4j.layout.json.template.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;

final class ThreadResolver implements EventResolver {

    private static final EventResolver NAME_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final String threadName = logEvent.getThreadName();
                jsonWriter.writeString(threadName);
            };

    private static final EventResolver ID_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final long threadId = logEvent.getThreadId();
                jsonWriter.writeNumber(threadId);
            };

    private static final EventResolver PRIORITY_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final int threadPriority = logEvent.getThreadPriority();
                jsonWriter.writeNumber(threadPriority);
            };

    private final EventResolver internalResolver;

    ThreadResolver(final String key) {
        this.internalResolver = createInternalResolver(key);
    }

    private static EventResolver createInternalResolver(final String key) {
        switch (key) {
            case "name": return NAME_RESOLVER;
            case "id": return ID_RESOLVER;
            case "priority": return PRIORITY_RESOLVER;
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    static String getName() {
        return "thread";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }

}
