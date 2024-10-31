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

import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.util.internal.instant.InstantFormatter;
import org.apache.logging.log4j.core.util.internal.instant.InstantNumberFormatter;
import org.apache.logging.log4j.core.util.internal.instant.InstantPatternFormatter;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutDefaults;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

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
 * <p>
 * If no configuration options are provided, <tt>pattern-config</tt> is employed.
 * There {@link JsonTemplateLayoutDefaults#getTimestampFormatPattern()}, {@link JsonTemplateLayoutDefaults#getTimeZone()}, {@link JsonTemplateLayoutDefaults#getLocale()} are used as defaults for <tt>pattern</tt>, <tt>timeZone</tt>, and <tt>locale</tt>, respectively.
 * </p>
 *
 * <p>
 * In <tt>epoch-config</tt>, <tt>millis.nanos</tt>, <tt>secs.nanos</tt> stand for the fractional component in nanoseconds.
 * </p>
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

    private static EventResolver createResolver(final TemplateResolverConfig config) {
        final boolean patternProvided = config.exists("pattern");
        final boolean epochProvided = config.exists("epoch");
        if (patternProvided && epochProvided) {
            throw new IllegalArgumentException("conflicting configuration options are provided: " + config);
        }
        return epochProvided ? createEpochResolver(config) : createPatternResolver(config);
    }

    private static EventResolver createPatternResolver(final TemplateResolverConfig config) {
        final String pattern = readPattern(config);
        final TimeZone timeZone = readTimeZone(config);
        final Locale locale = config.getLocale(new String[] {"pattern", "locale"});
        final InstantFormatter formatter = InstantPatternFormatter.newBuilder()
                .setPattern(pattern)
                .setTimeZone(timeZone)
                .setLocale(locale)
                .build();
        return new PatternResolver(formatter);
    }

    private static String readPattern(final TemplateResolverConfig config) {
        final String format = config.getString(new String[] {"pattern", "format"});
        return format != null ? format : JsonTemplateLayoutDefaults.getTimestampFormatPattern();
    }

    private static TimeZone readTimeZone(final TemplateResolverConfig config) {
        final String timeZoneId = config.getString(new String[] {"pattern", "timeZone"});
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
            throw new IllegalArgumentException("invalid timestamp time zone: " + config);
        }
        return TimeZone.getTimeZone(timeZoneId);
    }

    private static final class PatternResolver implements EventResolver {

        private final InstantFormatter formatter;

        private PatternResolver(final InstantFormatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
            jsonWriter.writeString(formatter::formatTo, logEvent.getInstant());
        }
    }

    private static EventResolver createEpochResolver(final TemplateResolverConfig config) {
        final String unit = config.getString(new String[] {"epoch", "unit"});
        final Boolean rounded = config.getBoolean(new String[] {"epoch", "rounded"});
        if ("nanos".equals(unit) && !Boolean.FALSE.equals(rounded)) {
            return EPOCH_NANOS_RESOLVER;
        } else if ("millis".equals(unit)) {
            return !Boolean.TRUE.equals(rounded) ? EPOCH_MILLIS_RESOLVER : EPOCH_MILLIS_ROUNDED_RESOLVER;
        } else if ("millis.nanos".equals(unit) && rounded == null) {
            return EPOCH_MILLIS_NANOS_RESOLVER;
        } else if ("secs".equals(unit)) {
            return !Boolean.TRUE.equals(rounded) ? EPOCH_SECS_RESOLVER : EPOCH_SECS_ROUNDED_RESOLVER;
        } else if ("secs.nanos".equals(unit) && rounded == null) {
            return EPOCH_SECS_NANOS_RESOLVER;
        }
        throw new IllegalArgumentException("invalid epoch configuration: " + config);
    }

    private static final EventResolver EPOCH_NANOS_RESOLVER = (logEvent, jsonWriter) -> {
        final StringBuilder buffer = jsonWriter.getStringBuilder();
        final Instant instant = logEvent.getInstant();
        InstantNumberFormatter.EPOCH_NANOS.formatTo(buffer, instant);
    };

    private static final EventResolver EPOCH_MILLIS_RESOLVER = (logEvent, jsonWriter) -> {
        final StringBuilder buffer = jsonWriter.getStringBuilder();
        final Instant instant = logEvent.getInstant();
        InstantNumberFormatter.EPOCH_MILLIS.formatTo(buffer, instant);
    };

    private static final EventResolver EPOCH_MILLIS_ROUNDED_RESOLVER = (logEvent, jsonWriter) -> {
        final StringBuilder buffer = jsonWriter.getStringBuilder();
        final Instant instant = logEvent.getInstant();
        InstantNumberFormatter.EPOCH_MILLIS_ROUNDED.formatTo(buffer, instant);
    };

    private static final EventResolver EPOCH_MILLIS_NANOS_RESOLVER = (logEvent, jsonWriter) -> {
        final StringBuilder buffer = jsonWriter.getStringBuilder();
        final Instant instant = logEvent.getInstant();
        InstantNumberFormatter.EPOCH_MILLIS_NANOS.formatTo(buffer, instant);
    };

    private static final EventResolver EPOCH_SECS_RESOLVER = (logEvent, jsonWriter) -> {
        final StringBuilder buffer = jsonWriter.getStringBuilder();
        final Instant instant = logEvent.getInstant();
        InstantNumberFormatter.EPOCH_SECONDS.formatTo(buffer, instant);
    };

    private static final EventResolver EPOCH_SECS_ROUNDED_RESOLVER = (logEvent, jsonWriter) -> {
        final StringBuilder buffer = jsonWriter.getStringBuilder();
        final Instant instant = logEvent.getInstant();
        InstantNumberFormatter.EPOCH_SECONDS_ROUNDED.formatTo(buffer, instant);
    };

    private static final EventResolver EPOCH_SECS_NANOS_RESOLVER = (logEvent, jsonWriter) -> {
        final StringBuilder buffer = jsonWriter.getStringBuilder();
        final Instant instant = logEvent.getInstant();
        InstantNumberFormatter.EPOCH_SECONDS_NANOS.formatTo(buffer, instant);
    };

    static String getName() {
        return "timestamp";
    }

    @Override
    public void resolve(final LogEvent logEvent, final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }
}
