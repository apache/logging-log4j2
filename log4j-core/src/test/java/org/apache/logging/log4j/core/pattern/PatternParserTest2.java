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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class PatternParserTest2 {

    @Test
    public void testParseConvertBackslashes() {
        final boolean convert = true;
        final StringBuilder buf = new StringBuilder();
        final String pattern = "%d{HH-mm-ss} \\t ...";

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 53);
        cal.set(Calendar.SECOND, 01);
        parse(pattern, convert, buf, cal.getTime(), 123);

        assertEquals("23-53-01 \t ...", buf.toString());
    }

    @Test
    public void testParseDontConvertBackslashes() {
        final boolean convert = false;
        final StringBuilder buf = new StringBuilder();
        final String pattern = "%d{HH-mm-ss} \\t---";

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 24);
        cal.set(Calendar.SECOND, 59);
        parse(pattern, convert, buf, cal.getTime(), 3);

        assertEquals("13-24-59 \\t---", buf.toString());
    }

    private void parse(final String pattern, final boolean convert, final StringBuilder buf, final Date date, final int i) {
        final PatternParser parser0 = new PatternParser(null, "Converter", null);
        final List<PatternConverter> converters = new ArrayList<>();
        final List<FormattingInfo> fields = new ArrayList<>();
        parser0.parse(pattern, converters, fields, false, false, convert);
        final FormattingInfo[] infoArray = new FormattingInfo[fields.size()];
        final FormattingInfo[] patternFields = fields.toArray(infoArray);
        final ArrayPatternConverter[] converterArray = new ArrayPatternConverter[converters.size()];
        final ArrayPatternConverter[] patternConverters = converters.toArray(converterArray);
        formatFileName(patternConverters, patternFields, buf, date, i);
    }

    /**
     * Format file name.
     * 
     * @param buf string buffer to which formatted file name is appended, may not be null.
     * @param objects objects to be evaluated in formatting, may not be null.
     */
    protected final void formatFileName(final ArrayPatternConverter[] patternConverters,
            final FormattingInfo[] patternFields, final StringBuilder buf, final Object... objects) {
        for (int i = 0; i < patternConverters.length; i++) {
            final int fieldStart = buf.length();
            patternConverters[i].format(buf, objects);

            if (patternFields[i] != null) {
                patternFields[i].format(fieldStart, buf);
            }
        }
    }
}
