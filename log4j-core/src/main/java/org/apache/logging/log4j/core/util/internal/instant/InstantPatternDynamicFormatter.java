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
package org.apache.logging.log4j.core.util.internal.instant;

import static java.util.Objects.requireNonNull;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.jspecify.annotations.Nullable;

/**
 * An {@link InstantPatternFormatter} that uses {@link DateTimeFormatter} under the hood.
 * The pattern is analyzed and parts that require a precision lower than or equal to {@value InstantPatternDynamicFormatter#PRECISION_THRESHOLD} are precomputed, cached, and updated once every {@value InstantPatternDynamicFormatter#PRECISION_THRESHOLD}.
 * The rest is computed dynamically.
 * <p>
 * For instance, given the pattern {@code yyyy-MM-dd'T'HH:mm:ss.SSSX}, the generated formatter will
 * </p>
 * <ol>
 * <li>Sequence the pattern and assign a time precision to each part (e.g., {@code MM} is of month precision)</li>
 * <li>Precompute and cache the output for parts that are of precision lower than or equal to {@value InstantPatternDynamicFormatter#PRECISION_THRESHOLD} (i.e., {@code yyyy-MM-dd'T'HH:mm:}, {@code .}, and {@code X}) and cache it</li>
 * <li>Upon a formatting request, combine the cached outputs with the dynamic parts (i.e., {@code ss} and {@code SSS})</li>
 * </ol>
 *
 * @since 2.25.0
 */
public final class InstantPatternDynamicFormatter implements InstantPatternFormatter {

    static final ChronoUnit PRECISION_THRESHOLD = ChronoUnit.MINUTES;

    private final AtomicReference<TimestampedFormatter> timestampedFormatterRef;

    InstantPatternDynamicFormatter(final String pattern, final Locale locale, final TimeZone timeZone) {
        final TimestampedFormatter timestampedFormatter = createTimestampedFormatter(pattern, locale, timeZone, null);
        this.timestampedFormatterRef = new AtomicReference<>(timestampedFormatter);
    }

    @Override
    public String getPattern() {
        return timestampedFormatterRef.get().formatter.getPattern();
    }

    @Override
    public Locale getLocale() {
        return timestampedFormatterRef.get().formatter.getLocale();
    }

    @Override
    public TimeZone getTimeZone() {
        return timestampedFormatterRef.get().formatter.getTimeZone();
    }

    @Override
    public ChronoUnit getPrecision() {
        return timestampedFormatterRef.get().formatter.getPrecision();
    }

    @Override
    public void formatTo(final StringBuilder buffer, final Instant instant) {
        requireNonNull(buffer, "buffer");
        requireNonNull(instant, "instant");
        getEffectiveFormatter(instant).formatTo(buffer, instant);
    }

    private InstantPatternFormatter getEffectiveFormatter(final Instant instant) {

        // Reuse the instance formatter, if timestamps match
        TimestampedFormatter oldTimestampedFormatter = timestampedFormatterRef.get();
        final long instantEpochMinutes = toEpochMinutes(instant);
        final InstantPatternFormatter oldFormatter = oldTimestampedFormatter.formatter;
        if (oldTimestampedFormatter.instantEpochMinutes == instantEpochMinutes) {
            return oldFormatter;
        }

        // Create a new formatter, [try to] update the instance formatter, and return that
        final TimestampedFormatter newTimestampedFormatter = createTimestampedFormatter(
                oldFormatter.getPattern(), oldFormatter.getLocale(), oldFormatter.getTimeZone(), instant);
        timestampedFormatterRef.compareAndSet(oldTimestampedFormatter, newTimestampedFormatter);
        return newTimestampedFormatter.formatter;
    }

    private static TimestampedFormatter createTimestampedFormatter(
            final String pattern, final Locale locale, final TimeZone timeZone, @Nullable Instant creationInstant) {
        if (creationInstant == null) {
            creationInstant = new MutableInstant();
            final java.time.Instant currentInstant = java.time.Instant.now();
            ((MutableInstant) creationInstant)
                    .initFromEpochSecond(currentInstant.getEpochSecond(), creationInstant.getNanoOfSecond());
        }
        final InstantPatternFormatter formatter =
                createFormatter(pattern, locale, timeZone, PRECISION_THRESHOLD, creationInstant);
        final long creationInstantEpochMinutes = toEpochMinutes(creationInstant);
        return new TimestampedFormatter(creationInstantEpochMinutes, formatter);
    }

