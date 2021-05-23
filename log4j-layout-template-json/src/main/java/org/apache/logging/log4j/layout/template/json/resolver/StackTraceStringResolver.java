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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.layout.template.json.util.TruncatingBufferedPrintWriter;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.Recycler;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Exception stack trace to JSON string resolver used by {@link ExceptionResolver}.
 */
final class StackTraceStringResolver implements StackTraceResolver {

    private final Recycler<TruncatingBufferedPrintWriter> writerRecycler;

    private final boolean truncationEnabled;

    private final String truncationSuffix;

    private final List<String> truncationPointMatcherStrings;

    private final List<Pattern> groupedTruncationPointMatcherRegexes;

    StackTraceStringResolver(
            final EventResolverContext context,
            final String truncationSuffix,
            final List<String> truncationPointMatcherStrings,
            final List<String> truncationPointMatcherRegexes) {
        final Supplier<TruncatingBufferedPrintWriter> writerSupplier =
                () -> TruncatingBufferedPrintWriter.ofCapacity(
                        context.getMaxStringByteCount());
        this.writerRecycler = context
                .getRecyclerFactory()
                .create(writerSupplier, TruncatingBufferedPrintWriter::close);
        this.truncationEnabled =
                !truncationPointMatcherStrings.isEmpty() ||
                        !truncationPointMatcherRegexes.isEmpty();
        this.truncationSuffix = truncationSuffix;
        this.truncationPointMatcherStrings = truncationPointMatcherStrings;
        this.groupedTruncationPointMatcherRegexes =
                groupTruncationPointMatcherRegexes(truncationPointMatcherRegexes);
    }

    private static List<Pattern> groupTruncationPointMatcherRegexes(
            final List<String> regexes) {
        return regexes
                .stream()
                .map(regex -> Pattern.compile(
                        "^.*(" + regex + ")(.*)$",
                        Pattern.MULTILINE | Pattern.DOTALL))
                .collect(Collectors.toList());
    }

    @Override
    public void resolve(
            final Throwable throwable,
            final JsonWriter jsonWriter) {
        final TruncatingBufferedPrintWriter writer = writerRecycler.acquire();
        try {
            throwable.printStackTrace(writer);
            truncate(writer);
            jsonWriter.writeString(writer.buffer(), 0, writer.position());
        } finally {
            writerRecycler.release(writer);
        }
    }

    private void truncate(final TruncatingBufferedPrintWriter writer) {

        // Short-circuit if truncation is not enabled.
        if (!truncationEnabled) {
            return;
        }

        // Check for string matches.
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int i = 0; i < truncationPointMatcherStrings.size(); i++) {
            final String matcher = truncationPointMatcherStrings.get(i);
            final int matchIndex = writer.indexOf(matcher);
            if (matchIndex > 0) {
                final int truncationPointIndex = matchIndex + matcher.length();
                truncate(writer, truncationPointIndex);
                return;
            }
        }

        // Check for regex matches.
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int i = 0; i < groupedTruncationPointMatcherRegexes.size(); i++) {
            final Pattern pattern = groupedTruncationPointMatcherRegexes.get(i);
            final Matcher matcher = pattern.matcher(writer);
            final boolean matched = matcher.matches();
            if (matched) {
                final int lastGroup = matcher.groupCount();
                final int truncationPointIndex = matcher.start(lastGroup);
                truncate(writer, truncationPointIndex);
                return;
            }
        }

    }

    private void truncate(
            final TruncatingBufferedPrintWriter writer,
            final int index) {
        writer.position(index);
        writer.print(truncationSuffix);
    }

}
