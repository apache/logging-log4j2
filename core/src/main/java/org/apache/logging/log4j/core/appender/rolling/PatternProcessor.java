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

import org.apache.logging.log4j.core.pattern.ArrayPatternConverter;
import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Parse the rollover pattern.
 */
public class PatternProcessor {

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

    private RolloverFrequency frequency = null;

    /**
     * Constructor.
     * @param pattern The file pattern.
     */
    public PatternProcessor(String pattern) {
        PatternParser parser = createPatternParser();
        List<PatternConverter> converters = new ArrayList<PatternConverter>();
        List<FormattingInfo> fields = new ArrayList<FormattingInfo>();
        parser.parse(pattern, converters, fields);
        FormattingInfo[] infoArray = new FormattingInfo[fields.size()];
        patternFields = fields.toArray(infoArray);
        ArrayPatternConverter[] converterArray = new ArrayPatternConverter[converters.size()];
        patternConverters = converters.toArray(converterArray);

        for (ArrayPatternConverter converter : patternConverters) {
            if (converter instanceof DatePatternConverter) {
                DatePatternConverter dateConverter = (DatePatternConverter) converter;
                frequency = calculateFrequency(dateConverter.getPattern());
            }
        }
    }

    /**
     * Return the next potential rollover time.
     * @param current The current time.
     * @param increment The increment to the next time.
     * @return the next potential rollover time.
     */
    public long getNextTime(long current, int increment, boolean modulus) {
        if (frequency == null) {
            throw new IllegalStateException("Pattern does not contain a date");
        }
        Calendar currentCal = Calendar.getInstance();
        currentCal.setTimeInMillis(current);
        Calendar cal = Calendar.getInstance();
        cal.set(currentCal.get(Calendar.YEAR), 0, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (frequency == RolloverFrequency.ANNUALLY) {
            increment(cal, Calendar.YEAR, increment, modulus);
            return cal.getTimeInMillis();
        }
        if (frequency == RolloverFrequency.MONTHLY) {
            increment(cal, Calendar.MONTH, increment, modulus);
            return cal.getTimeInMillis();
        }
        if (frequency == RolloverFrequency.WEEKLY) {
            increment(cal, Calendar.WEEK_OF_YEAR, increment, modulus);
            return cal.getTimeInMillis();
        }
        cal.set(Calendar.DAY_OF_YEAR, currentCal.get(Calendar.DAY_OF_YEAR));
        if (frequency == RolloverFrequency.DAILY) {
            increment(cal, Calendar.DAY_OF_YEAR, increment, modulus);
            return cal.getTimeInMillis();
        }
        cal.set(Calendar.HOUR, currentCal.get(Calendar.HOUR));
        if (frequency == RolloverFrequency.HOURLY) {
            increment(cal, Calendar.HOUR, increment, modulus);
            return cal.getTimeInMillis();
        }
        cal.set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE));
        if (frequency == RolloverFrequency.EVERY_MINUTE) {
            increment(cal, Calendar.MINUTE, increment, modulus);
            return cal.getTimeInMillis();
        }
        cal.set(Calendar.SECOND, currentCal.get(Calendar.SECOND));
        if (frequency == RolloverFrequency.EVERY_SECOND) {
            increment(cal, Calendar.SECOND, increment, modulus);
            return cal.getTimeInMillis();
        }
        increment(cal, Calendar.MILLISECOND, increment, modulus);
        return cal.getTimeInMillis();
    }

    private void increment(Calendar cal, int type, int increment, boolean modulate) {
        int interval =  modulate ? increment - (cal.get(type) % increment) : increment;
        cal.add(type, interval);
    }

    /**
     * Format file name.
     * @param buf string buffer to which formatted file name is appended, may not be null.
     * @param obj object to be evaluated in formatting, may not be null.
     */
    protected final void formatFileName(final StringBuilder buf, final Object obj) {
        formatFileName(buf, new Date(System.currentTimeMillis()), obj);
    }

    /**
     * Format file name.
     * @param buf string buffer to which formatted file name is appended, may not be null.
     * @param objects objects to be evaluated in formatting, may not be null.
     */
    protected final void formatFileName(final StringBuilder buf, final Object... objects) {
        for (int i = 0; i < patternConverters.length; i++) {
            int fieldStart = buf.length();
            patternConverters[i].format(buf, objects);

            if (patternFields[i] != null) {
                patternFields[i].format(fieldStart, buf);
            }
        }
    }

    private RolloverFrequency calculateFrequency(String pattern) {
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

    private boolean patternContains(String pattern, char[] chars) {
        for (char character : chars) {
            if (patternContains(pattern, character)) {
                return true;
            }
        }
        return false;
    }

    private boolean patternContains(String pattern, char character) {
        return pattern.indexOf(character) >= 0;
    }
}
