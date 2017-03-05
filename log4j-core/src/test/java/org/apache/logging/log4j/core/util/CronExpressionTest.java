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
package org.apache.logging.log4j.core.util;

import org.junit.Test;

import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

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
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testDayOfWeek() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 ? * Fri");
        final Date date = new GregorianCalendar(2015, 11, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2015, 11, 4, 7, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testNextMonth() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 1 * ?");
        final Date date = new GregorianCalendar(2015, 11, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2016, 0, 1, 7, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testLastDayOfMonth() throws Exception {
        final CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 L * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2015, 10, 30, 7, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testNextDay() throws Exception {
        final CronExpression parser = new CronExpression("0 0 0 * * ?");
        final Date date = new GregorianCalendar(2015, 10, 2).getTime();
        final Date fireDate = parser.getNextValidTimeAfter(date);
        final Date expected = new GregorianCalendar(2015, 10, 3, 0, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testPrevFireTime1() throws Exception {
        CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 L * ?");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getPrevFireTime(date);
        Date expected = new GregorianCalendar(2015, 9, 31, 17, 45, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testPrevFireTime2() throws Exception {
        CronExpression parser = new CronExpression("0 0/5 14,18 * * ?");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getPrevFireTime(date);
        Date expected = new GregorianCalendar(2015, 10, 1, 18, 55, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    /**
     * 35,45, and 55 minutes past the hour evern hour.
     */
    @Test
    public void testPrevFireTime3() throws Exception {
        CronExpression parser = new CronExpression("0 35/10 * * * ?");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getPrevFireTime(date);
        Date expected = new GregorianCalendar(2015, 10, 1, 23, 55, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    /**
     *
     * 10:15 every day.
     */
    @Test
    public void testPrevFireTimeTenFifteen() throws Exception {
        CronExpression parser = new CronExpression("0 15 10 * * ? *");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getPrevFireTime(date);
        Date expected = new GregorianCalendar(2015, 10, 1, 10, 15, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    /**
     * Every day from 2 pm to 2:59 pm
     */
    @Test
    public void testPrevFireTimeTwoPM() throws Exception {
        CronExpression parser = new CronExpression("0 * 14 * * ?");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getPrevFireTime(date);
        Date expected = new GregorianCalendar(2015, 10, 1, 14, 59, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    /**
     *  2:10pm and at 2:44pm every Wednesday in the month of March.
     */
    @Test
    public void testPrevFireTimeMarch() throws Exception {
        CronExpression parser = new CronExpression("0 10,44 14 ? 3 WED");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getPrevFireTime(date);
        Date expected = new GregorianCalendar(2015, 2, 25, 14, 44, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    /**
     *  Fire at 10:15am on the third Friday of every month.
     */
    @Test
    public void testPrevFireTimeThirdFriday() throws Exception {
        CronExpression parser = new CronExpression("0 15 10 ? * 6#3");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getPrevFireTime(date);
        Date expected = new GregorianCalendar(2015, 9, 16, 10, 15, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

}
