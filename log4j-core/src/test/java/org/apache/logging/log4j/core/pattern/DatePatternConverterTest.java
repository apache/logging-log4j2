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
package org.apache.logging.log4j.core.pattern;

import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;

import static org.junit.Assert.*;

public class DatePatternConverterTest {

    @Test
    public void testNewInstanceAllowsNullParameter() {
        DatePatternConverter.newInstance(null); // no errors
    }

    @Test
    public void testFormatLogEventStringBuilder() {
        LogEvent event = new MyLogEvent();
        DatePatternConverter converter = DatePatternConverter.newInstance(null);
        StringBuilder sb = new StringBuilder();
        converter.format(event, sb);

        String expected = "2011-12-30T10:56:35,987";
        assertEquals(expected, sb.toString());
    }

    private class MyLogEvent extends AbstractLogEvent {
        private static final long serialVersionUID = 0;

        @Override
        public long getTimeMillis() {
            final Calendar cal = Calendar.getInstance();
            cal.set(2011, 11, 30, 10, 56, 35);
            cal.set(Calendar.MILLISECOND, 987);
            return cal.getTimeInMillis();
        }
    }

    @Test
    public void testFormatObjectStringBuilder() {
        DatePatternConverter converter = DatePatternConverter.newInstance(null);
        StringBuilder sb = new StringBuilder();
        converter.format("nondate", sb);

        String expected = ""; // only process dates
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatDateStringBuilder() {
        DatePatternConverter converter = DatePatternConverter.newInstance(null);
        StringBuilder sb = new StringBuilder();
        converter.format(date(2001, 1, 1), sb);

        String expected = "2001-02-01T14:15:16,123";
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testFormatStringBuilderObjectArray() {
        DatePatternConverter converter = DatePatternConverter.newInstance(null);
        StringBuilder sb = new StringBuilder();
        converter.format(sb, date(2001, 1, 1), date(2002, 2, 2), date(2003, 3, 3));

        String expected = "2001-02-01T14:15:16,123"; // only process first date
        assertEquals(expected, sb.toString());
    }

    private Date date(int year, int month, int date) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, date, 14, 15, 16);
        cal.set(Calendar.MILLISECOND, 123);
        return cal.getTime();
    }

    @Test
    public void testGetPatternReturnsISO8601ByDefault() {
        assertEquals(DatePatternConverter.ISO8601_PATTERN, DatePatternConverter.newInstance(null).getPattern());
    }

}
