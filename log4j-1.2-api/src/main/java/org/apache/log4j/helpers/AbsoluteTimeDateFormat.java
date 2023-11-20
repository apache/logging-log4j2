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
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Formats a {@link Date} in the format "HH:mm:ss,SSS" for example, "15:49:37,459".
 *
 * @since 0.7.5
 */
public class AbsoluteTimeDateFormat extends DateFormat {

    private static final long serialVersionUID = -388856345976723342L;

    /**
     * String constant used to specify {@link org.apache.log4j.helpers.AbsoluteTimeDateFormat} in layouts. Current value is
     * <b>ABSOLUTE</b>.
     */
    public static final String ABS_TIME_DATE_FORMAT = "ABSOLUTE";

    /**
     * String constant used to specify {@link org.apache.log4j.helpers.DateTimeDateFormat} in layouts. Current value is
     * <b>DATE</b>.
     */
    public static final String DATE_AND_TIME_DATE_FORMAT = "DATE";

    /**
     * String constant used to specify {@link org.apache.log4j.helpers.ISO8601DateFormat} in layouts. Current value is
     * <b>ISO8601</b>.
     */
    public static final String ISO8601_DATE_FORMAT = "ISO8601";

    private static long previousTime;

    private static char[] previousTimeWithoutMillis = new char[9]; // "HH:mm:ss."

    public AbsoluteTimeDateFormat() {
        setCalendar(Calendar.getInstance());
    }

    public AbsoluteTimeDateFormat(final TimeZone timeZone) {
        setCalendar(Calendar.getInstance(timeZone));
    }

    /**
     * Appends to <code>sbuf</code> the time in the format "HH:mm:ss,SSS" for example, "15:49:37,459"
     *
     * @param date the date to format
     * @param sbuf the string buffer to write to
     * @param fieldPosition remains untouched
     */
    @Override
    public StringBuffer format(final Date date, final StringBuffer sbuf, final FieldPosition fieldPosition) {

        final long now = date.getTime();
        final int millis = (int) (now % 1000);

        if ((now - millis) != previousTime || previousTimeWithoutMillis[0] == 0) {
            // We reach this point at most once per second
            // across all threads instead of each time format()
            // is called. This saves considerable CPU time.

            calendar.setTime(date);

            final int start = sbuf.length();

            final int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour < 10) {
                sbuf.append('0');
            }
            sbuf.append(hour);
            sbuf.append(':');

            final int mins = calendar.get(Calendar.MINUTE);
            if (mins < 10) {
                sbuf.append('0');
            }
            sbuf.append(mins);
            sbuf.append(':');

            final int secs = calendar.get(Calendar.SECOND);
            if (secs < 10) {
                sbuf.append('0');
            }
            sbuf.append(secs);
            sbuf.append(',');

            // store the time string for next time to avoid recomputation
            sbuf.getChars(start, sbuf.length(), previousTimeWithoutMillis, 0);

            previousTime = now - millis;
        } else {
            sbuf.append(previousTimeWithoutMillis);
        }

        if (millis < 100) {
            sbuf.append('0');
        }
        if (millis < 10) {
            sbuf.append('0');
        }

        sbuf.append(millis);
        return sbuf;
    }

    /**
     * Always returns null.
     */
    @Override
    public Date parse(final String s, final ParsePosition pos) {
        return null;
    }
}
