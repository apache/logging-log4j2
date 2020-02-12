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
package org.apache.logging.log4j.jackson.json.template.layout.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.internal.format.FastDateFormat;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.jackson.json.template.layout.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.jackson.json.template.layout.util.JsonGenerators;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class TimestampResolver implements EventResolver {

    private final EventResolver internalResolver;

    TimestampResolver(final String key) {
        this.internalResolver = createInternalResolver(key);
    }

    private static EventResolver createInternalResolver(final String key) {
        return (key != null && key.startsWith("epoch:"))
                ? createEpochResolver(key)
                : createFormatResolver(key);
    }

    /**
     * Context for GC-free formatted timestamp resolvers.
     */
    private static final class FormatResolverContext {

        private final FastDateFormat timestampFormat;

        private final Calendar calendar;

        private final StringBuilder formattedTimestampBuilder;

        private char[] formattedTimestampBuffer;

        private FormatResolverContext(
                final TimeZone timeZone,
                final Locale locale,
                final FastDateFormat timestampFormat) {
            this.timestampFormat = timestampFormat;
            this.formattedTimestampBuilder = new StringBuilder();
            this.calendar = Calendar.getInstance(timeZone, locale);
            timestampFormat.format(calendar, formattedTimestampBuilder);
            final int formattedTimestampLength = formattedTimestampBuilder.length();
            this.formattedTimestampBuffer = new char[formattedTimestampLength];
            formattedTimestampBuilder.getChars(0, formattedTimestampLength, formattedTimestampBuffer, 0);
        }

        private static FormatResolverContext fromKey(final String key) {
            String pattern = JsonTemplateLayoutDefaults.getTimestampFormatPattern();
            boolean patternProvided = false;
            TimeZone timeZone = JsonTemplateLayoutDefaults.getTimeZone();
            boolean timeZoneProvided = false;
            Locale locale = JsonTemplateLayoutDefaults.getLocale();
            boolean localeProvided = false;
            final String[] pairs = key != null
                    ? key.split("\\s*,\\s*", 3)
                    : new String[0];
            for (final String pair : pairs) {
                final String[] nameAndValue = pair.split("\\s*=\\s*", 2);
                if (nameAndValue.length != 2) {
                    throw new IllegalArgumentException("illegal timestamp key: " + key);
                }
                final String name = nameAndValue[0];
                final String value = nameAndValue[1];
                switch (name) {

                    case "pattern": {
                        if (patternProvided) {
                            throw new IllegalArgumentException(
                                    "multiple occurrences of pattern in timestamp key: " + key);
                        }
                        try {
                            FastDateFormat.getInstance(value);
                        } catch (final IllegalArgumentException error) {
                            throw new IllegalArgumentException(
                                    "invalid pattern in timestamp key: " + key,
                                    error);
                        }
                        patternProvided = true;
                        pattern = value;
                        break;
                    }

                    case "timeZone": {
                        if (timeZoneProvided) {
                            throw new IllegalArgumentException(
                                    "multiple occurrences of time zone in timestamp key: " + key);
                        }
                        boolean found = false;
                        for (final String availableTimeZone : TimeZone.getAvailableIDs()) {
                            if (availableTimeZone.equalsIgnoreCase(value)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            throw new IllegalArgumentException(
                                    "invalid time zone in timestamp key: " + key);
                        }
                        timeZoneProvided = true;
                        timeZone = TimeZone.getTimeZone(value);
                        break;
                    }

                    case "locale": {
                        if (localeProvided) {
                            throw new IllegalArgumentException(
                                    "multiple occurrences of locale in timestamp key: " + key);
                        }
                        final String[] localeFields = value.split("_", 3);
                        switch (localeFields.length) {
                            case 1:
                                locale = new Locale(localeFields[0]);
                                break;
                            case 2:
                                locale = new Locale(localeFields[0], localeFields[1]);
                                break;
                            case 3:
                                locale = new Locale(localeFields[0], localeFields[1], localeFields[2]);
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "invalid locale in timestamp key: " + key);
                        }
                        localeProvided = true;
                        break;
                    }

                    default:
                        throw new IllegalArgumentException(
                                "invalid timestamp key: " + key);

                }
            }
            final FastDateFormat fastDateFormat =
                    FastDateFormat.getInstance(pattern, timeZone, locale);
            return new FormatResolverContext(timeZone, locale, fastDateFormat);
        }

    }

    /**
     * GC-free formatted timestamp resolver.
     */
    private static abstract class ContextualFormatResolver implements EventResolver {

        abstract FormatResolverContext acquireContext();

        abstract void releaseContext();

        @Override
        public void resolve(
                final LogEvent logEvent,
                final JsonGenerator jsonGenerator)
                throws IOException {
            final long timestampMillis = logEvent.getTimeMillis();
            final FormatResolverContext formatResolverContext = acquireContext();
            try {

                // Format timestamp if it doesn't match the last cached one.
                if (formatResolverContext.calendar.getTimeInMillis() != timestampMillis) {
                    formatResolverContext.formattedTimestampBuilder.setLength(0);
                    formatResolverContext.calendar.setTimeInMillis(timestampMillis);
                    formatResolverContext.timestampFormat.format(
                            formatResolverContext.calendar,
                            formatResolverContext.formattedTimestampBuilder);
                    final int formattedTimestampLength = formatResolverContext.formattedTimestampBuilder.length();
                    if (formattedTimestampLength > formatResolverContext.formattedTimestampBuffer.length) {
                        formatResolverContext.formattedTimestampBuffer = new char[formattedTimestampLength];
                    }
                    formatResolverContext.formattedTimestampBuilder.getChars(
                            0,
                            formattedTimestampLength,
                            formatResolverContext.formattedTimestampBuffer,
                            0);
                }

                // Write the formatted timestamp.
                jsonGenerator.writeString(
                        formatResolverContext.formattedTimestampBuffer,
                        0,
                        formatResolverContext.formattedTimestampBuilder.length());

            } finally {
                releaseContext();
            }
        }

    }

    /**
     * GC-free formatted timestamp resolver by means of thread locals.
     */
    private static final class ThreadLocalFormatResolver extends ContextualFormatResolver {

        private final ThreadLocal<FormatResolverContext> formatResolverContextRef;

        private ThreadLocalFormatResolver(final String key) {
            this.formatResolverContextRef = ThreadLocal.withInitial(
                    () -> FormatResolverContext.fromKey(key));
        }

        @Override
        FormatResolverContext acquireContext() {
            return formatResolverContextRef.get();
        }

        @Override
        void releaseContext() {}

    }

    /**
     * GC-free formatted timestamp resolver by means of a shared context.
     */
    private static final class LockingFormatResolver extends ContextualFormatResolver {

        private final FormatResolverContext formatResolverContext;

        private final Lock lock = new ReentrantLock();

        private LockingFormatResolver(final String key) {
            this.formatResolverContext = FormatResolverContext.fromKey(key);
        }

        @Override
        FormatResolverContext acquireContext() {
            lock.lock();
            return formatResolverContext;
        }

        @Override
        void releaseContext() {
            lock.unlock();
        }

    }

    private static EventResolver createFormatResolver(final String key) {
        return Constants.ENABLE_THREADLOCALS
                ? new ThreadLocalFormatResolver(key)
                : new LockingFormatResolver(key);
    }

    private static final int MICROS_PER_SEC = 1_000_000;

    private static final int NANOS_PER_SEC = 1_000_000_000;

    private static final int NANOS_PER_MILLI = 1_000_000;

    private static final int NANOS_PER_MICRO = 1_000;

    private static final EventResolver EPOCH_NANOS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final long nanos = epochNanos(logEvent.getInstant());
                jsonGenerator.writeNumber(nanos);
            };

    private static final EventResolver EPOCH_MICROS_DOUBLE_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final long secs = logEventInstant.getEpochSecond();
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final long micros = MICROS_PER_SEC * secs + nanosOfSecs / NANOS_PER_MICRO;
                final int nanosOfMicros = nanosOfSecs - nanosOfSecs % NANOS_PER_MICRO;
                JsonGenerators.writeDouble(jsonGenerator, micros, nanosOfMicros);
            };

    private static final EventResolver EPOCH_MICROS_LONG_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final long nanos = epochNanos(logEvent.getInstant());
                final long micros = nanos / NANOS_PER_MICRO;
                jsonGenerator.writeNumber(micros);
            };

    private static final EventResolver EPOCH_MILLIS_DOUBLE_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                JsonGenerators.writeDouble(
                        jsonGenerator,
                        logEventInstant.getEpochMillisecond(),
                        logEventInstant.getNanoOfMillisecond());
            };

    private static final EventResolver EPOCH_MILLIS_LONG_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final long nanos = epochNanos(logEvent.getInstant());
                final long millis = nanos / NANOS_PER_MILLI;
                jsonGenerator.writeNumber(millis);
            };

    private static final EventResolver EPOCH_SECS_DOUBLE_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                JsonGenerators.writeDouble(
                        jsonGenerator,
                        logEventInstant.getEpochSecond(),
                        logEventInstant.getNanoOfSecond());
            };

    private static final EventResolver EPOCH_SECS_LONG_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final long epochSecs = logEventInstant.getEpochSecond();
                jsonGenerator.writeNumber(epochSecs);
            };

    private static final EventResolver EPOCH_MICROS_NANOS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final int nanosOfMicros = nanosOfSecs % NANOS_PER_MICRO;
                jsonGenerator.writeNumber(nanosOfMicros);
            };

    private static final EventResolver EPOCH_MILLIS_NANOS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                jsonGenerator.writeNumber(logEventInstant.getNanoOfMillisecond());
            };

    private static final EventResolver EPOCH_MILLIS_MICROS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final int nanosOfMillis = logEventInstant.getNanoOfMillisecond();
                final int microsOfMillis = nanosOfMillis / NANOS_PER_MICRO;
                jsonGenerator.writeNumber(microsOfMillis);
            };

    private static final EventResolver EPOCH_SECS_NANOS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                jsonGenerator.writeNumber(logEventInstant.getNanoOfSecond());
            };

    private static final EventResolver EPOCH_SECS_MICROS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final int microsOfSecs = nanosOfSecs / NANOS_PER_MICRO;
                jsonGenerator.writeNumber(microsOfSecs);
            };

    private static final EventResolver EPOCH_SECS_MILLIS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final int millisOfSecs = nanosOfSecs / NANOS_PER_MILLI;
                jsonGenerator.writeNumber(millisOfSecs);
            };

    private static long epochNanos(Instant instant) {
        return NANOS_PER_SEC * instant.getEpochSecond() + instant.getNanoOfSecond();
    }

    private static EventResolver createEpochResolver(final String key) {
        switch (key) {
            case "epoch:nanos":
                return EPOCH_NANOS_RESOLVER;
            case "epoch:micros":
                return EPOCH_MICROS_DOUBLE_RESOLVER;
            case "epoch:micros,integral":
                return EPOCH_MICROS_LONG_RESOLVER;
            case "epoch:millis":
                return EPOCH_MILLIS_DOUBLE_RESOLVER;
            case "epoch:millis,integral":
                return EPOCH_MILLIS_LONG_RESOLVER;
            case "epoch:secs":
                return EPOCH_SECS_DOUBLE_RESOLVER;
            case "epoch:secs,integral":
                return EPOCH_SECS_LONG_RESOLVER;
            case "epoch:micros.nanos":
                return EPOCH_MICROS_NANOS_RESOLVER;
            case "epoch:millis.nanos":
                return EPOCH_MILLIS_NANOS_RESOLVER;
            case "epoch:millis.micros":
                return EPOCH_MILLIS_MICROS_RESOLVER;
            case "epoch:secs.nanos":
                return EPOCH_SECS_NANOS_RESOLVER;
            case "epoch:secs.micros":
                return EPOCH_SECS_MICROS_RESOLVER;
            case "epoch:secs.millis":
                return EPOCH_SECS_MILLIS_RESOLVER;
            default:
                throw new IllegalArgumentException(
                        "was expecting an epoch key, found: " + key);
        }
    }

    static String getName() {
        return "timestamp";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonGenerator jsonGenerator)
            throws IOException {
        internalResolver.resolve(logEvent, jsonGenerator);
    }

}
