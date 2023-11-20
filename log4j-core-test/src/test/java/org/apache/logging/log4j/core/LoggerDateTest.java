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
package org.apache.logging.log4j.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;

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
