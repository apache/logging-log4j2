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
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Filters events that fall within a specified time period in each day.
 */
@Plugin(name = "TimeFilter", type = "Core", elementType = "filter", printObject = true)
public final class TimeFilter extends AbstractFilter {
    /**
     * Length of hour in milliseconds.
     */
    private static final long HOUR_MS = 3600000;

    /**
     * Length of minute in milliseconds.
     */
    private static final long MINUTE_MS = 60000;

    /**
     * Length of second in milliseconds.
     */
    private static final long SECOND_MS = 1000;

    /**
     * Starting offset from midnight in milliseconds.
     */
    private final long start;
    /**
     * Ending offset from midnight in milliseconds.
     */
    private final long end;
    /**
     * Timezone.
     */
    private final TimeZone timezone;


    private TimeFilter(long start, long end, TimeZone tz, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        this.start = start;
        this.end = end;
        timezone = tz;
    }

    @Override
    public Result filter(LogEvent event) {
        Calendar calendar = Calendar.getInstance(timezone);
        calendar.setTimeInMillis(event.getMillis());
        //
        //   get apparent number of milliseconds since midnight
        //      (ignores extra or missing hour on daylight time changes).
        //
        long apparentOffset = calendar.get(Calendar.HOUR_OF_DAY) * HOUR_MS +
            calendar.get(Calendar.MINUTE) * MINUTE_MS +
            calendar.get(Calendar.SECOND) * SECOND_MS +
            calendar.get(Calendar.MILLISECOND);
        return (apparentOffset >= start && apparentOffset < end) ? onMatch : onMismatch;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("start=").append(start);
        sb.append(", end=").append(end);
        sb.append(", timezone=").append(timezone.toString());
        return sb.toString();
    }

    /**
     * Create a TimeFilter.
     * @param start The start time.
     * @param end The end time.
     * @param tz timezone.
     * @param match Action to perform if the time matches.
     * @param mismatch Action to perform if the action does not match.
     * @return A TimeFilter.
     */
    @PluginFactory
    public static TimeFilter createFilter(@PluginAttr("start") String start,
                                          @PluginAttr("end") String end,
                                          @PluginAttr("timezone") String tz,
                                          @PluginAttr("onMatch") String match,
                                          @PluginAttr("onMismatch") String mismatch) {
        SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss");
        long s = 0;
        if (start != null) {
            stf.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                s = stf.parse(start).getTime();
            } catch (ParseException ex) {
                LOGGER.warn("Error parsing start value " + start, ex);
            }
        }
        long e = Long.MAX_VALUE;
        if (end != null) {
            stf.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                e = stf.parse(end).getTime();
            } catch (ParseException ex) {
                LOGGER.warn("Error parsing start value " + end, ex);
            }
        }
        TimeZone timezone = (tz == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(tz);
        Result onMatch = match == null ? Result.NEUTRAL : Result.toResult(match);
        Result onMismatch = mismatch == null ? Result.DENY : Result.toResult(mismatch);

        return new TimeFilter(s, e, timezone, onMatch, onMismatch);
    }

}
