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
package org.apache.logging.log4j.core.lookup;

import java.util.Calendar;

import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DateLookupTest {

    @Test
    public void testDateLookupInEvent() {
        final LogEvent mockedEvent = mock(LogEvent.class);
        final Calendar cal = Calendar.getInstance();
        cal.set(2011, Calendar.DECEMBER, 30, 10, 56, 35);
        when(mockedEvent.getTimeMillis()).thenReturn(cal.getTimeInMillis());

        final String actualDate = new DateLookup().lookup(mockedEvent, "MM/dd/yyyy");
        assertEquals("12/30/2011", actualDate);
    }

    @Test
    public void testDateLookUpNoEvent() {
        final String dateFormat = "MM/dd/yyyy";
        final String actualDate = new DateLookup().lookup(null, dateFormat);
        // we don't know current time, so check length of format instead.
        assertEquals(actualDate.length(), dateFormat.length());
    }

    @Test
    public void testNullDateLookupYieldsCurrentTime() {
        final String currentTime = new DateLookup().lookup(null, null);
        assertNotNull(currentTime);
        // we don't know current time nor format, so just check length of date string.
        assertTrue(currentTime.length() > 0);
    }

    @Test
    public void testInvalidFormatYieldsDefaultFormat() {
        final String currentTime = new DateLookup().lookup(null, "bananas");
        // we don't know current time nor format, so just check length of date string.
        assertTrue(currentTime.length() > 0);
    }
}
