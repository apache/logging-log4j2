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

import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Nested Diagnostic Context (NDC), aka. Thread Context Stack, resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config  = [ pattern ]
 * pattern = "pattern" -> string
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve all NDC values into a list:
 *
 * <pre>
 * {
 *   "$resolver": "ndc"
 * }
 * </pre>
 *
 * Resolve all NDC values matching with the <tt>pattern</tt> regex:
 *
 * <pre>
 * {
 *   "$resolver": "ndc",
 *   "pattern": "user(Role|Rank):\\w+"
 * }
 * </pre>
 */
public final class ThreadContextStackResolver implements EventResolver {

    private final Pattern itemPattern;

    ThreadContextStackResolver(final TemplateResolverConfig config) {
        this.itemPattern = Optional.ofNullable(config.getString("pattern"))
                .map(Pattern::compile)
                .orElse(null);
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
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        final ThreadContext.ContextStack contextStack = logEvent.getContextStack();
        if (contextStack.getDepth() == 0) {
            jsonWriter.writeNull();
            return;
        }
        boolean arrayStarted = false;
        for (final String contextStackItem : contextStack.asList()) {
            final boolean matched =
                    itemPattern == null || itemPattern.matcher(contextStackItem).matches();
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
