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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class DateLookupTest {


    @Test
    public void testLookup() {
        final StrLookup lookup = new DateLookup();
        final LogEvent event = new MyLogEvent();
        final String value = lookup.lookup(event, "MM/dd/yyyy");
        assertNotNull(value);
        assertEquals("12/30/2011", value);
    }

    private class MyLogEvent extends AbstractLogEvent {
        /**
         * Generated serial version ID.
         */
        private static final long serialVersionUID = -2663819677970643109L;

        @Override
        public long getTimeMillis() {
            final Calendar cal = Calendar.getInstance();
            cal.set(2011, 11, 30, 10, 56, 35);
            return cal.getTimeInMillis();
        }

    }
}