    private static final class TimestampedFormatter {

        private final long instantEpochMinutes;

        private final InstantPatternFormatter formatter;

        private TimestampedFormatter(final long instantEpochMinutes, final InstantPatternFormatter formatter) {
            this.instantEpochMinutes = instantEpochMinutes;
            this.formatter = formatter;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static InstantPatternFormatter createFormatter(
            final String pattern,
            final Locale locale,
            final TimeZone timeZone,
            final ChronoUnit precisionThreshold,
            final Instant creationInstant) {

        // Sequence the pattern and create associated formatters
        final List<PatternSequence> sequences = sequencePattern(pattern, precisionThreshold);
        final List<InstantPatternFormatter> formatters = sequences.stream()
                .map(sequence -> {
                    final InstantPatternFormatter formatter = sequence.createFormatter(locale, timeZone);
                    final boolean constant = sequence.isConstantForDurationOf(precisionThreshold);
                    if (!constant) {
                        return formatter;
                    }
                    final String formattedInstant;
                    {
                        final StringBuilder buffer = new StringBuilder();
                        formatter.formatTo(buffer, creationInstant);
                        formattedInstant = buffer.toString();
                    }
                    return new AbstractFormatter(formatter.getPattern(), locale, timeZone, formatter.getPrecision()) {
                        @Override
                        public void formatTo(final StringBuilder buffer, final Instant instant) {
                            buffer.append(formattedInstant);
                        }
                    };
                })
                .collect(Collectors.toList());

        switch (formatters.size()) {

                // If found an empty pattern, return an empty formatter
            case 0:
                return new AbstractFormatter(pattern, locale, timeZone, ChronoUnit.FOREVER) {
                    @Override
                    public void formatTo(final StringBuilder buffer, final Instant instant) {
                        // Do nothing
                    }
                };

                // If extracted a single formatter, return it as is
            case 1:
                return formatters.get(0);

                // Combine all extracted formatters into one
            default:
                final ChronoUnit precision = new CompositePatternSequence(sequences).precision;
                return new AbstractFormatter(pattern, locale, timeZone, precision) {
                    @Override
                    public void formatTo(final StringBuilder buffer, final Instant instant) {
                        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
                        for (int formatterIndex = 0; formatterIndex < formatters.size(); formatterIndex++) {
                            final InstantPatternFormatter formatter = formatters.get(formatterIndex);
                            formatter.formatTo(buffer, instant);
                        }
                    }
                };
        }
    }

    static List<PatternSequence> sequencePattern(final String pattern, final ChronoUnit precisionThreshold) {
        List<PatternSequence> sequences = sequencePattern(pattern);
        final List<PatternSequence> mergedSequences = mergeDynamicSequences(sequences, precisionThreshold);
        return mergeConsequentEffectivelyConstantSequences(mergedSequences, precisionThreshold);
    }

    private static List<PatternSequence> sequencePattern(final String pattern) {
        if (pattern.isEmpty()) {
            return Collections.emptyList();
        }
        final List<PatternSequence> sequences = new ArrayList<>();
        for (int startIndex = 0; startIndex < pattern.length(); ) {
            final char c = pattern.charAt(startIndex);

            // Handle dynamic pattern letters
            final boolean dynamic = isDynamicPatternLetter(c);
            if (dynamic) {
                int endIndex = startIndex + 1;
                while (endIndex < pattern.length() && pattern.charAt(endIndex) == c) {
                    endIndex++;
                }
                final String sequenceContent = pattern.substring(startIndex, endIndex);
                final PatternSequence sequence = new DynamicPatternSequence(sequenceContent);
                sequences.add(sequence);
                startIndex = endIndex;
            }

            // Handle single-quotes
            else if (c == '\'') {
                final int endIndex = pattern.indexOf('\'', startIndex + 1);
                if (endIndex < 0) {
                    final String message = String.format(
                            "pattern ends with an incomplete string literal that started at index %d: `%s`",
                            startIndex, pattern);
                    throw new IllegalArgumentException(message);
                }
                final String sequenceLiteral =
                        (startIndex + 1) == endIndex ? "'" : pattern.substring(startIndex + 1, endIndex);
                final PatternSequence sequence = new StaticPatternSequence(sequenceLiteral);
                sequences.add(sequence);
                startIndex = endIndex + 1;
            }

            // Handle unknown literal
            else {
                final PatternSequence sequence = new StaticPatternSequence("" + c);
                sequences.add(sequence);
                startIndex++;
            }
        }
        return mergeConsequentStaticPatternSequences(sequences);
    }

    private static boolean isDynamicPatternLetter(final char c) {
        return "GuyDMLdgQqYwWEecFaBhKkHmsSAnNVvzOXxZ".indexOf(c) >= 0;
    }

    /**
     * Merges consequent static sequences.
     *
     * <p>
     * For example, the sequencing of the {@code [MM-dd] HH:mm} pattern will create two static sequences for {@code ]} (right brace) and {@code } (whitespace) characters.
     * This method will combine such consequent static sequences into one.
     * </p>
     *
     * <h2>Example</h2>
     *
     * <p>
     * The {@code [MM-dd] HH:mm} pattern will result in following sequences:
     * </p>
     *
     * <pre>{@code
     * [
     *     static(literal="["),
     *     dynamic(pattern="MM", precision=MONTHS),
     *     static(literal="-"),
     *     dynamic(pattern="dd", precision=DAYS),
     *     static(literal="]"),
     *     static(literal=" "),
     *     dynamic(pattern="HH", precision=HOURS),
     *     static(literal=":"),
     *     dynamic(pattern="mm", precision=MINUTES)
     * ]
     * }</pre>
     *
     * <p>
     * The above sequencing implies creation of 9 {@link AbstractFormatter}s.
     * This method transforms it to the following:
     * </p>
     *
     * <pre>{@code
     * [
     *     static(literal="["),
     *     dynamic(pattern="MM", precision=MONTHS),
     *     static(literal="-"),
     *     dynamic(pattern="dd", precision=DAYS),
     *     static(literal="] "),
     *     dynamic(pattern="HH", precision=HOURS),
     *     static(literal=":"),
     *     dynamic(pattern="mm", precision=MINUTES)
     * ]
     * }</pre>
     *
     * <p>
     * The above sequencing implies creation of 8 {@link AbstractFormatter}s.
     * </p>
     *
     * @param sequences sequences to be transformed
     * @return transformed sequencing where consequent static sequences are merged
     */
    private static List<PatternSequence> mergeConsequentStaticPatternSequences(final List<PatternSequence> sequences) {

        // Short-circuit if there is nothing to merge
        if (sequences.size() < 2) {
            return sequences;
        }

        final List<PatternSequence> mergedSequences = new ArrayList<>();
        final List<StaticPatternSequence> accumulatedSequences = new ArrayList<>();
        for (final PatternSequence sequence : sequences) {

            // Spotted a static sequence? Stage it for merging.
            if (sequence instanceof StaticPatternSequence) {
                accumulatedSequences.add((StaticPatternSequence) sequence);
            }

            // Spotted a dynamic sequence.
            // Merge the accumulated static sequences, and then append the dynamic sequence.
            else {
                mergeConsequentStaticPatternSequences(mergedSequences, accumulatedSequences);
                mergedSequences.add(sequence);
            }
        }

        // Merge leftover static sequences
        mergeConsequentStaticPatternSequences(mergedSequences, accumulatedSequences);
        return mergedSequences;
    }

    private static void mergeConsequentStaticPatternSequences(
            final List<PatternSequence> mergedSequences, final List<StaticPatternSequence> accumulatedSequences) {
        mergeAccumulatedSequences(mergedSequences, accumulatedSequences, () -> {
            final String literal = accumulatedSequences.stream()
                    .map(sequence -> sequence.literal)
                    .collect(Collectors.joining());
            return new StaticPatternSequence(literal);
        });
    }

    /**
     * Merges the sequences in between the first and the last found dynamic (i.e., non-constant) sequences.
     *
     * <p>
     * For example, given the {@code ss.SSS} pattern – where {@code ss} and {@code SSS} is effectively not constant, yet {@code .} is – this method will combine it into a single dynamic sequence.
     * Because, as demonstrated in {@code DateTimeFormatterSequencingBenchmark}, formatting {@code ss.SSS} is approximately 20% faster than formatting first {@code ss}, then manually appending a {@code .}, and then formatting {@code SSS}.
     * </p>
     *
     * <h2>Example</h2>
     *
     * <p>
     * Assume {@link #mergeConsequentStaticPatternSequences(List)} produced the following:
     * </p>
     *
     * <pre>{@code
     * [
     *     dynamic(pattern="yyyy", precision=YEARS),
     *     static(literal="-"),
     *     dynamic(pattern="MM", precision=MONTHS),
     *     static(literal="-"),
     *     dynamic(pattern="dd", precision=DAYS),
     *     static(literal="T"),
     *     dynamic(pattern="HH", precision=HOURS),
     *     static(literal=":"),
     *     dynamic(pattern="mm", precision=MINUTES),
     *     static(literal=":"),
     *     dynamic(pattern="ss", precision=SECONDS),
     *     static(literal="."),
     *     dynamic(pattern="SSS", precision=MILLISECONDS),
     *     dynamic(pattern="X", precision=HOURS),
     * ]
     * }</pre>
     *
     * <p>
     * For a threshold precision of {@link ChronoUnit#MINUTES}, this sequencing effectively translates to two {@link DateTimeFormatter#formatTo(TemporalAccessor, Appendable)} invocations for each {@link #formatTo(StringBuilder, Instant)} call: one for {@code ss}, and another one for {@code SSS}.
     * This method transforms the above sequencing into the following:
     * </p>
     *
     * <pre>{@code
     * [
     *     dynamic(pattern="yyyy", precision=YEARS),
     *     static(literal="-"),
     *     dynamic(pattern="MM", precision=MONTHS),
     *     static(literal="-"),
     *     dynamic(pattern="dd", precision=DAYS),
     *     static(literal="T"),
     *     dynamic(pattern="HH", precision=HOURS),
     *     static(literal=":"),
     *     dynamic(pattern="mm", precision=MINUTES),
     *     static(literal=":"),
     *     composite(
     *         sequences=[
     *             dynamic(pattern="ss", precision=SECONDS),
     *             static(literal="."),
     *             dynamic(pattern="SSS", precision=MILLISECONDS)
     *         ],
     *         precision=MILLISECONDS),
     *     dynamic(pattern="X", precision=HOURS),
     * ]
     * }</pre>
     *
     * <p>
     * The resultant sequencing effectively translates to a single {@link DateTimeFormatter#formatTo(TemporalAccessor, Appendable)} invocation for each {@link #formatTo(StringBuilder, Instant)} call: only one fore {@code ss.SSS}.
     * </p>
     *
     * @param sequences sequences, preferable produced by {@link #mergeConsequentStaticPatternSequences(List)}, to be transformed
     * @param precisionThreshold  a precision threshold to determine dynamic (i.e., non-constant) sequences
     * @return transformed sequencing where sequences in between the first and the last found dynamic (i.e., non-constant) sequences are merged
     */
    private static List<PatternSequence> mergeDynamicSequences(
            final List<PatternSequence> sequences, final ChronoUnit precisionThreshold) {

        // Locate the first and the last dynamic (i.e., non-constant) sequence indices
        int firstDynamicSequenceIndex = -1;
        int lastDynamicSequenceIndex = -1;
        for (int sequenceIndex = 0; sequenceIndex < sequences.size(); sequenceIndex++) {
            final PatternSequence sequence = sequences.get(sequenceIndex);
            final boolean constant = sequence.isConstantForDurationOf(precisionThreshold);
            if (!constant) {
                if (firstDynamicSequenceIndex < 0) {
                    firstDynamicSequenceIndex = sequenceIndex;
                }
                lastDynamicSequenceIndex = sequenceIndex;
            }
        }

        // Short-circuit if there are less than 2 dynamic sequences
        if (firstDynamicSequenceIndex < 0 || firstDynamicSequenceIndex == lastDynamicSequenceIndex) {
            return sequences;
        }

        // Merge dynamic sequences
        final List<PatternSequence> mergedSequences = new ArrayList<>();
        if (firstDynamicSequenceIndex > 0) {
            mergedSequences.addAll(sequences.subList(0, firstDynamicSequenceIndex));
        }
        final PatternSequence mergedDynamicSequence = new CompositePatternSequence(
                sequences.subList(firstDynamicSequenceIndex, lastDynamicSequenceIndex + 1));
        mergedSequences.add(mergedDynamicSequence);
        if ((lastDynamicSequenceIndex + 1) < sequences.size()) {
            mergedSequences.addAll(sequences.subList(lastDynamicSequenceIndex + 1, sequences.size()));
        }
        return mergedSequences;
    }

    /**
     * Merges sequences that are consequent and effectively constant for the provided precision threshold.
     *
     * <p>
     * For example, given the {@code yyyy-MM-dd'T'HH:mm:ss.SSS} pattern and a precision threshold of {@link ChronoUnit#MINUTES}, this method will combine sequences associated with {@code yyyy-MM-dd'T'HH:mm:} into a single sequence, since these are consequent and effectively constant sequences.
     * </p>
     *
     * <h2>Example</h2>
     *
     * <p>
     * Assume {@link #mergeDynamicSequences(List, ChronoUnit)} produced the following:
     * </p>
     *
     * <pre>{@code
     * [
     *     dynamic(pattern="yyyy", precision=YEARS),
     *     static(literal="-"),
     *     dynamic(pattern="MM", precision=MONTHS),
     *     static(literal="-"),
     *     dynamic(pattern="dd", precision=DAYS),
     *     static(literal="T"),
     *     dynamic(pattern="HH", precision=HOURS),
     *     static(literal=":"),
     *     dynamic(pattern="mm", precision=MINUTES),
     *     static(literal=":"),
     *     composite(
     *         sequences=[
     *             dynamic(pattern="ss", precision=SECONDS),
     *             static(literal="."),
     *             dynamic(pattern="SSS", precision=MILLISECONDS)
     *         ],
     *         precision=MILLISECONDS),
     *     dynamic(pattern="X", precision=HOURS),
     * ]
     * }</pre>
     *
     * <p>
     * The above sequencing implies creation of 12 {@link AbstractFormatter}s.
     * This method transforms it to the following:
     * </p>
     *
     * <pre>{@code
     * [
     *     composite(
     *         sequences=[
     *             dynamic(pattern="yyyy", precision=YEARS),
     *             static(literal="-"),
     *             dynamic(pattern="MM", precision=MONTHS),
     *             static(literal="-"),
     *             dynamic(pattern="dd", precision=DAYS),
     *             static(literal="T"),
     *             dynamic(pattern="HH", precision=HOURS),
     *             static(literal=":"),
     *             dynamic(pattern="mm", precision=MINUTES),
     *             static(literal=":")
     *         ],
     *         precision=MINUTES),
     *     composite(
     *         sequences=[
     *             dynamic(pattern="ss", precision=SECONDS),
     *             static(literal="."),
     *             dynamic(pattern="SSS", precision=MILLISECONDS)
     *         ],
     *         precision=MILLISECONDS),
     *     dynamic(pattern="X", precision=HOURS),
     * ]
     * }</pre>
     *
     * <p>
     * The resultant sequencing effectively translates to 3 {@link AbstractFormatter}s.
     * </p>
     *
     * @param sequences sequences, preferable produced by {@link #mergeDynamicSequences(List, ChronoUnit)}, to be transformed
     * @param precisionThreshold  a precision threshold to determine effectively constant sequences
     * @return transformed sequencing where sequences that are consequent and effectively constant for the provided precision threshold are merged
     */
    private static List<PatternSequence> mergeConsequentEffectivelyConstantSequences(
            final List<PatternSequence> sequences, final ChronoUnit precisionThreshold) {

        // Short-circuit if there is nothing to merge
        if (sequences.size() < 2) {
            return sequences;
        }

        final List<PatternSequence> mergedSequences = new ArrayList<>();
        boolean accumulatorConstant = true;
        final List<PatternSequence> accumulatedSequences = new ArrayList<>();
        for (final PatternSequence sequence : sequences) {
            final boolean sequenceConstant = sequence.isConstantForDurationOf(precisionThreshold);
            if (sequenceConstant != accumulatorConstant) {
                mergeConsequentEffectivelyConstantSequences(mergedSequences, accumulatedSequences);
                accumulatorConstant = sequenceConstant;
            }
            accumulatedSequences.add(sequence);
        }

        // Merge the accumulator leftover
        mergeConsequentEffectivelyConstantSequences(mergedSequences, accumulatedSequences);
        return mergedSequences;
    }

    private static void mergeConsequentEffectivelyConstantSequences(
            final List<PatternSequence> mergedSequences, final List<PatternSequence> accumulatedSequences) {
        mergeAccumulatedSequences(
                mergedSequences, accumulatedSequences, () -> new CompositePatternSequence(accumulatedSequences));
    }

    private static <S extends PatternSequence> void mergeAccumulatedSequences(
            final List<PatternSequence> mergedSequences,
            final List<S> accumulatedSequences,
            final Supplier<PatternSequence> mergedSequenceSupplier) {
        if (accumulatedSequences.isEmpty()) {
            return;
        }
        final PatternSequence mergedSequence =
                accumulatedSequences.size() == 1 ? accumulatedSequences.get(0) : mergedSequenceSupplier.get();
        mergedSequences.add(mergedSequence);
        accumulatedSequences.clear();
    }

    private static long toEpochMinutes(final Instant instant) {
        return instant.getEpochSecond() / 60;
    }

    private static TemporalAccessor toTemporalAccessor(final Instant instant) {
        return instant instanceof TemporalAccessor
                ? (TemporalAccessor) instant
                : java.time.Instant.ofEpochSecond(instant.getEpochSecond(), instant.getNanoOfSecond());
    }

    private abstract static class AbstractFormatter implements InstantPatternFormatter {

        private final String pattern;

        private final Locale locale;

        private final TimeZone timeZone;

        private final ChronoUnit precision;

        private AbstractFormatter(
                final String pattern, final Locale locale, final TimeZone timeZone, final ChronoUnit precision) {
            this.pattern = pattern;
            this.locale = locale;
            this.timeZone = timeZone;
            this.precision = precision;
        }

        @Override
        public ChronoUnit getPrecision() {
            return precision;
        }

        @Override
        public String getPattern() {
            return pattern;
        }

        @Override
        public Locale getLocale() {
            return locale;
        }

        @Override
        public TimeZone getTimeZone() {
            return timeZone;
        }
    }

    abstract static class PatternSequence {

        final String pattern;

        final ChronoUnit precision;

        @SuppressWarnings("ReturnValueIgnored")
        PatternSequence(final String pattern, final ChronoUnit precision) {
            DateTimeFormatter.ofPattern(pattern); // Validate the pattern
            this.pattern = pattern;
            this.precision = precision;
        }

        InstantPatternFormatter createFormatter(final Locale locale, final TimeZone timeZone) {
            final DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofPattern(pattern, locale).withZone(timeZone.toZoneId());
            return new AbstractFormatter(pattern, locale, timeZone, precision) {
                @Override
                public void formatTo(final StringBuilder buffer, final Instant instant) {
                    final TemporalAccessor instantAccessor = toTemporalAccessor(instant);
                    dateTimeFormatter.formatTo(instantAccessor, buffer);
                }
            };
        }

        private boolean isConstantForDurationOf(final ChronoUnit thresholdPrecision) {
            return precision.compareTo(thresholdPrecision) >= 0;
        }

        @Override
        public boolean equals(final Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            PatternSequence sequence = (PatternSequence) object;
            return Objects.equals(pattern, sequence.pattern) && precision == sequence.precision;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern, precision);
        }

        @Override
        public String toString() {
            return String.format("<%s>%s", pattern, precision);
        }
    }

