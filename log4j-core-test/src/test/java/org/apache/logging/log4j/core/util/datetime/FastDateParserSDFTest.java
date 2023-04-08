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
package org.apache.logging.log4j.core.util.datetime;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Compare FastDateParser with SimpleDateFormat
 *
 * Copied from Apache Commons Lang 3 on 2016-11-16.
 */
public class FastDateParserSDFTest {

    static Stream<Arguments> data() {
        return Stream.of(
                // General Time zone tests
                Arguments.of("z yyyy", "GMT 2010", Locale.UK, true), // no offset specified, but this is allowed as a
                                                                     // TimeZone name
                Arguments.of("z yyyy", "GMT-123 2010", Locale.UK, false),
                Arguments.of("z yyyy", "GMT-1234 2010", Locale.UK, false),
                Arguments.of("z yyyy", "GMT-12:34 2010", Locale.UK, true),
                Arguments.of("z yyyy", "GMT-1:23 2010", Locale.UK, true),
                // RFC 822 tests
                Arguments.of("z yyyy", "-1234 2010", Locale.UK, true),
                Arguments.of("z yyyy", "-12:34 2010", Locale.UK, false),
                Arguments.of("z yyyy", "-123 2010", Locale.UK, false),
                // year tests
                Arguments.of("MM/dd/yyyy", "01/11/12", Locale.UK, true),
                Arguments.of("MM/dd/yy", "01/11/12", Locale.UK, true),

                // LANG-1089
                Arguments.of("HH", "00", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "00", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "00", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "00", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "01", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "01", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "01", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "01", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "11", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "11", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "11", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "11", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "12", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "12", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "12", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "12", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "13", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "13", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "13", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "13", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "23", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "23", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "23", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "23", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "24", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "24", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "24", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "24", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "25", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "25", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "25", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "25", Locale.UK, true), // Hour in day (1-24), i.e. midnight is 24, not 0

                Arguments.of("HH", "48", Locale.UK, true), // Hour in day (0-23)
                Arguments.of("KK", "48", Locale.UK, true), // Hour in am/pm (0-11)
                Arguments.of("hh", "48", Locale.UK, true), // Hour in am/pm (1-12), i.e. midday/midnight is 12, not 0
                Arguments.of("kk", "48", Locale.UK, true) // Hour in day (1-24), i.e. midnight is 24, not 0
        );
    }

    private final TimeZone timeZone = TimeZone.getDefault();


    private void checkParse(final String formattedDate, String format, String input, Locale locale, boolean valid) {
        final SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
        sdf.setTimeZone(timeZone);
        final DateParser fdf = new FastDateParser(format, timeZone, locale);
        Date expectedTime=null;
        Class<?> sdfE = null;
        try {
            expectedTime = sdf.parse(formattedDate);
            if (!valid) {
                // Error in test data
                throw new RuntimeException("Test data error: expected SDF parse to fail, but got " + expectedTime);
            }
        } catch (final ParseException e) {
            if (valid) {
                // Error in test data
                throw new RuntimeException("Test data error: expected SDF parse to succeed, but got " + e);
            }
            sdfE = e.getClass();
        }
        Date actualTime = null;
        Class<?> fdfE = null;
        try {
            actualTime = fdf.parse(formattedDate);
            if (!valid) {
                // failure in test
                fail("Expected FDP parse to fail, but got " + actualTime);
            }
        } catch (final ParseException e) {
            if (valid) {
                // failure in test
                fail("Expected FDP parse to succeed, but got " + e);
            }
            fdfE = e.getClass();
        }
        if (valid) {
            assertEquals(expectedTime, actualTime, locale.toString() + " " + formattedDate + "\n");
        } else {
            assertEquals(sdfE, fdfE, locale.toString() + " " + formattedDate + " expected same Exception ");
        }
    }

    private void checkParsePosition(final String formattedDate, String format, String input, Locale locale,
            boolean valid) {
        final SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
        sdf.setTimeZone(timeZone);
        final DateParser fdf = new FastDateParser(format, timeZone, locale);

        final ParsePosition sdfP = new ParsePosition(0);
        final Date expectedTime = sdf.parse(formattedDate, sdfP);
        final int sdferrorIndex = sdfP.getErrorIndex();
        if (valid) {
            assertEquals(-1, sdferrorIndex, "Expected SDF error index -1 ");
            final int endIndex = sdfP.getIndex();
            final int length = formattedDate.length();
            if (endIndex != length) {
                // Error in test data
                throw new RuntimeException("Test data error: expected SDF parse to consume entire string; endindex " + endIndex + " != " + length);
            }
        } else {
            final int errorIndex = sdfP.getErrorIndex();
            if (errorIndex == -1) {
                throw new RuntimeException("Test data error: expected SDF parse to fail, but got " + expectedTime);
            }
        }

        final ParsePosition fdfP = new ParsePosition(0);
        final Date actualTime = fdf.parse(formattedDate, fdfP);
        final int fdferrorIndex = fdfP.getErrorIndex();
        if (valid) {
            assertEquals(-1, fdferrorIndex, "Expected FDF error index -1 ");
            final int endIndex = fdfP.getIndex();
            final int length = formattedDate.length();
            assertEquals(length, endIndex, "Expected FDF to parse full string " + fdfP);
            assertEquals(expectedTime, actualTime, locale.toString() + " " + formattedDate + "\n");
        } else {
            assertNotEquals(-1, fdferrorIndex, "Test data error: expected FDF parse to fail, but got " + actualTime);
            assertTrue(sdferrorIndex - fdferrorIndex <= 4,
                    "FDF error index (" + fdferrorIndex + ") should approxiamate SDF index (" + sdferrorIndex + ")");
        }
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testLowerCase(String format, String input, Locale locale, boolean valid) throws Exception {
        checkParse(input.toLowerCase(locale), format, input, locale, valid);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testLowerCasePP(String format, String input, Locale locale, boolean valid) throws Exception {
        checkParsePosition(input.toLowerCase(locale), format, input, locale, valid);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testOriginal(String format, String input, Locale locale, boolean valid) throws Exception {
        checkParse(input, format, input, locale, valid);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testOriginalPP(String format, String input, Locale locale, boolean valid) throws Exception {
        checkParsePosition(input, format, input, locale, valid);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testUpperCase(String format, String input, Locale locale, boolean valid) throws Exception {
        checkParse(input.toUpperCase(locale), format, input, locale, valid);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testUpperCasePP(String format, String input, Locale locale, boolean valid) throws Exception {
        checkParsePosition(input.toUpperCase(locale), format, input, locale, valid);
    }
}
