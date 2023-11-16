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
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.jupiter.api.Test;

/**
 * Class Description goes here.
 * Created by rgoers on 11/15/15
 */
public class CronExpressionTest {

    @Test
    public void testDayOfMonth() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 * * ?");
        final Date date = new GregorianCalendar(2015, 11, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2015, 11, 2, 7, 0, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    @Test
    public void testDayOfWeek() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 ? * Fri");
        final Date date = new GregorianCalendar(2015, 11, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2015, 11, 4, 7, 0, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    @Test
    public void testNextMonth() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 1 * ?");
        final Date date = new GregorianCalendar(2015, 11, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2016, 0, 1, 7, 0, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    @Test
    public void testLastDayOfMonth() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 L * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2015, 10, 30, 7, 0, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    @Test
    public void testNextDay() throws Exception {
        final CronExpression parser = new CronExpression("0 0 0 * * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2015, 10, 3, 0, 0, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    @Test
    public void testPrevFireTime1() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 L * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getPrevFireTime(date);
        final Date expected = new GregorianCalendar(2015, 9, 31, 17, 45, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    @Test
    public void testPrevFireTime2() throws Exception {
        final CronExpression parser = new CronExpression("0 0/5 14,18 * * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getPrevFireTime(date);
        final Date expected = new GregorianCalendar(2015, 10, 1, 18, 55, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    /**
     * 35,45, and 55 minutes past the hour every hour.
     */
    @Test
    public void testPrevFireTime3() throws Exception {
        final CronExpression parser = new CronExpression("0 35/10 * * * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getPrevFireTime(date);
        final Date expected = new GregorianCalendar(2015, 10, 1, 23, 55, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    /**
     *
     * 10:15 every day.
     */
    @Test
    public void testPrevFireTimeTenFifteen() throws Exception {
        final CronExpression parser = new CronExpression("0 15 10 * * ? *");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getPrevFireTime(date);
        final Date expected = new GregorianCalendar(2015, 10, 1, 10, 15, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    /**
     * Every day from 2 pm to 2:59 pm
     */
    @Test
    public void testPrevFireTimeTwoPM() throws Exception {
        final CronExpression parser = new CronExpression("0 * 14 * * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getPrevFireTime(date);
        final Date expected = new GregorianCalendar(2015, 10, 1, 14, 59, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    /**
     *  2:10pm and at 2:44pm every Wednesday in the month of March.
     */
    @Test
    public void testPrevFireTimeMarch() throws Exception {
        final CronExpression parser = new CronExpression("0 10,44 14 ? 3 WED");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getPrevFireTime(date);
        final Date expected = new GregorianCalendar(2015, 2, 25, 14, 44, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    /**
     *  Fire at 10:15am on the third Friday of every month.
     */
    @Test
    public void testPrevFireTimeThirdFriday() throws Exception {
        final CronExpression parser = new CronExpression("0 15 10 ? * 6#3");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getPrevFireTime(date);
        final Date expected = new GregorianCalendar(2015, 9, 16, 10, 15, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }

    /*
     * Input time with milliseconds will correctly return the next
     * scheduled time.
     */
    @Test
    public void testTimeBeforeMilliseconds() throws Exception {
        final CronExpression parser = new CronExpression("0 0 0 * * ?");
        final GregorianCalendar cal = new GregorianCalendar(2015, 10, 2, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 100);
        final Date date = cal.getTime();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        System.err.println(sdf.format(date));
        final Date fireDate = parser.getTimeBefore(date);
        System.err.println(sdf.format(fireDate));
        final Date expected = new GregorianCalendar(2015, 10, 1, 0, 0, 0).getTime();
        assertEquals(expected, fireDate, "Dates not equal.");
    }
}
