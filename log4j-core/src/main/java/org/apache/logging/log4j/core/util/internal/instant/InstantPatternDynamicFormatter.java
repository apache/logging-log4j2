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
import java.util.stream.Stream;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.Strings;
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
 * <li>Precompute and cache the output for parts that are of precision lower than or equal to {@value InstantPatternDynamicFormatter#PRECISION_THRESHOLD} (i.e., {@code yyyy-MM-dd'T'HH:mm:} and {@code X}) and cache it</li>
 * <li>Upon a formatting request, combine the cached outputs with the dynamic parts (i.e., {@code ss.SSS})</li>
 * </ol>
 *
 * @since 2.25.0
 */
final class InstantPatternDynamicFormatter implements InstantPatternFormatter {

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
        final InstantPatternFormatter[] formatters = sequences.stream()
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
                .toArray(InstantPatternFormatter[]::new);

        switch (formatters.length) {

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
                return formatters[0];

            // Profiling shows that unrolling the generic loop boosts performance
            case 2:
                final InstantPatternFormatter first = formatters[0];
                final InstantPatternFormatter second = formatters[1];
                return new AbstractFormatter(
                        pattern, locale, timeZone, min(first.getPrecision(), second.getPrecision())) {
                    @Override
                    public void formatTo(StringBuilder buffer, Instant instant) {
                        first.formatTo(buffer, instant);
                        second.formatTo(buffer, instant);
                    }
                };

            // Combine all extracted formatters into one
            default:
                final ChronoUnit precision = Stream.of(formatters)
                        .map(InstantFormatter::getPrecision)
                        .min(Comparator.comparing(ChronoUnit::getDuration))
                        .get();
                return new AbstractFormatter(pattern, locale, timeZone, precision) {
                    @Override
                    public void formatTo(final StringBuilder buffer, final Instant instant) {
                        // noinspection ForLoopReplaceableByForEach (avoid iterator allocation)
                        for (int formatterIndex = 0; formatterIndex < formatters.length; formatterIndex++) {
                            final InstantPatternFormatter formatter = formatters[formatterIndex];
                            formatter.formatTo(buffer, instant);
                        }
                    }
                };
        }
    }

    private static ChronoUnit min(ChronoUnit left, ChronoUnit right) {
        return left.getDuration().compareTo(right.getDuration()) < 0 ? left : right;
    }

    static List<PatternSequence> sequencePattern(final String pattern, final ChronoUnit precisionThreshold) {
        List<PatternSequence> sequences = sequencePattern(pattern);
        return mergeFactories(sequences, precisionThreshold);
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
                final PatternSequence sequence;
                switch (c) {
                    case 's':
                        sequence = new SecondPatternSequence(sequenceContent.length(), "", 0);
                        break;
                    case 'S':
                        sequence = new SecondPatternSequence(0, "", sequenceContent.length());
                        break;
                    default:
                        sequence = new DynamicPatternSequence(sequenceContent);
                }
                sequences.add(sequence);
                startIndex = endIndex;
            }

            // Handle single-quotes
            else if (c == '\'') {
                final int endIndex = pattern.indexOf('\'', startIndex + 1);
                final PatternSequence sequence = getStaticPatternSequence(pattern, startIndex, endIndex);
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
        return sequences;
    }

    private static PatternSequence getStaticPatternSequence(String pattern, int startIndex, int endIndex) {
        if (endIndex < 0) {
            final String message = String.format(
                    "pattern ends with an incomplete string literal that started at index %d: `%s`",
                    startIndex, pattern);
            throw new IllegalArgumentException(message);
        }
        final String sequenceLiteral = (startIndex + 1) == endIndex ? "'" : pattern.substring(startIndex + 1, endIndex);
        return new StaticPatternSequence(sequenceLiteral);
    }

    private static boolean isDynamicPatternLetter(final char c) {
        return "GuyDMLdgQqYwWEecFaBhKkHmsSAnNVvzOXxZ".indexOf(c) >= 0;
    }

    /**
     * Merges pattern sequences using {@link PatternSequence#tryMerge}.
     *
     * <h2>Example</h2>
     *
     * <p>
     * For example, given the {@code yyyy-MM-dd'T'HH:mm:ss.SSS} pattern, a precision threshold of {@link ChronoUnit#MINUTES}
     * and the three implementations ({@link DynamicPatternSequence}, {@link StaticPatternSequence} and
     * {@link SecondPatternSequence}) from this class,
     * this method will combine pattern sequences associated with {@code yyyy-MM-dd'T'HH:mm:} into a single sequence,
     * since these are consecutive and effectively constant sequences.
     * </p>
     *
     * <pre>{@code
     * [
     *     dateTimeFormatter(pattern="yyyy", precision=YEARS),
     *     static(literal="-"),
     *     dateTimeFormatter(pattern="MM", precision=MONTHS),
     *     static(literal="-"),
     *     dateTimeFormatter(pattern="dd", precision=DAYS),
     *     static(literal="T"),
     *     dateTimeFormatter(pattern="HH", precision=HOURS),
     *     static(literal=":"),
     *     dateTimeFormatter(pattern="mm", precision=MINUTES),
     *     static(literal=":"),
     *     second(pattern="ss", precision=SECONDS),
     *     static(literal="."),
     *     second(pattern="SSS", precision=MILLISECONDS)
     *     dateTimeFormatter(pattern="X", precision=HOURS),
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
     *     dateTimeFormatter(pattern="yyyy-MM-dd'T'HH:mm", precision=MINUTES),
     *     second(pattern="ss.SSS", precision=MILLISECONDS),
     *     dateTimeFormatter(pattern="X", precision=MINUTES)
     * ]
     * }</pre>
     *
     * <p>
     * The resultant sequencing effectively translates to 3 {@link AbstractFormatter}s.
     * </p>
     *
     * @param sequences a list of pattern formatter factories
     * @param precisionThreshold  a precision threshold to determine effectively constant sequences
     * @return transformed sequencing, where sequences that are effectively constant or effectively dynamic are merged.
     */
    private static List<PatternSequence> mergeFactories(
            final List<PatternSequence> sequences, final ChronoUnit precisionThreshold) {
        if (sequences.size() < 2) {
            return sequences;
        }
        final List<PatternSequence> mergedSequences = new ArrayList<>();
        PatternSequence currentFactory = sequences.get(0);
        for (int i = 1; i < sequences.size(); i++) {
            PatternSequence nextFactory = sequences.get(i);
            PatternSequence mergedFactory = currentFactory.tryMerge(nextFactory, precisionThreshold);
            // The current factory cannot be merged with the next one.
            if (mergedFactory == null) {
                mergedSequences.add(currentFactory);
                currentFactory = nextFactory;
            } else {
                currentFactory = mergedFactory;
            }
        }
        mergedSequences.add(currentFactory);
        return mergedSequences;
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

        AbstractFormatter(
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

        abstract InstantPatternFormatter createFormatter(final Locale locale, final TimeZone timeZone);

        /**
         * Tries to merge two pattern sequences.
         *
         * <p>
         *     If not {@link null}, the pattern sequence returned by this method must:
         * </p>
         * <ol>
         *     <li>Have a {@link #precision}, which is the minimum of the precisions of the two merged sequences.</li>
         *     <li>
         *         Create formatters that are equivalent to the concatenation of the formatters produced by the
         *         two merged sequences.
         *     </li>
         * </ol>
         * <p>
         *     The returned pattern sequence should try to achieve these two goals:
         * </p>
         * <ol>
         *     <li>
         *         Create formatters which are faster than the concatenation of the formatters produced by the
         *         two merged sequences.
         *     </li>
         *     <li>
         *         It should be {@link null} if one of the pattern sequences is effectively constant over
         *         {@code thresholdPrecision}, but the other one is not.
         *     </li>
         * </ol>
         *
         * @param other A pattern sequence.
         * @param thresholdPrecision A precision threshold to determine effectively constant sequences.
         *                           This prevents merging effectively constant and dynamic pattern sequences.
         * @return A merged formatter factory or {@code null} if merging is not possible.
         */
        @Nullable
        PatternSequence tryMerge(PatternSequence other, ChronoUnit thresholdPrecision) {
            return null;
        }

        boolean isConstantForDurationOf(final ChronoUnit thresholdPrecision) {
            return precision.compareTo(thresholdPrecision) >= 0;
        }

        static String escapeLiteral(String literal) {
            StringBuilder sb = new StringBuilder(literal.length() + 2);
            boolean inSingleQuotes = false;
            for (int i = 0; i < literal.length(); i++) {
                char c = literal.charAt(i);
                if (c == '\'') {
                    if (inSingleQuotes) {
                        sb.append("'");
                    }
                    inSingleQuotes = false;
                    sb.append("''");
                } else {
                    if (!inSingleQuotes) {
                        sb.append("'");
                    }
                    inSingleQuotes = true;
                    sb.append(c);
                }
            }
            if (inSingleQuotes) {
                sb.append("'");
            }
            return sb.toString();
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
            return getClass().getSimpleName() + "[" + "pattern='" + pattern + '\'' + ", precision=" + precision + ']';
        }
    }

    static final class StaticPatternSequence extends PatternSequence {

        private final String literal;

        StaticPatternSequence(final String literal) {
            super(escapeLiteral(literal), ChronoUnit.FOREVER);
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

        @Override
        @Nullable
        PatternSequence tryMerge(PatternSequence other, ChronoUnit thresholdPrecision) {
            // We always merge consecutive static pattern factories
            if (other instanceof StaticPatternSequence) {
                final StaticPatternSequence otherStatic = (StaticPatternSequence) other;
                return new StaticPatternSequence(this.literal + otherStatic.literal);
            }
            // We also merge a static pattern factory with a DTF factory
            if (other instanceof DynamicPatternSequence) {
                final DynamicPatternSequence otherDtf = (DynamicPatternSequence) other;
                return new DynamicPatternSequence(this.pattern + otherDtf.pattern, otherDtf.precision);
            }
            return null;
        }
    }

    /**
     * Creates formatters that use {@link DateTimeFormatter}.
     */
    static final class DynamicPatternSequence extends PatternSequence {

        /**
         * @param singlePattern A {@link DateTimeFormatter} pattern containing a single letter.
         */
        DynamicPatternSequence(final String singlePattern) {
            this(singlePattern, patternPrecision(singlePattern));
        }

        /**
         * @param pattern Any {@link DateTimeFormatter} pattern.
         * @param precision The maximum interval of time over which this pattern is constant.
         */
        DynamicPatternSequence(final String pattern, final ChronoUnit precision) {
            super(pattern, precision);
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

        @Override
        @Nullable
        PatternSequence tryMerge(PatternSequence other, ChronoUnit thresholdPrecision) {
            // We merge two DTF factories if they are both above or below the threshold
            if (other instanceof DynamicPatternSequence) {
                final DynamicPatternSequence otherDtf = (DynamicPatternSequence) other;
                if (isConstantForDurationOf(thresholdPrecision)
                        == otherDtf.isConstantForDurationOf(thresholdPrecision)) {
                    ChronoUnit precision = this.precision.getDuration().compareTo(otherDtf.precision.getDuration()) < 0
                            ? this.precision
                            : otherDtf.precision;
                    return new DynamicPatternSequence(this.pattern + otherDtf.pattern, precision);
                }
            }
            // We merge a static pattern factory
            if (other instanceof StaticPatternSequence) {
                final StaticPatternSequence otherStatic = (StaticPatternSequence) other;
                return new DynamicPatternSequence(this.pattern + otherStatic.pattern, this.precision);
            }
            return null;
        }

        /**
         * @param singlePattern a single-letter directive singlePattern complying (e.g., {@code H}, {@code HH}, or {@code pHH})
         * @return the time precision of the directive
         */
        private static ChronoUnit patternPrecision(final String singlePattern) {

            validateContent(singlePattern);
            final String paddingRemovedContent = removePadding(singlePattern);

            if (paddingRemovedContent.matches("G+")) {
                return ChronoUnit.ERAS;
            } else if (paddingRemovedContent.matches("[uyY]+")) {
                return ChronoUnit.YEARS;
            } else if (paddingRemovedContent.matches("[MLQq]+")) {
                return ChronoUnit.MONTHS;
            } else if (paddingRemovedContent.matches("w+")) {
                return ChronoUnit.WEEKS;
            } else if (paddingRemovedContent.matches("[DdgEecFW]+")) {
                return ChronoUnit.DAYS;
            } else if (paddingRemovedContent.matches("[aBhKkH]+")) {
                return ChronoUnit.HOURS;
            } else if (paddingRemovedContent.contains("m")
                    // Time-zone directives
                    || paddingRemovedContent.matches("[ZxXOzVv]+")) {
                return ChronoUnit.MINUTES;
            } else if (paddingRemovedContent.contains("s")) {
                return ChronoUnit.SECONDS;
            }

            // 2 to 3 consequent `S` characters output millisecond precision
            else if (paddingRemovedContent.matches("S{1,3}")
                    // `A` (milli-of-day) outputs millisecond precision.
                    || paddingRemovedContent.contains("A")) {
                return ChronoUnit.MILLIS;
            }

            // 4 to 6 consequent `S` characters output microsecond precision
            else if (paddingRemovedContent.matches("S{4,6}")) {
                return ChronoUnit.MICROS;
            }

            // 7 to 9 consequent `S` characters output nanosecond precision
            else if (paddingRemovedContent.matches("S{7,9}")
                    // `n` (nano-of-second) and `N` (nano-of-day) always output nanosecond precision.
                    // This is independent of how many times they occur sequentially.
                    || paddingRemovedContent.matches("[nN]+")) {
                return ChronoUnit.NANOS;
            }

            final String message = String.format("unrecognized pattern: `%s`", singlePattern);
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

    static class SecondPatternSequence extends PatternSequence {

        private static final int[] POWERS_OF_TEN = {
            100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100, 10, 1
        };

        private final int secondDigits;
        private final String separator;
        private final int fractionalDigits;

        SecondPatternSequence(int secondDigits, String separator, int fractionalDigits) {
            super(
                    createPattern(secondDigits, separator, fractionalDigits),
                    determinePrecision(secondDigits, fractionalDigits));
            final int maxSecondDigits = 2;
            if (secondDigits > maxSecondDigits) {
                final String message = String.format(
                        "More than %d `s` pattern letters are not supported, found: %d", maxSecondDigits, secondDigits);
                throw new IllegalArgumentException(message);
            }
            final int maxFractionalDigits = 9;
            if (fractionalDigits > maxFractionalDigits) {
                final String message = String.format(
                        "More than %d `S` pattern letters are not supported, found: %d",
                        maxFractionalDigits, fractionalDigits);
                throw new IllegalArgumentException(message);
            }
            this.secondDigits = secondDigits;
            this.separator = separator;
            this.fractionalDigits = fractionalDigits;
        }

        private static String createPattern(int secondDigits, String separator, int fractionalDigits) {
            return Strings.repeat("s", secondDigits)
                    + StaticPatternSequence.escapeLiteral(separator)
                    + Strings.repeat("S", fractionalDigits);
        }

        private static ChronoUnit determinePrecision(int secondDigits, int digits) {
            if (digits > 6) return ChronoUnit.NANOS;
            if (digits > 3) return ChronoUnit.MICROS;
            if (digits > 0) return ChronoUnit.MILLIS;
            return secondDigits > 0 ? ChronoUnit.SECONDS : ChronoUnit.FOREVER;
        }

        private static void formatUnpaddedSeconds(StringBuilder buffer, Instant instant) {
            buffer.append(instant.getEpochSecond() % 60L);
        }

        private static void formatPaddedSeconds(StringBuilder buffer, Instant instant) {
            long secondsInMinute = instant.getEpochSecond() % 60L;
            buffer.append((char) ((secondsInMinute / 10L) + '0'));
            buffer.append((char) ((secondsInMinute % 10L) + '0'));
        }

        private void formatFractionalDigits(StringBuilder buffer, Instant instant) {
            int nanos = instant.getNanoOfSecond();
            // digits contain the first idx digits.
            int digits;
            // moreDigits contains the first (idx + 1) digits
            int moreDigits = 0;
            // Print the digits
            for (int idx = 0; idx < fractionalDigits; idx++) {
                digits = moreDigits;
                moreDigits = nanos / POWERS_OF_TEN[idx];
                buffer.append((char) ('0' + moreDigits - 10 * digits));
            }
        }

        private static void formatMillis(StringBuilder buffer, Instant instant) {
            int ms = instant.getNanoOfSecond() / 1_000_000;
            int cs = ms / 10;
            int ds = cs / 10;
            buffer.append((char) ('0' + ds));
            buffer.append((char) ('0' + cs - 10 * ds));
            buffer.append((char) ('0' + ms - 10 * cs));
        }

        @Override
        InstantPatternFormatter createFormatter(Locale locale, TimeZone timeZone) {
            final BiConsumer<StringBuilder, Instant> secondDigitsFormatter = secondDigits == 2
                    ? SecondPatternSequence::formatPaddedSeconds
                    : SecondPatternSequence::formatUnpaddedSeconds;
            final BiConsumer<StringBuilder, Instant> fractionDigitsFormatter =
                    fractionalDigits == 3 ? SecondPatternSequence::formatMillis : this::formatFractionalDigits;
            if (secondDigits == 0) {
                return new AbstractFormatter(pattern, locale, timeZone, precision) {
                    @Override
                    public void formatTo(StringBuilder buffer, Instant instant) {
                        buffer.append(separator);
                        fractionDigitsFormatter.accept(buffer, instant);
                    }
                };
            }
            if (fractionalDigits == 0) {
                return new AbstractFormatter(pattern, locale, timeZone, precision) {
                    @Override
                    public void formatTo(StringBuilder buffer, Instant instant) {
                        secondDigitsFormatter.accept(buffer, instant);
                        buffer.append(separator);
                    }
                };
            }
            return new AbstractFormatter(pattern, locale, timeZone, precision) {
                @Override
                public void formatTo(StringBuilder buffer, Instant instant) {
                    secondDigitsFormatter.accept(buffer, instant);
                    buffer.append(separator);
                    fractionDigitsFormatter.accept(buffer, instant);
                }
            };
        }

        @Override
        @Nullable
        PatternSequence tryMerge(PatternSequence other, ChronoUnit thresholdPrecision) {
            // If we don't have a fractional part, we can merge a literal separator
            if (other instanceof StaticPatternSequence) {
                StaticPatternSequence staticOther = (StaticPatternSequence) other;
                if (fractionalDigits == 0) {
                    return new SecondPatternSequence(
                            this.secondDigits, this.separator + staticOther.literal, fractionalDigits);
                }
            }
            // We can always append more fractional digits
            if (other instanceof SecondPatternSequence) {
                SecondPatternSequence secondOther = (SecondPatternSequence) other;
                if (secondOther.secondDigits == 0 && secondOther.separator.isEmpty()) {
                    return new SecondPatternSequence(
                            this.secondDigits, this.separator, this.fractionalDigits + secondOther.fractionalDigits);
                }
            }
            return null;
        }
    }
}
