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
package org.apache.logging.log4j.core.appender.rolling;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.ArrayPatternConverter;
import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Parse the rollover pattern.
 */
public class PatternProcessor {

    protected static final Logger LOGGER = StatusLogger.getLogger();
    private static final String KEY = "FileConverter";

    private static final char YEAR_CHAR = 'y';
    private static final char MONTH_CHAR = 'M';
    private static final char[] WEEK_CHARS = {'w', 'W'};
    private static final char[] DAY_CHARS = {'D', 'd', 'F', 'E'};
    private static final char[] HOUR_CHARS = {'H', 'K', 'h', 'k'};
    private static final char MINUTE_CHAR = 'm';
    private static final char SECOND_CHAR = 's';
    private static final char MILLIS_CHAR = 'S';

    private final ArrayPatternConverter[] patternConverters;
    private final FormattingInfo[] patternFields;

    private long prevFileTime = 0;
    private long nextFileTime = 0;

    private RolloverFrequency frequency = null;

    /**
     * Constructor.
     * @param pattern The file pattern.
     */
    public PatternProcessor(final String pattern) {
        final PatternParser parser = createPatternParser();
        final List<PatternConverter> converters = new ArrayList<PatternConverter>();
        final List<FormattingInfo> fields = new ArrayList<FormattingInfo>();
        parser.parse(pattern, converters, fields, false);
        final FormattingInfo[] infoArray = new FormattingInfo[fields.size()];
        patternFields = fields.toArray(infoArray);
        final ArrayPatternConverter[] converterArray = new ArrayPatternConverter[converters.size()];
        patternConverters = converters.toArray(converterArray);

        for (final ArrayPatternConverter converter : patternConverters) {
            if (converter instanceof DatePatternConverter) {
                final DatePatternConverter dateConverter = (DatePatternConverter) converter;
                frequency = calculateFrequency(dateConverter.getPattern());
            }
        }
    }

