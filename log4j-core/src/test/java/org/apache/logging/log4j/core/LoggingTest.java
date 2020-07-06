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

import java.util.List;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Timer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertEquals;


public class LoggingTest {

    private static final String CONFIG = "log4j-list.xml";

    @Rule
    public final TestName testName = new TestName();
    private ListAppender list;


    @Rule
    public LoggerContextRule context = new LoggerContextRule(CONFIG);

    private void assertEventCount(final List<LogEvent> events, final int expected) {
        assertEquals("Incorrect number of events.", expected, events.size());
    }

    @Before
    public void before() {
        logger = context.getLogger("LoggerTest");
    }

    org.apache.logging.log4j.Logger logger;

    @Test
    public void logTime() {
        Timer timer = new Timer("initial");
        timer.start();
        logger.info("This is a test");
        System.out.println(timer.stop());
        timer = new Timer("more", 100);
/*        timer.start();
        for (int i=0; i < 100; ++i) {
            logger.info("This is another test");
        }
        System.out.println(timer.stop());*/
    }
}

