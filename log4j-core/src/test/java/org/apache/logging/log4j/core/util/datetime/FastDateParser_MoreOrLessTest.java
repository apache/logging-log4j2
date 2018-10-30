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
package org.apache.logging.log4j.core.util.datetime;

import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
        Assert.assertNull(date);
        Assert.assertEquals(0, parsePosition.getIndex());      
        Assert.assertEquals(0, parsePosition.getErrorIndex());        
    }

    @Test
    public void testInputHasWhitespace() {
        final FastDateParser parser = new FastDateParser("M/d/y", TimeZone.getDefault(), Locale.getDefault());
        //SimpleDateFormat parser = new SimpleDateFormat("M/d/y");
        final ParsePosition parsePosition = new ParsePosition(0);
        final Date date = parser.parse(" 3/ 23/ 1961", parsePosition);
        Assert.assertEquals(12, parsePosition.getIndex());

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Assert.assertEquals(1961, calendar.get(Calendar.YEAR));
        Assert.assertEquals(2, calendar.get(Calendar.MONTH));
        Assert.assertEquals(23, calendar.get(Calendar.DATE));       
    }

    @Test
    public void testInputHasMoreCharacters() {
        final FastDateParser parser = new FastDateParser("MM/dd", TimeZone.getDefault(), Locale.getDefault());
        final ParsePosition parsePosition = new ParsePosition(0);
        final Date date = parser.parse("3/23/61", parsePosition);
        Assert.assertEquals(4, parsePosition.getIndex());

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        Assert.assertEquals(2, calendar.get(Calendar.MONTH));
        Assert.assertEquals(23, calendar.get(Calendar.DATE));       
    }
    
    @Test
    public void testInputHasWrongCharacters() {
        final FastDateParser parser = new FastDateParser("MM-dd-yyy", TimeZone.getDefault(), Locale.getDefault());
        final ParsePosition parsePosition = new ParsePosition(0);
        Assert.assertNull(parser.parse("03/23/1961", parsePosition));
        Assert.assertEquals(2, parsePosition.getErrorIndex());
    }
    
    @Test
    public void testInputHasLessCharacters() {
        final FastDateParser parser = new FastDateParser("MM/dd/yyy", TimeZone.getDefault(), Locale.getDefault());
        final ParsePosition parsePosition = new ParsePosition(0);
        Assert.assertNull(parser.parse("03/23", parsePosition));
        Assert.assertEquals(5, parsePosition.getErrorIndex());
    }
    
    @Test
    public void testInputHasWrongTimeZone() {
        final FastDateParser parser = new FastDateParser("mm:ss z", NEW_YORK, Locale.US);
        
        final String input = "11:23 Pacific Standard Time";
        final ParsePosition parsePosition = new ParsePosition(0);
        Assert.assertNotNull(parser.parse(input, parsePosition));
        Assert.assertEquals(input.length(), parsePosition.getIndex());
        
        parsePosition.setIndex(0);
        Assert.assertNull(parser.parse( "11:23 Pacific Standard ", parsePosition));
        Assert.assertEquals(6, parsePosition.getErrorIndex());
    }
    
    @Test
    public void testInputHasWrongDay() {
        final FastDateParser parser = new FastDateParser("EEEE, MM/dd/yyy", NEW_YORK, Locale.US);
        final String input = "Thursday, 03/23/61";
        final ParsePosition parsePosition = new ParsePosition(0);
        Assert.assertNotNull(parser.parse(input, parsePosition));
        Assert.assertEquals(input.length(), parsePosition.getIndex());
        
        parsePosition.setIndex(0);
        Assert.assertNull(parser.parse( "Thorsday, 03/23/61", parsePosition));
        Assert.assertEquals(0, parsePosition.getErrorIndex());
    }
}
