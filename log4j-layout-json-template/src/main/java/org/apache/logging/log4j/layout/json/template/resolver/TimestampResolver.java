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
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.internal.format.FastDateFormat;
import org.apache.logging.log4j.layout.json.template.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

final class TimestampResolver implements EventResolver {

    private final EventResolver internalResolver;

    TimestampResolver(final String key) {
        this.internalResolver = (key != null && key.startsWith("epoch:"))
                ? createEpochResolver(key)
                : createFormatResolver(key);
    }

    /**
     * Context for GC-free formatted timestamp resolver.
     */
    private static final class FormatResolverContext {

        private final FastDateFormat timestampFormat;

        private final Calendar calendar;

        private final StringBuilder formattedTimestampBuilder;

        private FormatResolverContext(
                final TimeZone timeZone,
                final Locale locale,
                final FastDateFormat timestampFormat) {
            this.timestampFormat = timestampFormat;
            this.formattedTimestampBuilder = new StringBuilder();
            this.calendar = Calendar.getInstance(timeZone, locale);
            timestampFormat.format(calendar, formattedTimestampBuilder);
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
    private static final class FormatResolver implements EventResolver {

        private final FormatResolverContext formatResolverContext;

        private FormatResolver(final FormatResolverContext formatResolverContext) {
            this.formatResolverContext = formatResolverContext;
        }

        @Override
        public synchronized void resolve(
                final LogEvent logEvent,
                final JsonWriter jsonWriter) {

            // Format timestamp if it doesn't match the last cached one.
            final long timestampMillis = logEvent.getTimeMillis();
            if (formatResolverContext.calendar.getTimeInMillis() != timestampMillis) {

                // Format the timestamp.
                formatResolverContext.formattedTimestampBuilder.setLength(0);
                formatResolverContext.calendar.setTimeInMillis(timestampMillis);
                formatResolverContext.timestampFormat.format(
                        formatResolverContext.calendar,
                        formatResolverContext.formattedTimestampBuilder);

                // Write the formatted timestamp.
                final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
                final int startIndex = jsonWriterStringBuilder.length();
                jsonWriter.writeString(formatResolverContext.formattedTimestampBuilder);

                // Cache the written value.
                formatResolverContext.formattedTimestampBuilder.setLength(0);
                formatResolverContext.formattedTimestampBuilder.append(
                        jsonWriterStringBuilder,
                        startIndex,
                        jsonWriterStringBuilder.length());

            }

            // Write the cached formatted timestamp.
            else {
                jsonWriter.writeRawString(
                        formatResolverContext.formattedTimestampBuilder);
            }

        }

    }

    private static EventResolver createFormatResolver(final String key) {
        final FormatResolverContext formatResolverContext =
                FormatResolverContext.fromKey(key);
        return new FormatResolver(formatResolverContext);
    }

    private static EventResolver createEpochResolver(final String key) {
        switch (key) {
            case "epoch:nanos":
                return createEpochNanosResolver();
            case "epoch:micros":
                return createEpochMicrosDoubleResolver();
            case "epoch:micros,integral":
                return createEpochMicrosLongResolver();
            case "epoch:millis":
                return createEpochMillisDoubleResolver();
            case "epoch:millis,integral":
                return createEpochMillisLongResolver();
            case "epoch:secs":
                return createEpochSecsDoubleResolver();
            case "epoch:secs,integral":
                return createEpochSecsLongResolver();
            case "epoch:micros.nanos":
                return createEpochMicrosNanosResolver();
            case "epoch:millis.nanos":
                return createEpochMillisNanosResolver();
            case "epoch:millis.micros":
                return createEpochMillisMicrosResolver();
            case "epoch:secs.nanos":
                return createEpochSecsNanosResolver();
            case "epoch:secs.micros":
                return createEpochSecsMicrosResolver();
            case "epoch:secs.millis":
                return createEpochSecsMillisResolver();
            default:
                throw new IllegalArgumentException(
                        "was expecting an epoch key: " + key);
        }
    }

    private static final int MICROS_PER_SEC = 1_000_000;

    private static final int NANOS_PER_SEC = 1_000_000_000;

    private static final int NANOS_PER_MILLI = 1_000_000;

    private static final int NANOS_PER_MICRO = 1_000;

    private static final class EpochResolutionRecord {

        private static final int MAX_LONG_LENGTH =
                String.valueOf(Long.MAX_VALUE).length();

        private Instant instant;

        private char[] resolution = new char[/* integral: */MAX_LONG_LENGTH + /* dot: */1 + /* fractional: */MAX_LONG_LENGTH ];

        private int resolutionLength;

        private EpochResolutionRecord() {}

    }

    private static abstract class EpochResolver implements EventResolver {

        private final EpochResolutionRecord resolutionRecord =
                new EpochResolutionRecord();

        @Override
        public synchronized void resolve(
                final LogEvent logEvent,
                final JsonWriter jsonWriter) {
            final Instant logEventInstant = logEvent.getInstant();
            if (logEventInstant.equals(resolutionRecord.instant)) {
                jsonWriter.writeRawString(
                        resolutionRecord.resolution,
                        0,
                        resolutionRecord.resolutionLength);
            } else {
                resolutionRecord.instant = logEventInstant;
                final StringBuilder stringBuilder = jsonWriter.getStringBuilder();
                final int startIndex = stringBuilder.length();
                resolve(logEventInstant, jsonWriter);
                resolutionRecord.resolutionLength = stringBuilder.length() - startIndex;
                stringBuilder.getChars(
                        startIndex,
                        stringBuilder.length(),
                        resolutionRecord.resolution,
                        0);
            }
        }

        abstract void resolve(Instant logEventInstant, JsonWriter jsonWriter);

    }

    private static EventResolver createEpochNanosResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                final long nanos = epochNanos(logEventInstant);
                jsonWriter.writeNumber(nanos);
            }
        };
    }

    private static EventResolver createEpochMicrosDoubleResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                final long secs = logEventInstant.getEpochSecond();
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final long micros = MICROS_PER_SEC * secs + nanosOfSecs / NANOS_PER_MICRO;
                final int nanosOfMicros = nanosOfSecs - nanosOfSecs % NANOS_PER_MICRO;
                jsonWriter.writeNumber(micros, nanosOfMicros);
            }
        };
    }

    private static EventResolver createEpochMicrosLongResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                final long nanos = epochNanos(logEventInstant);
                final long micros = nanos / NANOS_PER_MICRO;
                jsonWriter.writeNumber(micros);
            }
        };
    }

    private static EventResolver createEpochMillisDoubleResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                jsonWriter.writeNumber(
                        logEventInstant.getEpochMillisecond(),
                        logEventInstant.getNanoOfMillisecond());
            }
        };
    }

    private static EventResolver createEpochMillisLongResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                jsonWriter.writeNumber(logEventInstant.getEpochMillisecond());
            }
        };
    }

    private static EventResolver createEpochSecsDoubleResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                jsonWriter.writeNumber(
                        logEventInstant.getEpochSecond(),
                        logEventInstant.getNanoOfSecond());
            }
        };
    }

    private static EventResolver createEpochSecsLongResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                jsonWriter.writeNumber(logEventInstant.getEpochSecond());
            }
        };
    }

    private static EventResolver createEpochMicrosNanosResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final int nanosOfMicros = nanosOfSecs % NANOS_PER_MICRO;
                jsonWriter.writeNumber(nanosOfMicros);
            }
        };
    }

    private static EventResolver createEpochMillisNanosResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                jsonWriter.writeNumber(logEventInstant.getNanoOfMillisecond());
            }
        };
    }

    private static EventResolver createEpochMillisMicrosResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                final int nanosOfMillis = logEventInstant.getNanoOfMillisecond();
                final int microsOfMillis = nanosOfMillis / NANOS_PER_MICRO;
                jsonWriter.writeNumber(microsOfMillis);
            }
        };
    }

    private static EventResolver createEpochSecsNanosResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                jsonWriter.writeNumber(logEventInstant.getNanoOfSecond());
            }
        };
    }

    private static EventResolver createEpochSecsMicrosResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final int microsOfSecs = nanosOfSecs / NANOS_PER_MICRO;
                jsonWriter.writeNumber(microsOfSecs);
            }
        };
    }

    private static EventResolver createEpochSecsMillisResolver() {
        return new EpochResolver() {
            @Override
            void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                final int nanosOfSecs = logEventInstant.getNanoOfSecond();
                final int millisOfSecs = nanosOfSecs / NANOS_PER_MILLI;
                jsonWriter.writeNumber(millisOfSecs);
            }
        };
    }

    private static long epochNanos(Instant instant) {
        return NANOS_PER_SEC * instant.getEpochSecond() + instant.getNanoOfSecond();
    }

    static String getName() {
        return "timestamp";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }

}
