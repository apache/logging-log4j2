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
        CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 * * ?");
        Date date = new GregorianCalendar(2015, 11, 2).getTime();
        Date fireDate = parser.getNextValidTimeAfter(date);
        Date expected = new GregorianCalendar(2015, 11, 2, 7, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testDayOfWeek() throws Exception {
        CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 ? * Fri");
        Date date = new GregorianCalendar(2015, 11, 2).getTime();
        Date fireDate = parser.getNextValidTimeAfter(date);
        Date expected = new GregorianCalendar(2015, 11, 4, 7, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testNextMonth() throws Exception {
        CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 1 * ?");
        Date date = new GregorianCalendar(2015, 11, 2).getTime();
        Date fireDate = parser.getNextValidTimeAfter(date);
        Date expected = new GregorianCalendar(2016, 0, 1, 7, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }

    @Test
    public void testLastDayOfMonth() throws Exception {
        CronExpression parser = new CronExpression("0 */15,12 7-11,13-17 L * ?");
        Date date = new GregorianCalendar(2015, 10, 2).getTime();
        Date fireDate = parser.getNextValidTimeAfter(date);
        Date expected = new GregorianCalendar(2015, 10, 30, 7, 0, 0).getTime();
        assertEquals("Dates not equal.", expected, fireDate);
    }
}
