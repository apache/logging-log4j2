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
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("log4j-date.xml")
public class LoggerDateTest {

    private final FileAppender fileApp;

    public LoggerDateTest(@Named("File") final FileAppender fileApp) {
        this.fileApp = fileApp;
    }

    @Test
    public void testFileName() {
        final String name = fileApp.getFileName();
        final int year = Calendar.getInstance().get(Calendar.YEAR);
        assertTrue(name.contains(Integer.toString(year)), "Date was not substituted: " + name);
    }
}
