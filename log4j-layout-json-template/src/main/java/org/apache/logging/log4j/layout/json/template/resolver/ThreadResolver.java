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

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;

final class ThreadResolver implements EventResolver {

    private final EventResolver internalResolver;

    ThreadResolver(final EventResolverContext context, final String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static EventResolver createInternalResolver(
            final EventResolverContext context,
            final String key) {
        switch (key) {
            case "name": return createNameResolver(context);
            case "id": return createIdResolver();
            case "priority": return createPriorityResolver();
        }
        throw new IllegalArgumentException("unknown key: " + key);
    }

    private static EventResolver createNameResolver(final EventResolverContext context) {
        return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
            final String threadName = logEvent.getThreadName();
            final boolean threadNameExcluded =
                    context.isBlankFieldExclusionEnabled() &&
                            Strings.isBlank(threadName);
            if (threadNameExcluded) {
                jsonGenerator.writeNull();
            } else {
                jsonGenerator.writeString(threadName);
            }
        };
    }

    private static EventResolver createIdResolver() {
        return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
            final long threadId = logEvent.getThreadId();
            jsonGenerator.writeNumber(threadId);
        };
    }

    private static EventResolver createPriorityResolver() {
        return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
            final int threadPriority = logEvent.getThreadPriority();
            jsonGenerator.writeNumber(threadPriority);
        };
    }

    static String getName() {
        return "thread";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonGenerator jsonGenerator)
            throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
