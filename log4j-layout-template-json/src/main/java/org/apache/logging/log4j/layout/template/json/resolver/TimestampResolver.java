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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.InstantFormatter;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Timestamp resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config        = [ patternConfig | epochConfig ]
 *
 * patternConfig = "pattern" -> ( [ format ] , [ timeZone ] , [ locale ] )
 * format        = "format" -> string
 * timeZone      = "timeZone" -> string
 * locale        = "locale" -> (
 *                     language                                   |
 *                   ( language , "_" , country )                 |
 *                   ( language , "_" , country , "_" , variant )
 *                 )
 *
 * epochConfig   = "epoch" -> ( unit , [ rounded ] )
 * unit          = "unit" -> (
 *                     "nanos"         |
 *                     "millis"        |
 *                     "secs"          |
 *                     "millis.nanos"  |
 *                     "secs.nanos"    |
 *                  )
 * rounded       = "rounded" -> boolean
 * </pre>
 *
 * If no configuration options are provided, <tt>pattern-config</tt> is
 * employed. There {@link
 * JsonTemplateLayoutDefaults#getTimestampFormatPattern()}, {@link
 * JsonTemplateLayoutDefaults#getTimeZone()}, {@link
 * JsonTemplateLayoutDefaults#getLocale()} are used as defaults for
 * <tt>pattern</tt>, <tt>timeZone</tt>, and <tt>locale</tt>, respectively.
 *
 * In <tt>epoch-config</tt>, <tt>millis.nanos</tt>, <tt>secs.nanos</tt> stand
 * for the fractional component in nanoseconds.
 *
 * <h3>Examples</h3>
 *
 * <table>
 * <tr>
 *     <td>Configuration</td>
 *     <td>Output</td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp"
 * }
 *     </pre></td>
 *     <td><pre>
 * 2020-02-07T13:38:47.098+02:00
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "pattern": {
 *     "format": "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
 *     "timeZone": "UTC",
 *     "locale": "en_US"
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 * 2020-02-07T13:38:47.098Z
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "epoch": {
 *     "unit": "secs"
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 * 1581082727.982123456
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "epoch": {
 *     "unit": "secs",
 *     "rounded": true
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 * 1581082727
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "epoch": {
 *     "unit": "secs.nanos"
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 *            982123456
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "epoch": {
 *     "unit": "millis"
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 * 1581082727982.123456
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "epoch": {
 *     "unit": "millis",
 *     "rounded": true
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 * 1581082727982
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "epoch": {
 *     "unit": "millis.nanos"
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 *              123456
 *     </pre></td>
 * </tr>
 * <tr>
 *     <td><pre>
 * {
 *   "$resolver": "timestamp",
 *   "epoch": {
 *     "unit": "nanos"
 *   }
 * }
 *     </pre></td>
 *     <td><pre>
 * 1581082727982123456
 *     </pre></td>
 * </tr>
 * </table>
 */
public final class TimestampResolver implements EventResolver {

    private final EventResolver internalResolver;

    TimestampResolver(final TemplateResolverConfig config) {
        this.internalResolver = createResolver(config);
    }

    private static EventResolver createResolver(
            final TemplateResolverConfig config) {
        final boolean patternProvided = config.exists("pattern");
        final boolean epochProvided = config.exists("epoch");
        if (patternProvided && epochProvided) {
            throw new IllegalArgumentException(
                    "conflicting configuration options are provided: " + config);
        }
        return epochProvided
                ? createEpochResolver(config)
                : createPatternResolver(config);
    }

    private static final class PatternResolverContext {

        private final InstantFormatter formatter;

        private final StringBuilder lastFormattedInstantBuffer = new StringBuilder();

        private final MutableInstant lastFormattedInstant = new MutableInstant();

        private PatternResolverContext(
                final String pattern,
                final TimeZone timeZone,
                final Locale locale) {
            this.formatter = InstantFormatter
                    .newBuilder()
                    .setPattern(pattern)
                    .setTimeZone(timeZone)
                    .setLocale(locale)
                    .build();
            lastFormattedInstant.initFromEpochSecond(-1, 0);
        }

        private static PatternResolverContext fromConfig(
                final TemplateResolverConfig config) {
            final String pattern = readPattern(config);
            final TimeZone timeZone = readTimeZone(config);
            final Locale locale = config.getLocale(new String[]{"pattern", "locale"});
            return new PatternResolverContext(pattern, timeZone, locale);
        }

        private static String readPattern(final TemplateResolverConfig config) {
            final String format = config.getString(new String[]{"pattern", "format"});
            return format != null
                    ? format
                    : JsonTemplateLayoutDefaults.getTimestampFormatPattern();
        }

        private static TimeZone readTimeZone(final TemplateResolverConfig config) {
            final String timeZoneId = config.getString(new String[]{"pattern", "timeZone"});
            if (timeZoneId == null) {
                return JsonTemplateLayoutDefaults.getTimeZone();
            }
            boolean found = false;
            for (final String availableTimeZone : TimeZone.getAvailableIDs()) {
                if (availableTimeZone.equalsIgnoreCase(timeZoneId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(
                        "invalid timestamp time zone: " + config);
            }
            return TimeZone.getTimeZone(timeZoneId);
        }

    }

    private static final class PatternResolver implements EventResolver {

        private final PatternResolverContext patternResolverContext;

        private PatternResolver(final PatternResolverContext patternResolverContext) {
            this.patternResolverContext = patternResolverContext;
        }

        @Override
        public synchronized void resolve(
                final LogEvent logEvent,
                final JsonWriter jsonWriter) {

            // Format timestamp if it doesn't match the last cached one.
            final boolean instantMatching = patternResolverContext.formatter.isInstantMatching(
                    patternResolverContext.lastFormattedInstant,
                    logEvent.getInstant());
            if (!instantMatching) {

                // Format the timestamp.
                patternResolverContext.lastFormattedInstantBuffer.setLength(0);
                patternResolverContext.lastFormattedInstant.initFrom(logEvent.getInstant());
                patternResolverContext.formatter.format(
                        patternResolverContext.lastFormattedInstant,
                        patternResolverContext.lastFormattedInstantBuffer);

                // Write the formatted timestamp.
                final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
                final int startIndex = jsonWriterStringBuilder.length();
                jsonWriter.writeString(patternResolverContext.lastFormattedInstantBuffer);

                // Cache the written value.
                patternResolverContext.lastFormattedInstantBuffer.setLength(0);
                patternResolverContext.lastFormattedInstantBuffer.append(
                        jsonWriterStringBuilder,
                        startIndex,
                        jsonWriterStringBuilder.length());

            }

            // Write the cached formatted timestamp.
            else {
                jsonWriter.writeRawString(
                        patternResolverContext.lastFormattedInstantBuffer);
            }

        }

    }

    private static EventResolver createPatternResolver(
            final TemplateResolverConfig config) {
        final PatternResolverContext patternResolverContext =
                PatternResolverContext.fromConfig(config);
        return new PatternResolver(patternResolverContext);
    }

    private static EventResolver createEpochResolver(
            final TemplateResolverConfig config) {
        final String unit = config.getString(new String[]{"epoch", "unit"});
        final Boolean rounded = config.getBoolean(new String[]{"epoch", "rounded"});
        if ("nanos".equals(unit) && !Boolean.FALSE.equals(rounded)) {
            return EPOCH_NANOS_RESOLVER;
        } else if ("millis".equals(unit)) {
            return !Boolean.TRUE.equals(rounded)
                    ? EPOCH_MILLIS_RESOLVER
                    : EPOCH_MILLIS_ROUNDED_RESOLVER;
        } else if ("millis.nanos".equals(unit) && rounded == null) {
                return EPOCH_MILLIS_NANOS_RESOLVER;
        } else if ("secs".equals(unit)) {
            return !Boolean.TRUE.equals(rounded)
                    ? EPOCH_SECS_RESOLVER
                    : EPOCH_SECS_ROUNDED_RESOLVER;
        } else if ("secs.nanos".equals(unit) && rounded == null) {
            return EPOCH_SECS_NANOS_RESOLVER;
        }
        throw new IllegalArgumentException(
                "invalid epoch configuration: " + config);
    }

    private static final class EpochResolutionRecord {

        private static final int MAX_LONG_LENGTH =
                String.valueOf(Long.MAX_VALUE).length();

        private final MutableInstant instant = new MutableInstant();

        private final char[] resolution = new char[
                /* integral: */ MAX_LONG_LENGTH +
                /* dot: */ 1 +
                /* fractional: */ MAX_LONG_LENGTH];

        private int resolutionLength;

        private EpochResolutionRecord() {
            instant.initFromEpochSecond(-1, 0);
        }

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
                resolutionRecord.instant.initFrom(logEventInstant);
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

    private static final EventResolver EPOCH_NANOS_RESOLVER =
            new EpochResolver() {
                @Override
                void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                    final long nanos = epochNanos(logEventInstant);
                    jsonWriter.writeNumber(nanos);
                }
            };

    private static final EventResolver EPOCH_MILLIS_RESOLVER =
            new EpochResolver() {
                @Override
                void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                    final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
                    final long nanos = epochNanos(logEventInstant);
                    jsonWriterStringBuilder.append(nanos);
                    jsonWriterStringBuilder.insert(jsonWriterStringBuilder.length() - 6, '.');
                }
            };

    private static final EventResolver EPOCH_MILLIS_ROUNDED_RESOLVER =
            new EpochResolver() {
                @Override
                void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                    jsonWriter.writeNumber(logEventInstant.getEpochMillisecond());
                }
            };

    private static final EventResolver EPOCH_MILLIS_NANOS_RESOLVER =
            new EpochResolver() {
                @Override
                void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                    final long nanos = epochNanos(logEventInstant);
                    final long fraction = nanos % 1_000_000L;
                    jsonWriter.writeNumber(fraction);
                }
            };

    private static final EventResolver EPOCH_SECS_RESOLVER =
            new EpochResolver() {
                @Override
                void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                    final StringBuilder jsonWriterStringBuilder = jsonWriter.getStringBuilder();
                    final long nanos = epochNanos(logEventInstant);
                    jsonWriterStringBuilder.append(nanos);
                    jsonWriterStringBuilder.insert(jsonWriterStringBuilder.length() - 9, '.');
                }
            };

    private static final EventResolver EPOCH_SECS_ROUNDED_RESOLVER =
            new EpochResolver() {
                @Override
                void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                    jsonWriter.writeNumber(logEventInstant.getEpochSecond());
                }
            };

    private static final EventResolver EPOCH_SECS_NANOS_RESOLVER =
            new EpochResolver() {
                @Override
                void resolve(final Instant logEventInstant, final JsonWriter jsonWriter) {
                    jsonWriter.writeNumber(logEventInstant.getNanoOfSecond());
                }
            };

    private static long epochNanos(final Instant instant) {
        final long nanos = Math.multiplyExact(1_000_000_000L, instant.getEpochSecond());
        return Math.addExact(nanos, instant.getNanoOfSecond());
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
