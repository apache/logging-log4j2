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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.layout.template.json.util.*;

/**
 * Exception stack trace to JSON string resolver used by {@link ExceptionResolver}.
 */
final class StackTraceStringResolver implements StackTraceResolver {

    private final Recycler<TruncatingBufferedPrintWriter> srcWriterRecycler;

    private final Recycler<TruncatingBufferedPrintWriter> dstWriterRecycler;

    private final Recycler<CharSequencePointer> sequencePointerRecycler;

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
                () -> TruncatingBufferedPrintWriter.ofCapacity(context.getMaxStringByteCount());
        final RecyclerFactory recyclerFactory = context.getRecyclerFactory();
        this.srcWriterRecycler = recyclerFactory.create(writerSupplier, TruncatingBufferedPrintWriter::close);
        this.dstWriterRecycler = recyclerFactory.create(writerSupplier, TruncatingBufferedPrintWriter::close);
        this.sequencePointerRecycler = recyclerFactory.create(CharSequencePointer::new);
        this.truncationEnabled = !truncationPointMatcherStrings.isEmpty() || !truncationPointMatcherRegexes.isEmpty();
        this.truncationSuffix = truncationSuffix;
        this.truncationPointMatcherStrings = truncationPointMatcherStrings;
        this.groupedTruncationPointMatcherRegexes = groupTruncationPointMatcherRegexes(truncationPointMatcherRegexes);
    }

    private static List<Pattern> groupTruncationPointMatcherRegexes(final List<String> regexes) {
        return regexes.stream()
                .map(regex -> Pattern.compile(
                        ".*?" + // Make `.*` lazy with `?` suffix, since we want to find the _first_ match of `regex`.
                                regex
                                + // Match the user input.
                                "(.*)", // Group that is to be truncated.
                        Pattern.DOTALL))
                .collect(Collectors.toList());
    }

    @Override
    public void resolve(final Throwable throwable, final JsonWriter jsonWriter) {
        final TruncatingBufferedPrintWriter srcWriter = srcWriterRecycler.acquire();
        try {
            throwable.printStackTrace(srcWriter);
            truncate(srcWriter, jsonWriter::writeString);
        } finally {
            srcWriterRecycler.release(srcWriter);
        }
    }

    private void truncate(
            final TruncatingBufferedPrintWriter srcWriter,
            final Consumer<TruncatingBufferedPrintWriter> effectiveWriterConsumer) {

        // Short-circuit if truncation is not enabled.
        if (!truncationEnabled) {
            effectiveWriterConsumer.accept(srcWriter);
            return;
        }

        // Allocate temporary buffers and truncate the input.
        final TruncatingBufferedPrintWriter dstWriter = dstWriterRecycler.acquire();
        try {
            final CharSequencePointer sequencePointer = sequencePointerRecycler.acquire();
            try {
                truncate(srcWriter, dstWriter, sequencePointer);
            } finally {
                sequencePointerRecycler.release(sequencePointer);
            }
            effectiveWriterConsumer.accept(dstWriter);
        } finally {
            dstWriterRecycler.release(dstWriter);
        }
    }

    private void truncate(
            final TruncatingBufferedPrintWriter srcWriter,
            final TruncatingBufferedPrintWriter dstWriter,
            final CharSequencePointer sequencePointer) {
        int startIndex = 0;
        for (; ; ) {

            // Find the next label start, if present.
            final int labeledLineStartIndex = findLabeledLineStartIndex(srcWriter, startIndex, srcWriter.length());
            final int endIndex = labeledLineStartIndex >= 0 ? labeledLineStartIndex : srcWriter.length();

            // Copy up to the truncation point, if it matches.
            final int truncationPointIndex = findTruncationPointIndex(srcWriter, startIndex, endIndex, sequencePointer);
            if (truncationPointIndex > 0) {
                dstWriter.append(srcWriter, startIndex, truncationPointIndex);
                dstWriter.append(System.lineSeparator());
                dstWriter.append(truncationSuffix);
            }

            // Otherwise, copy the entire labeled block.
            else {
                dstWriter.append(srcWriter, startIndex, endIndex);
            }

            // Copy the label to avoid stepping over it again.
            if (labeledLineStartIndex > 0) {
                dstWriter.append(System.lineSeparator());
                startIndex = labeledLineStartIndex;
                for (; ; ) {
                    final char c = srcWriter.charAt(startIndex++);
                    dstWriter.append(c);
                    if (c == ':') {
                        break;
                    }
                }
            }

            // Otherwise, the source is exhausted, stop.
            else {
                break;
            }
        }
    }

    private int findTruncationPointIndex(
            final TruncatingBufferedPrintWriter writer,
            final int startIndex,
            final int endIndex,
            final CharSequencePointer sequencePointer) {

        // Check for string matches.
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int i = 0; i < truncationPointMatcherStrings.size(); i++) {
            final String matcher = truncationPointMatcherStrings.get(i);
            final int matchIndex = findMatchingIndex(matcher, writer, startIndex, endIndex);
            if (matchIndex > 0) {
                // No need for `Math.addExact()`, since we have a match:
                return matchIndex + matcher.length();
            }
        }

        // Check for regex matches.
        CharSequence sequence;
        if (startIndex == 0 && endIndex == writer.length()) {
            sequence = writer;
        } else {
            sequencePointer.reset(writer, startIndex, writer.length());
            sequence = sequencePointer;
        }
        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
        for (int i = 0; i < groupedTruncationPointMatcherRegexes.size(); i++) {
            final Pattern pattern = groupedTruncationPointMatcherRegexes.get(i);
            final Matcher matcher = pattern.matcher(sequence);
            final boolean matched = matcher.matches();
            if (matched) {
                final int lastGroup = matcher.groupCount();
                return matcher.start(lastGroup);
            }
        }

        // No matches.
        return -1;
    }

    private static int findLabeledLineStartIndex(final CharSequence buffer, final int startIndex, final int endIndex) {
        // Note that the index arithmetic in this method is not guarded.
        // That is, there are no `Math.addExact()` or `Math.subtractExact()` usages.
        // Since we know a priori that we are already operating within buffer limits.
        for (int bufferIndex = startIndex; bufferIndex < endIndex; ) {

            // Find the next line start, if exists.
            final int lineStartIndex = findLineStartIndex(buffer, bufferIndex, endIndex);
            if (lineStartIndex < 0) {
                break;
            }
            bufferIndex = lineStartIndex;

            // Skip tabs.
            while (bufferIndex < endIndex && '\t' == buffer.charAt(bufferIndex)) {
                bufferIndex++;
            }

            // Search for the `Caused by: ` occurrence.
            if (bufferIndex < (endIndex - 11)
                    && buffer.charAt(bufferIndex) == 'C'
                    && buffer.charAt(bufferIndex + 1) == 'a'
                    && buffer.charAt(bufferIndex + 2) == 'u'
                    && buffer.charAt(bufferIndex + 3) == 's'
                    && buffer.charAt(bufferIndex + 4) == 'e'
                    && buffer.charAt(bufferIndex + 5) == 'd'
                    && buffer.charAt(bufferIndex + 6) == ' '
                    && buffer.charAt(bufferIndex + 7) == 'b'
                    && buffer.charAt(bufferIndex + 8) == 'y'
                    && buffer.charAt(bufferIndex + 9) == ':'
                    && buffer.charAt(bufferIndex + 10) == ' ') {
                return lineStartIndex;
            }

            // Search for the `Suppressed: ` occurrence.
            else if (bufferIndex < (endIndex - 12)
                    && buffer.charAt(bufferIndex) == 'S'
                    && buffer.charAt(bufferIndex + 1) == 'u'
                    && buffer.charAt(bufferIndex + 2) == 'p'
                    && buffer.charAt(bufferIndex + 3) == 'p'
                    && buffer.charAt(bufferIndex + 4) == 'r'
                    && buffer.charAt(bufferIndex + 5) == 'e'
                    && buffer.charAt(bufferIndex + 6) == 's'
                    && buffer.charAt(bufferIndex + 7) == 's'
                    && buffer.charAt(bufferIndex + 8) == 'e'
                    && buffer.charAt(bufferIndex + 9) == 'd'
                    && buffer.charAt(bufferIndex + 10) == ':'
                    && buffer.charAt(bufferIndex + 11) == ' ') {
                return lineStartIndex;
            }
        }
        return -1;
    }

    private static int findLineStartIndex(final CharSequence buffer, final int startIndex, final int endIndex) {
        char prevChar = '-';
        for (int i = startIndex; i <= endIndex; i++) {
            if (prevChar == '\n') {
                return i;
            }
            prevChar = buffer.charAt(i);
        }
        return -1;
    }

    private static int findMatchingIndex(
            final CharSequence matcher,
            final CharSequence buffer,
            final int bufferStartIndex,
            final int bufferEndIndex) {

        // Note that the index arithmetic in this method is not guarded.
        // That is, there are no `Math.addExact()` or `Math.subtractExact()` usages.
        // Since we know a priori that we are already operating within buffer limits.

        // While searching for an input of length `n`, no need to traverse the last `n-1` characters.
        final int effectiveBufferEndIndex = bufferEndIndex - matcher.length() + 1;

        // Perform the search.
        for (int bufferIndex = bufferStartIndex; bufferIndex <= effectiveBufferEndIndex; bufferIndex++) {
            boolean found = true;
            for (int matcherIndex = 0; matcherIndex < matcher.length(); matcherIndex++) {
                final char matcherChar = matcher.charAt(matcherIndex);
                final char bufferChar = buffer.charAt(bufferIndex + matcherIndex);
                if (matcherChar != bufferChar) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return bufferIndex;
            }
        }
        return -1;
    }
}
