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
package org.apache.logging.log4j.core.lookup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DateLookupTest {

    @Test
    public void testCorrectEvent() {
        final LogEvent mockedEvent = mock(LogEvent.class);
        final Calendar cal = Calendar.getInstance();
        cal.set(2011, Calendar.DECEMBER, 30, 10, 56, 35);
        when(mockedEvent.getTimeMillis()).thenReturn(cal.getTimeInMillis());

        final String lookupDate = new DateLookup().lookup(mockedEvent, "MM/dd/yyyy");
        assertEquals("12/30/2011", lookupDate);
    }

    @Test
    public void testValidKeyWithoutEvent() {
        final String dateFormat = "MM/dd/yyyy";

        final Calendar cal = Calendar.getInstance();
        final DateFormat formatter = new SimpleDateFormat(dateFormat);
        cal.setTimeInMillis(System.currentTimeMillis());
        final String today = formatter.format(cal.getTime());
        cal.add(Calendar.DATE, 1);
        final String tomorrow = formatter.format(cal.getTime());

        final String lookupTime = new DateLookup().lookup(null, dateFormat);
        // lookup gives current time, which by now could be tomorrow at midnight sharp
        assertTrue(lookupTime.equals(today) || lookupTime.equals(tomorrow));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"bananas"})
    public void testInvalidKey(final String key) {
        // For invalid keys without event, the current time in default format should be returned.
        // Checking this may depend on locale and exact time, and could become flaky.
        // Therefore we just check that the result isn't null and that (formatting) exceptions are caught.
        assertNotNull(new DateLookup().lookup(null, key));
    }
}
