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
package org.apache.logging.log4j.jul;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.logging.Level.INFO;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class BracketInNotInterpolatedMessageTest {

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
    }

    @Test
    public void noInterpolation() {
        final Logger logger = Logger.getLogger("Test");
        logger.info("{raw}");
        logger.log(new LogRecord(INFO, "{raw}"));// should lead to the same as previous but was not the case LOG4J2-1251
        final List<LogEvent> events = ListAppender.getListAppender("TestAppender").getEvents();
        assertThat(events, hasSize(2));
        assertEquals("{raw}", events.get(0).getMessage().getFormattedMessage());
        assertEquals("{raw}", events.get(1).getMessage().getFormattedMessage());
    }

}