    static final class StaticPatternSequence extends PatternSequence {

        private final String literal;

        StaticPatternSequence(final String literal) {
            super(literal.equals("'") ? "''" : ("'" + literal + "'"), ChronoUnit.FOREVER);
            this.literal = literal;
        }

        @Override
        InstantPatternFormatter createFormatter(final Locale locale, final TimeZone timeZone) {
            return new AbstractFormatter(pattern, locale, timeZone, precision) {
                @Override
                public void formatTo(final StringBuilder buffer, final Instant instant) {
                    buffer.append(literal);
                }
            };
        }
    }

    static final class DynamicPatternSequence extends PatternSequence {

        DynamicPatternSequence(final String content) {
            super(content, contentPrecision(content));
        }

        /**
         * @param content a single-letter directive content complying (e.g., {@code H}, {@code HH}, or {@code pHH})
         * @return the time precision of the directive
         */
        @Nullable
        private static ChronoUnit contentPrecision(final String content) {

            validateContent(content);
            final String paddingRemovedContent = removePadding(content);

            if (paddingRemovedContent.matches("[GuyY]+")) {
                return ChronoUnit.YEARS;
            } else if (paddingRemovedContent.matches("[MLQq]+")) {
                return ChronoUnit.MONTHS;
            } else if (paddingRemovedContent.matches("[wW]+")) {
                return ChronoUnit.WEEKS;
            } else if (paddingRemovedContent.matches("[DdgEecF]+")) {
                return ChronoUnit.DAYS;
            } else if (paddingRemovedContent.matches("[aBhKkH]+")
                    // Time-zone directives
                    || paddingRemovedContent.matches("[ZxXOzvV]+")) {
                return ChronoUnit.HOURS;
            } else if (paddingRemovedContent.contains("m")) {
                return ChronoUnit.MINUTES;
            } else if (paddingRemovedContent.contains("s")) {
                return ChronoUnit.SECONDS;
            }

            // 2 to 3 consequent `S` characters output millisecond precision
            else if (paddingRemovedContent.matches("S{2,3}")
                    // `A` (milli-of-day) outputs millisecond precision.
                    || paddingRemovedContent.contains("A")) {
                return ChronoUnit.MILLIS;
            }

            // 4 to 6 consequent `S` characters output microsecond precision
            else if (paddingRemovedContent.matches("S{4,6}")) {
                return ChronoUnit.MICROS;
            }

            // A single `S` (fraction-of-second) outputs nanosecond precision
            else if (paddingRemovedContent.equals("S")
                    // 7 to 9 consequent `S` characters output nanosecond precision
                    || paddingRemovedContent.matches("S{7,9}")
                    // `n` (nano-of-second) and `N` (nano-of-day) always output nanosecond precision.
                    // This is independent of how many times they occur sequentially.
                    || paddingRemovedContent.matches("[nN]+")) {
                return ChronoUnit.NANOS;
            }

            final String message = String.format("unrecognized pattern: `%s`", content);
            throw new IllegalArgumentException(message);
        }

