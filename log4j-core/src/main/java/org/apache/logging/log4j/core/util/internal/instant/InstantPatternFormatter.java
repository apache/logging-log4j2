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
import static org.apache.logging.log4j.util.Strings.isBlank;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Contract for formatting {@link Instant}s using a date and time formatting pattern.
 * <h2>Internal usage only!</h2>
 * <p>
 * This class is intended only for internal Log4j usage.
 * <b>Log4j users should not use this class!</b>
 * This class is not subject to any backward compatibility concerns.
 * </p>
 *
 * @since 2.25.0
 */
public interface InstantPatternFormatter extends InstantFormatter {

    boolean LEGACY_FORMATTERS_ENABLED =
            "legacy".equalsIgnoreCase(PropertiesUtil.getProperties().getStringProperty("log4j2.instant.formatter"));

    String getPattern();

    Locale getLocale();

    TimeZone getTimeZone();

    static Builder newBuilder() {
        return new Builder();
    }

    final class Builder {

        private String pattern;

        private Locale locale = Locale.getDefault();

        private TimeZone timeZone = TimeZone.getDefault();

        private boolean cachingEnabled = Constants.ENABLE_THREADLOCALS;

        private boolean legacyFormattersEnabled = LEGACY_FORMATTERS_ENABLED;

        private Builder() {}

        public String getPattern() {
            return pattern;
        }

        public Builder setPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Locale getLocale() {
            return locale;
        }

        public Builder setLocale(final Locale locale) {
            this.locale = locale;
            return this;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public Builder setTimeZone(final TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public boolean isCachingEnabled() {
            return cachingEnabled;
        }

        public Builder setCachingEnabled(boolean cachingEnabled) {
            this.cachingEnabled = cachingEnabled;
            return this;
        }

        public boolean isLegacyFormattersEnabled() {
            return legacyFormattersEnabled;
        }

        public Builder setLegacyFormattersEnabled(boolean legacyFormattersEnabled) {
            this.legacyFormattersEnabled = legacyFormattersEnabled;
            return this;
        }

        public InstantPatternFormatter build() {

            // Validate arguments
            requireNonNull(locale, "locale");
            requireNonNull(timeZone, "timeZone");

            // Return a literal formatter if the pattern is blank
            if (isBlank(pattern)) {
                return createLiteralFormatter(pattern, locale, timeZone);
            }

            // Return legacy formatters, if requested
            if (legacyFormattersEnabled) {
                return new InstantPatternLegacyFormatter(pattern, locale, timeZone);
            }

            // Create the formatter, and return it, if caching is disabled
            final InstantPatternDynamicFormatter formatter =
                    new InstantPatternDynamicFormatter(pattern, locale, timeZone);
            if (!cachingEnabled) {
                return formatter;
            }

            // Wrap the formatter with caching, if necessary
            switch (formatter.getPrecision()) {

                // It is not worth caching when a precision equal to or higher than microsecond is requested
                case NANOS:
                case MICROS:
                    return formatter;

                // Millisecond precision cache
                case MILLIS:
                    return InstantPatternThreadLocalCachedFormatter.ofMilliPrecision(formatter);

                // Cache everything else with second precision
                default:
                    return InstantPatternThreadLocalCachedFormatter.ofSecondPrecision(formatter);
            }
        }

        private static InstantPatternFormatter createLiteralFormatter(
                final String literal, final Locale locale, final TimeZone timeZone) {
            return new InstantPatternFormatter() {

                @Override
                public String getPattern() {
                    return literal;
                }

                @Override
                public Locale getLocale() {
                    return locale;
                }

                @Override
                public TimeZone getTimeZone() {
                    return timeZone;
                }

                @Override
                public ChronoUnit getPrecision() {
                    return ChronoUnit.FOREVER;
                }

                @Override
                public void formatTo(final StringBuilder buffer, final Instant instant) {
                    buffer.append(literal);
                }
            };
        }
    }
}