    /**
     * Returns the next potential rollover time.
     * @param current The current time.
     * @param increment The increment to the next time.
     * @param modulus If true the time will be rounded to occur on a boundary aligned with the increment.
     * @return the next potential rollover time and the timestamp for the target file.
     */
    public long getNextTime(final long current, final int increment, final boolean modulus) {
        prevFileTime = nextFileTime;
        long nextTime;

        if (frequency == null) {
            throw new IllegalStateException("Pattern does not contain a date");
        }
        final Calendar currentCal = Calendar.getInstance();
        currentCal.setTimeInMillis(current);
        final Calendar cal = Calendar.getInstance();
        cal.set(currentCal.get(Calendar.YEAR), 0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (frequency == RolloverFrequency.ANNUALLY) {
            increment(cal, Calendar.YEAR, increment, modulus);
            nextTime = cal.getTimeInMillis();
            cal.add(Calendar.YEAR, -1);
            nextFileTime = cal.getTimeInMillis();
            return debugGetNextTime(nextTime);
        }
        cal.set(Calendar.MONTH, currentCal.get(Calendar.MONTH));
        if (frequency == RolloverFrequency.MONTHLY) {
            increment(cal, Calendar.MONTH, increment, modulus);
            nextTime = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, -1);
            nextFileTime = cal.getTimeInMillis();
            return debugGetNextTime(nextTime);
        }
        if (frequency == RolloverFrequency.WEEKLY) {
            cal.set(Calendar.WEEK_OF_YEAR, currentCal.get(Calendar.WEEK_OF_YEAR));
            increment(cal, Calendar.WEEK_OF_YEAR, increment, modulus);
            cal.set(Calendar.DAY_OF_WEEK, currentCal.getFirstDayOfWeek());
            nextTime = cal.getTimeInMillis();
            cal.add(Calendar.WEEK_OF_YEAR, -1);
            nextFileTime = cal.getTimeInMillis();
            return debugGetNextTime(nextTime);
        }
        cal.set(Calendar.DAY_OF_YEAR, currentCal.get(Calendar.DAY_OF_YEAR));
        if (frequency == RolloverFrequency.DAILY) {
            increment(cal, Calendar.DAY_OF_YEAR, increment, modulus);
            nextTime = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            nextFileTime = cal.getTimeInMillis();
            return debugGetNextTime(nextTime);
        }
        cal.set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY));
        if (frequency == RolloverFrequency.HOURLY) {
            increment(cal, Calendar.HOUR_OF_DAY, increment, modulus);
            nextTime = cal.getTimeInMillis();
            cal.add(Calendar.HOUR_OF_DAY, -1);
            nextFileTime = cal.getTimeInMillis();
            return debugGetNextTime(nextTime);
        }
        cal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE));
        if (frequency == RolloverFrequency.EVERY_MINUTE) {
            increment(cal, Calendar.MINUTE, increment, modulus);
            nextTime = cal.getTimeInMillis();
            cal.add(Calendar.MINUTE, -1);
            nextFileTime = cal.getTimeInMillis();
            return debugGetNextTime(nextTime);
        }
        cal.set(Calendar.SECOND, currentCal.get(Calendar.SECOND));
        if (frequency == RolloverFrequency.EVERY_SECOND) {
            increment(cal, Calendar.SECOND, increment, modulus);
            nextTime = cal.getTimeInMillis();
            cal.add(Calendar.SECOND, -1);
            nextFileTime = cal.getTimeInMillis();
            return debugGetNextTime(nextTime);
        }
        cal.set(Calendar.MILLISECOND, currentCal.get(Calendar.MILLISECOND));
        increment(cal, Calendar.MILLISECOND, increment, modulus);
        nextTime = cal.getTimeInMillis();
        cal.add(Calendar.MILLISECOND, -1);
        nextFileTime = cal.getTimeInMillis();
        return debugGetNextTime(nextTime);
    }

    private long debugGetNextTime(long nextTime) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("PatternProcessor.getNextTime returning {}, nextFileTime={}, prevFileTime={}, freq={}", //
                    format(nextTime), format(nextFileTime), format(prevFileTime), frequency);
        }
        return nextTime;
    }

    private String format(long time) {
        return new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS").format(new Date(time));
    }

    private void increment(final Calendar cal, final int type, final int increment, final boolean modulate) {
        final int interval =  modulate ? increment - (cal.get(type) % increment) : increment;
        cal.add(type, interval);
    }

    /**
     * Format file name.
     * @param buf string buffer to which formatted file name is appended, may not be null.
     * @param obj object to be evaluated in formatting, may not be null.
     */
    public final void formatFileName(final StringBuilder buf, final Object obj) {
        final long time = prevFileTime == 0 ? System.currentTimeMillis() : prevFileTime;
        formatFileName(buf, new Date(time), obj);
    }

    /**
     * Format file name.
     * @param subst The StrSubstitutor.
     * @param buf string buffer to which formatted file name is appended, may not be null.
     * @param obj object to be evaluated in formatting, may not be null.
     */
    public final void formatFileName(final StrSubstitutor subst, final StringBuilder buf, final Object obj) {
        // LOG4J2-628: we deliberately use System time, not the log4j.Clock time
        // for creating the file name of rolled-over files. 
        final long time = prevFileTime == 0 ? System.currentTimeMillis() : prevFileTime;
        formatFileName(buf, new Date(time), obj);
        LogEvent event = new Log4jLogEvent(time);
        String fileName = subst.replace(event, buf);
        buf.setLength(0);
        buf.append(fileName);
    }

    /**
     * Format file name.
     * @param buf string buffer to which formatted file name is appended, may not be null.
     * @param objects objects to be evaluated in formatting, may not be null.
     */
    protected final void formatFileName(final StringBuilder buf, final Object... objects) {
        for (int i = 0; i < patternConverters.length; i++) {
            final int fieldStart = buf.length();
            patternConverters[i].format(buf, objects);

            if (patternFields[i] != null) {
                patternFields[i].format(fieldStart, buf);
            }
        }
    }

    private RolloverFrequency calculateFrequency(final String pattern) {
        if (patternContains(pattern, MILLIS_CHAR)) {
            return RolloverFrequency.EVERY_MILLISECOND;
        }
        if (patternContains(pattern, SECOND_CHAR)) {
            return RolloverFrequency.EVERY_SECOND;
        }
        if (patternContains(pattern, MINUTE_CHAR)) {
            return RolloverFrequency.EVERY_MINUTE;
        }
        if (patternContains(pattern, HOUR_CHARS)) {
            return RolloverFrequency.HOURLY;
        }
        if (patternContains(pattern, DAY_CHARS)) {
            return RolloverFrequency.DAILY;
        }
        if (patternContains(pattern, WEEK_CHARS)) {
            return RolloverFrequency.WEEKLY;
        }
        if (patternContains(pattern, MONTH_CHAR)) {
            return RolloverFrequency.MONTHLY;
        }
        if (patternContains(pattern, YEAR_CHAR)) {
            return RolloverFrequency.ANNUALLY;
        }
        return null;
    }

    private PatternParser createPatternParser() {

        return new PatternParser(null, KEY, null);
    }

    private boolean patternContains(final String pattern, final char... chars) {
        for (final char character : chars) {
            if (patternContains(pattern, character)) {
                return true;
            }
        }
        return false;
    }

    private boolean patternContains(final String pattern, final char character) {
        return pattern.indexOf(character) >= 0;
    }
}
