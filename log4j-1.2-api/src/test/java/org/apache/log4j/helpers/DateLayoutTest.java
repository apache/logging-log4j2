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
package org.apache.log4j.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.log4j.Layout;
import org.apache.log4j.LayoutTest;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Tests {@link DateLayout}.
 */
public class DateLayoutTest extends LayoutTest {

    /**
     * Construct a new instance of LayoutTest.
     *
     * @param testName test name.
     */
    public DateLayoutTest(final String testName) {
        super(testName);
    }

    /**
     * Constructor for use by derived tests.
     *
     * @param testName name of test.
     * @param expectedContentType expected value for getContentType().
     * @param expectedIgnoresThrowable expected value for ignoresThrowable().
     * @param expectedHeader expected value for getHeader().
     * @param expectedFooter expected value for getFooter().
     */
    protected DateLayoutTest(
            final String testName,
            final String expectedContentType,
            final boolean expectedIgnoresThrowable,
            final String expectedHeader,
            final String expectedFooter) {
        super(testName, expectedContentType, expectedIgnoresThrowable, expectedHeader, expectedFooter);
    }

    /**
     * {@inheritDoc}
     */
    protected Layout createLayout() {
        return new MockLayout();
    }

    /**
     * Tests DateLayout.NULL_DATE_FORMAT constant.
     */
    public void testNullDateFormat() {
        assertEquals("NULL", DateLayout.NULL_DATE_FORMAT);
    }

    /**
     * Tests DateLayout.RELATIVE constant.
     */
    public void testRelativeTimeDateFormat() {
        assertEquals("RELATIVE", DateLayout.RELATIVE_TIME_DATE_FORMAT);
    }

    /**
     * Tests DateLayout.DATE_FORMAT_OPTION constant.
     *
     * @deprecated since constant is deprecated
     */
    public void testDateFormatOption() {
        assertEquals("DateFormat", DateLayout.DATE_FORMAT_OPTION);
    }

    /**
     * Tests DateLayout.TIMEZONE_OPTION constant.
     *
     * @deprecated since constant is deprecated
     */
    public void testTimeZoneOption() {
        assertEquals("TimeZone", DateLayout.TIMEZONE_OPTION);
    }

    /**
     * Tests getOptionStrings().
     *
     * @deprecated since getOptionStrings is deprecated.
     *
     */
    public void testGetOptionStrings() {
        final String[] options = ((DateLayout) createLayout()).getOptionStrings();
        assertEquals(2, options.length);
    }

    /**
     * Tests setting DateFormat through setOption method.
     *
     * @deprecated since setOption is deprecated.
     */
    public void testSetOptionDateFormat() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setOption("dAtefOrmat", "foobar");
        assertEquals("FOOBAR", layout.getDateFormat());
    }

    /**
     * Tests setting TimeZone through setOption method.
     *
     * @deprecated since setOption is deprecated.
     */
    public void testSetOptionTimeZone() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setOption("tImezOne", "+05:00");
        assertEquals("+05:00", layout.getTimeZone());
    }

    /**
     * Tests setDateFormat.
     */
    public void testSetDateFormat() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("ABSOLUTE");
        assertEquals("ABSOLUTE", layout.getDateFormat());
    }

    /**
     * Tests setTimeZone.
     */
    public void testSetTimeZone() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setTimeZone("+05:00");
        assertEquals("+05:00", layout.getTimeZone());
    }

    /**
     * Tests 2 parameter setDateFormat with null.
     */
    public void testSetDateFormatNull() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat((String) null, null);
    }

    /**
     * Tests 2 parameter setDateFormat with "NULL".
     */
    public void testSetDateFormatNullString() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("NuLL", null);
    }

    /**
     * Tests 2 parameter setDateFormat with "RELATIVE".
     */
    public void testSetDateFormatRelative() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("rElatIve", TimeZone.getDefault());
    }

    /**
     * Tests 2 parameter setDateFormat with "ABSOLUTE".
     */
    public void testSetDateFormatAbsolute() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("aBsolUte", TimeZone.getDefault());
    }

    /**
     * Tests 2 parameter setDateFormat with "DATETIME".
     */
    public void testSetDateFormatDateTime() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("dAte", TimeZone.getDefault());
    }

    /**
     * Tests 2 parameter setDateFormat with "ISO8601".
     */
    public void testSetDateFormatISO8601() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("iSo8601", TimeZone.getDefault());
    }

    /**
     * Tests 2 parameter setDateFormat with "HH:mm:ss".
     */
    public void testSetDateFormatSimple() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("HH:mm:ss", TimeZone.getDefault());
    }

    /**
     * Tests activateOptions.
     */
    public void testActivateOptions() {
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat("HH:mm:ss");
        layout.setTimeZone("+05:00");
        layout.activateOptions();
    }

    /**
     * Tests setDateFormat(DateFormat, TimeZone).
     */
    public void testSetDateFormatWithFormat() {
        final DateFormat format = new SimpleDateFormat("HH:mm");
        final DateLayout layout = (DateLayout) createLayout();
        layout.setDateFormat(format, TimeZone.getDefault());
    }

    /**
     * Tests IS08601DateFormat class.
     *
     * @deprecated since ISO8601DateFormat is deprecated
     */
    public void testISO8601Format() {
        final DateFormat format = new ISO8601DateFormat();
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(1970, 0, 1, 0, 0, 0);
        final String actual = format.format(calendar.getTime());
        assertEquals("1970-01-01 00:00:00,000", actual);
    }

    /**
     * Tests DateTimeDateFormat class.
     *
     * @deprecated since DateTimeDateFormat is deprecated
     */
    public void testDateTimeFormat() {
        final DateFormat format = new DateTimeDateFormat();
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(1970, 0, 1, 0, 0, 0);
        final String actual = format.format(calendar.getTime());
        final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss,SSS");
        final String expected = df.format(calendar.getTime());
        assertEquals(expected, actual);
    }

    /**
     * Concrete Layout class for tests.
     */
    private static final class MockLayout extends DateLayout {
        /**
         * Create new instance of MockLayout.
         */
        public MockLayout() {
            //
            // checks that protected fields are properly initialized
            assertNotNull(pos);
            assertNotNull(date);
            assertNull(dateFormat);
        }

        /**
         * {@inheritDoc}
         */
        public String format(final LoggingEvent event) {
            return "Mock";
        }

        /**
         * {@inheritDoc}
         */
        public void activateOptions() {}

        /**
         * {@inheritDoc}
         */
        public boolean ignoresThrowable() {
            return true;
        }
    }
}
