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
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
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
                final ChronoUnit precision = formatters.stream()
                        .map(InstantFormatter::getPrecision)
                        .min(Comparator.comparing(ChronoUnit::getDuration))
                        .get();
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
                        sequence = new SecondPatternSequence(true, "", 0);
                        break;
                    case 'S':
                        sequence = new SecondPatternSequence(false, "", sequenceContent.length());
                        break;
                    default:
                        sequence = new DateTimeFormatterPatternSequence(sequenceContent);
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
     * and the three implementations ({@link DateTimeFormatterPatternSequence}, {@link StaticPatternSequence} and
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

    abstract static class AbstractFormatter implements InstantPatternFormatter {

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
            if (other instanceof DateTimeFormatterPatternSequence) {
                final DateTimeFormatterPatternSequence otherDtf = (DateTimeFormatterPatternSequence) other;
                return new DateTimeFormatterPatternSequence(this.pattern + otherDtf.pattern, otherDtf.precision);
            }
            return null;
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
    }

    /**
     * Creates formatters that use {@link DateTimeFormatter}.
     */
    static final class DateTimeFormatterPatternSequence extends PatternSequence {

        /**
         * @param singlePattern A {@link DateTimeFormatter} pattern containing a single letter.
         */
        DateTimeFormatterPatternSequence(final String singlePattern) {
            this(singlePattern, patternPrecision(singlePattern));
        }

        /**
         * @param pattern Any {@link DateTimeFormatter} pattern.
         * @param precision The maximum interval of time over which this pattern is constant.
         */
        DateTimeFormatterPatternSequence(final String pattern, final ChronoUnit precision) {
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
            if (other instanceof DateTimeFormatterPatternSequence) {
                final DateTimeFormatterPatternSequence otherDtf = (DateTimeFormatterPatternSequence) other;
                if (isConstantForDurationOf(thresholdPrecision)
                        == otherDtf.isConstantForDurationOf(thresholdPrecision)) {
                    ChronoUnit precision = this.precision.getDuration().compareTo(otherDtf.precision.getDuration()) < 0
                            ? this.precision
                            : otherDtf.precision;
                    return new DateTimeFormatterPatternSequence(this.pattern + otherDtf.pattern, precision);
                }
            }
            // We merge a static pattern factory
            if (other instanceof StaticPatternSequence) {
                final StaticPatternSequence otherStatic = (StaticPatternSequence) other;
                return new DateTimeFormatterPatternSequence(this.pattern + otherStatic.pattern, this.precision);
            }
            return null;
        }

        /**
         * @param simplePattern a single-letter directive simplePattern complying (e.g., {@code H}, {@code HH}, or {@code pHH})
         * @return the time precision of the directive
         */
        private static ChronoUnit patternPrecision(final String simplePattern) {

            validateContent(simplePattern);
            final String paddingRemovedContent = removePadding(simplePattern);

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

            final String message = String.format("unrecognized pattern: `%s`", simplePattern);
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

        private final boolean printSeconds;
        private final String separator;
        private final int fractionalDigits;

        SecondPatternSequence(boolean printSeconds, String separator, int fractionalDigits) {
            super(
                    createPattern(printSeconds, separator, fractionalDigits),
                    determinePrecision(printSeconds, fractionalDigits));
            this.printSeconds = printSeconds;
            this.separator = separator;
            this.fractionalDigits = fractionalDigits;
        }

        private static String createPattern(boolean printSeconds, String separator, int fractionalDigits) {
            StringBuilder builder = new StringBuilder();
            if (printSeconds) {
                builder.append("ss");
            }
            builder.append(StaticPatternSequence.escapeLiteral(separator));
            if (fractionalDigits > 0) {
                builder.append(Strings.repeat("S", fractionalDigits));
            }
            return builder.toString();
        }

        private static ChronoUnit determinePrecision(boolean printSeconds, int digits) {
            return digits > 6
                    ? ChronoUnit.NANOS
                    : digits > 3
                            ? ChronoUnit.MICROS
                            : digits > 0 ? ChronoUnit.MILLIS : printSeconds ? ChronoUnit.SECONDS : ChronoUnit.FOREVER;
        }

        private static void formatSeconds(StringBuilder buffer, Instant instant) {
            long secondsInMinute = instant.getEpochSecond() % 60L;
            buffer.append((char) ((secondsInMinute / 10L) + '0'));
            buffer.append((char) ((secondsInMinute % 10L) + '0'));
        }

        private void formatFractionalDigits(StringBuilder buffer, Instant instant) {
            final int offset = buffer.length();
            buffer.setLength(offset + fractionalDigits);
            long value = instant.getNanoOfSecond();
            int valuePrecision = 9;
            // Skip digits beyond the requested precision
            while (fractionalDigits < valuePrecision) {
                valuePrecision--;
                value = value / 10L;
            }
            // Print the digits
            while (0 < valuePrecision--) {
                buffer.setCharAt(offset + valuePrecision, (char) ('0' + value % 10L));
                value = value / 10L;
            }
        }

        @Override
        InstantPatternFormatter createFormatter(Locale locale, TimeZone timeZone) {
            if (!printSeconds) {
                return new AbstractFormatter(pattern, locale, timeZone, precision) {
                    @Override
                    public void formatTo(StringBuilder buffer, Instant instant) {
                        buffer.append(separator);
                        formatFractionalDigits(buffer, instant);
                    }
                };
            }
            if (fractionalDigits == 0) {
                return new AbstractFormatter(pattern, locale, timeZone, precision) {
                    @Override
                    public void formatTo(StringBuilder buffer, Instant instant) {
                        formatSeconds(buffer, instant);
                        buffer.append(separator);
                    }
                };
            }
            return new AbstractFormatter(pattern, locale, timeZone, precision) {
                @Override
                public void formatTo(StringBuilder buffer, Instant instant) {
                    formatSeconds(buffer, instant);
                    buffer.append(separator);
                    formatFractionalDigits(buffer, instant);
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
                            printSeconds, this.separator + staticOther.literal, fractionalDigits);
                }
            }
            // We can always append more fractional digits
            if (other instanceof SecondPatternSequence) {
                SecondPatternSequence secondOther = (SecondPatternSequence) other;
                if (!secondOther.printSeconds && secondOther.separator.isEmpty()) {
                    return new SecondPatternSequence(
                            printSeconds, this.separator, this.fractionalDigits + secondOther.fractionalDigits);
                }
            }
            return null;
        }
    }
}
