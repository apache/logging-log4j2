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

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;

import java.util.regex.Pattern;

/**
 * Add Nested Diagnostic Context (NDC).
 */
final class ContextStackResolver implements EventResolver {

    private final EventResolverContext context;

    ContextStackResolver(final EventResolverContext context) {
        this.context = context;
    }

    static String getName() {
        return "ndc";
    }

    @Override
    public boolean isResolvable(final LogEvent logEvent) {
        final ThreadContext.ContextStack contextStack = logEvent.getContextStack();
        return contextStack.getDepth() > 0;
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        final ThreadContext.ContextStack contextStack = logEvent.getContextStack();
        if (contextStack.getDepth() == 0) {
            jsonWriter.writeNull();
            return;
        }
        final Pattern itemPattern = context.getNdcPattern();
        boolean arrayStarted = false;
        for (final String contextStackItem : contextStack.asList()) {
            final boolean matched =
                    itemPattern == null ||
                            itemPattern.matcher(contextStackItem).matches();
            if (matched) {
                if (arrayStarted) {
                    jsonWriter.writeSeparator();
                } else {
                    jsonWriter.writeArrayStart();
                    arrayStarted = true;
                }
                jsonWriter.writeString(contextStackItem);
            }
        }
        if (arrayStarted) {
            jsonWriter.writeArrayEnd();
        } else {
            jsonWriter.writeNull();
        }
    }

}
