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
package org.apache.logging.log4j.core.config.plugins.convert;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@RunWith(Parameterized.class)
public class DateTypeConverterTest {

    private final Class<? extends Date> dateClass;
    private final long timestamp;
    private final Object expected;

    @Parameterized.Parameters
    public static Object[][] data() {
        final long millis = System.currentTimeMillis();
        return new Object[][]{
            {Date.class, millis, new Date(millis)},
            {java.sql.Date.class, millis, new java.sql.Date(millis)},
            {Time.class, millis, new Time(millis)},
            {Timestamp.class, millis, new Timestamp(millis)}
        };
    }

    public DateTypeConverterTest(final Class<? extends Date> dateClass, final long timestamp, final Object expected) {
        this.dateClass = dateClass;
        this.timestamp = timestamp;
        this.expected = expected;
    }

    @Test
    public void testFromMillis() throws Exception {
        assertEquals(expected, DateTypeConverter.fromMillis(timestamp, dateClass));
    }
}