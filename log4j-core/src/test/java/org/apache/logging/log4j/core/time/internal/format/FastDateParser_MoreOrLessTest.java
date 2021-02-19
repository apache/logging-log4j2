/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.logging.log4j.core.time.internal.format;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.logging.log4j.core.time.internal.format.FastDateParser;
import org.junit.Assert;
import org.junit.Test;

/**
 * Copied from Apache Commons Lang 3 on 2016-11-16.
 */
public class FastDateParser_MoreOrLessTest {

    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    
    @Test
    public void testInputHasPrecedingCharacters() {
        final FastDateParser parser = new FastDateParser("MM/dd", TimeZone.getDefault(), Locale.getDefault());
        final ParsePosition parsePosition = new ParsePosition(0);
        final Date date = parser.parse("A 3/23/61", parsePosition);
        assertThat(date).isNull();
        assertThat(parsePosition.getIndex()).isEqualTo(0);      
        assertThat(parsePosition.getErrorIndex()).isEqualTo(0);        
    }

    @Test
    public void testInputHasWhitespace() {
        final FastDateParser parser = new FastDateParser("M/d/y", TimeZone.getDefault(), Locale.getDefault());
        //SimpleDateFormat parser = new SimpleDateFormat("M/d/y");
        final ParsePosition parsePosition = new ParsePosition(0);
        final Date date = parser.parse(" 3/ 23/ 1961", parsePosition);
        assertThat(parsePosition.getIndex()).isEqualTo(12);

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        assertThat(calendar.get(Calendar.YEAR)).isEqualTo(1961);
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(2);
        assertThat(calendar.get(Calendar.DATE)).isEqualTo(23);       
    }

    @Test
    public void testInputHasMoreCharacters() {
        final FastDateParser parser = new FastDateParser("MM/dd", TimeZone.getDefault(), Locale.getDefault());
        final ParsePosition parsePosition = new ParsePosition(0);
        final Date date = parser.parse("3/23/61", parsePosition);
        assertThat(parsePosition.getIndex()).isEqualTo(4);

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        assertThat(calendar.get(Calendar.MONTH)).isEqualTo(2);
        assertThat(calendar.get(Calendar.DATE)).isEqualTo(23);       
    }
    
    @Test
    public void testInputHasWrongCharacters() {
        final FastDateParser parser = new FastDateParser("MM-dd-yyy", TimeZone.getDefault(), Locale.getDefault());
        final ParsePosition parsePosition = new ParsePosition(0);
        assertThat(parser.parse("03/23/1961", parsePosition)).isNull();
        assertThat(parsePosition.getErrorIndex()).isEqualTo(2);
    }
    
    @Test
    public void testInputHasLessCharacters() {
        final FastDateParser parser = new FastDateParser("MM/dd/yyy", TimeZone.getDefault(), Locale.getDefault());
        final ParsePosition parsePosition = new ParsePosition(0);
        assertThat(parser.parse("03/23", parsePosition)).isNull();
        assertThat(parsePosition.getErrorIndex()).isEqualTo(5);
    }
    
    @Test
    public void testInputHasWrongTimeZone() {
        final FastDateParser parser = new FastDateParser("mm:ss z", NEW_YORK, Locale.US);
        
        final String input = "11:23 Pacific Standard Time";
        final ParsePosition parsePosition = new ParsePosition(0);
        assertThat(parser.parse(input, parsePosition)).isNotNull();
        assertThat(parsePosition.getIndex()).isEqualTo(input.length());
        
        parsePosition.setIndex(0);
        assertThat(parser.parse( "11:23 Pacific Standard ", parsePosition)).isNull();
        assertThat(parsePosition.getErrorIndex()).isEqualTo(6);
    }
    
    @Test
    public void testInputHasWrongDay() {
        final FastDateParser parser = new FastDateParser("EEEE, MM/dd/yyy", NEW_YORK, Locale.US);
        final String input = "Thursday, 03/23/61";
        final ParsePosition parsePosition = new ParsePosition(0);
        assertThat(parser.parse(input, parsePosition)).isNotNull();
        assertThat(parsePosition.getIndex()).isEqualTo(input.length());
        
        parsePosition.setIndex(0);
        assertThat(parser.parse( "Thorsday, 03/23/61", parsePosition)).isNull();
        assertThat(parsePosition.getErrorIndex()).isEqualTo(0);
    }
}
