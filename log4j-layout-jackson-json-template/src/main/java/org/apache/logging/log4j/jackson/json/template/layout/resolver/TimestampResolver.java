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
import org.apache.logging.log4j.jackson.json.template.layout.util.JsonGenerators;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TimestampResolver implements EventResolver {

    private final EventResolver internalResolver;

    TimestampResolver(final EventResolverContext context, final String key) {
        this.internalResolver = createInternalResolver(context, key);
    }

    private static EventResolver createInternalResolver(
            final EventResolverContext eventResolverContext,
            final String key) {

        // Fallback to date-time formatting if key is empty.
        if (key == null || key.isEmpty()) {
            return createFormatResolver(eventResolverContext);
        }

        // Parse key.
        final Matcher matcher =
                Pattern.compile("^epoch(:divisor=([^,]+)(,integral)?)?$").matcher(key);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unknown key: " + key);
        }

        // Otherwise, read fields.
        final String divisorString = matcher.group(2);
        final String integralString = matcher.group(3);
        double divisor;
        boolean integral;
        if (divisorString == null) {
            divisor = 1e0;
            integral = false;
        } else {

            // Read divisor.
            try {
                divisor = Double.parseDouble(divisorString);
            } catch (final NumberFormatException error) {
                throw new IllegalArgumentException("invalid divisor: " + divisorString, error);
            }
            if (Double.compare(0D, divisor) == 0) {
                throw new IllegalArgumentException("invalid divisor: " + divisorString);
            }

            // Read integral.
            integral = integralString != null;

        }

        // Create the divisor resolver.
        return createDivisorResolver(divisor, integral);

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

        private static FormatResolverContext fromEventResolverContext(
                final EventResolverContext eventResolverContext) {
            return new FormatResolverContext(
                    eventResolverContext.getTimeZone(),
                    eventResolverContext.getLocale(),
                    eventResolverContext.getTimestampFormat());
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

        private ThreadLocalFormatResolver(final EventResolverContext eventResolverContext) {
            this.formatResolverContextRef = ThreadLocal.withInitial(
                    () -> FormatResolverContext.fromEventResolverContext(eventResolverContext));
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

        private LockingFormatResolver(final EventResolverContext eventResolverContext) {
            this.formatResolverContext =
                    FormatResolverContext.fromEventResolverContext(eventResolverContext);
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

    private static EventResolver createFormatResolver(
            final EventResolverContext eventResolverContext) {
        return Constants.ENABLE_THREADLOCALS
                ? new ThreadLocalFormatResolver(eventResolverContext)
                : new LockingFormatResolver(eventResolverContext);
    }

    private static final EventResolver SECS_LONG_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final long epochSecs = logEventInstant.getEpochSecond();
                jsonGenerator.writeNumber(epochSecs);
            };

    private static final EventResolver SECS_DOUBLE_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                JsonGenerators.writeDouble(
                        jsonGenerator,
                        logEventInstant.getEpochSecond(),
                        logEventInstant.getNanoOfSecond());
            };

    private static final EventResolver MILLIS_LONG_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final long epochMillis = logEventInstant.getEpochMillisecond();
                jsonGenerator.writeNumber(epochMillis);
            };

    private static final EventResolver MILLIS_DOUBLE_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                JsonGenerators.writeDouble(
                        jsonGenerator,
                        logEventInstant.getEpochMillisecond(),
                        logEventInstant.getNanoOfMillisecond());
            };

    private static final EventResolver NANOS_RESOLVER =
            (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                final long epochNanos = Math.multiplyExact(1_000_000_000L, logEventInstant.getEpochSecond());
                final long number = Math.addExact(epochNanos, logEventInstant.getNanoOfSecond());
                jsonGenerator.writeNumber(number);
            };

    private static EventResolver createDivisorResolver(
            final double divisor,
            final boolean integral) {

        // Resolve to seconds?
        if (Double.compare(1e9D, divisor) == 0) {
            return integral
                    ? SECS_LONG_RESOLVER
                    : SECS_DOUBLE_RESOLVER;
        }

        // Resolve to milliseconds?
        else if (Double.compare(1e6D, divisor) == 0) {
            return integral
                    ? MILLIS_LONG_RESOLVER
                    : MILLIS_DOUBLE_RESOLVER;
        }

        // Resolve to nanoseconds?
        else if (Double.compare(1e0D, divisor) == 0) {
            return NANOS_RESOLVER;
        }

        // Unknown divisor, divide as is then.
        else {
            return (final LogEvent logEvent, final JsonGenerator jsonGenerator) -> {
                final Instant logEventInstant = logEvent.getInstant();
                double quotient =
                        // According to Herbie[1], transforming ((x * 1e9) + y) / z
                        // equation to 1e9 * (x / z) + y / z reduces the average
                        // error error from 0.7 to 0.3, yay!
                        // [1] http://herbie.uwplse.org
                        1e9F * (logEventInstant.getEpochSecond() / divisor) +
                                logEventInstant.getNanoOfSecond() / divisor;
                if (integral) {
                    final long integralQuotient = (long) quotient;
                    jsonGenerator.writeNumber(integralQuotient);
                } else {
                    jsonGenerator.writeNumber(quotient);
                }
            };
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