        private static void validateContent(final String content) {

            // Is the content empty?
            final String paddingRemovedContent = removePadding(content);
            if (paddingRemovedContent.isEmpty()) {
                final String message = String.format("empty content: `%s`", content);
                throw new IllegalArgumentException(message);
            }

            // Does the content start with a recognized letter?
            final char letter = paddingRemovedContent.charAt(0);
            final boolean dynamic = isDynamicPatternLetter(letter);
            if (!dynamic) {
                String message =
                        String.format("pattern sequence doesn't start with a dynamic pattern letter: `%s`", content);
                throw new IllegalArgumentException(message);
            }

            // Is the content composed of repetitions of the first letter?
            final boolean repeated = paddingRemovedContent.matches("^(\\Q" + letter + "\\E)+$");
            if (!repeated) {
                String message = String.format(
                        "was expecting letter `%c` to be repeated through the entire pattern sequence: `%s`",
                        letter, content);
                throw new IllegalArgumentException(message);
            }
        }

        private static String removePadding(final String content) {
            return content.replaceAll("^p+", "");
        }
    }

    static final class CompositePatternSequence extends PatternSequence {

        CompositePatternSequence(final List<PatternSequence> sequences) {
            super(concatSequencePatterns(sequences), findSequenceMaxPrecision(sequences));
            // Only allow two or more sequences
            if (sequences.size() < 2) {
                throw new IllegalArgumentException("was expecting two or more sequences: " + sequences);
            }
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        private static ChronoUnit findSequenceMaxPrecision(List<PatternSequence> sequences) {
            return sequences.stream()
                    .map(sequence -> sequence.precision)
                    .min(Comparator.comparing(ChronoUnit::getDuration))
                    .get();
        }

        private static String concatSequencePatterns(List<PatternSequence> sequences) {
            return sequences.stream().map(sequence -> sequence.pattern).collect(Collectors.joining());
        }
    }
}
