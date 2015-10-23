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
package org.apache.logging.log4j.core;

import java.util.Calendar;

import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class LoggerDateTest {

    private static final String CONFIG = "log4j-date.xml";
    private FileAppender fileApp;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Before
    public void before() {
        fileApp = context.getRequiredAppender("File", FileAppender.class);
    }

    @Test
    public void testFileName() {
        final String name = fileApp.getFileName();
        final int year = Calendar.getInstance().get(Calendar.YEAR);
        assertTrue("Date was not substituted: " + name, name.contains(Integer.toString(year)));
    }
}
