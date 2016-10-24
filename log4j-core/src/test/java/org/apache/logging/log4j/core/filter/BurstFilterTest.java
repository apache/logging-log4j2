/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.logging.log4j.core.filter;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * Unit test for <code>BurstFilter</code>.
 */
public class BurstFilterTest {

    private static final String CONFIG = "log4j-burst.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Before
    public void setUp() throws Exception {
        app = context.getListAppender("ListAppender");
        filter = (BurstFilter) app.getFilter();
        assertNotNull("No BurstFilter", filter);
    }

    private ListAppender app;
    private BurstFilter filter;

    private final Logger logger = context.getLogger();

    /**
     * Test BurstFilter by surpassing maximum number of log messages allowed by filter and
     * making sure only the maximum number are indeed logged, then wait for while and make
     * sure the filter allows the appropriate number of messages to be logged.
     */
    @Test
    public void test() throws Exception {
        System.nanoTime();
        for (int i = 0; i < 110; i++) {
            if (i % 10 == 0) {
                Thread.sleep(200);
            }
            logger.info("Logging 110 messages, should only see 100 logs # " + (i + 1));
            assertTrue("Incorrect number of available slots", filter.getAvailable() < 100);
        }
        List<String> msgs = app.getMessages();
        assertEquals("Incorrect message count. Should be 100, actual " + msgs.size(), 100, msgs.size());
        app.clear();

        assertTrue("Incorrect number of available slots", filter.getAvailable() < 100);
        // Allow some of the events to clear
        Thread.sleep(1500);

        for (int i = 0; i < 110; i++) {
            logger.info("Waited 1.5 seconds and trying to log again, should see more than 0 and less than 100" + (i + 1));
        }

        msgs = app.getMessages();
        assertFalse("No messages were counted.", msgs.isEmpty());
        assertTrue("Incorrect message count. Should be > 0 and < 100, actual " + msgs.size(), msgs.size() < 100);
        app.clear();

        filter.clear();

        for (int i = 0; i < 110; i++) {
            logger.info("Waited 1.5 seconds and trying to log again, should see more than 0 and less than 100" + (i + 1));
        }
        assertEquals("", 0, filter.getAvailable());
        app.clear();


        // now log 100 debugs, they shouldn't get through because there are no available slots.
        for (int i = 0; i < 110; i++) {
            logger.debug(
                "TEST FAILED! Logging 110 debug messages, shouldn't see any of them because they are debugs #" + (i + 1));
        }

        msgs = app.getMessages();
        assertTrue("Incorrect message count. Should be 0, actual " + msgs.size(), msgs.isEmpty());
        app.clear();

        // now log 100 warns, they should all get through because the filter's level is set at info
        for (int i = 0; i < 110; i++) {
            logger.warn("Logging 110 warn messages, should see all of them because they are warns #" + (i + 1));
        }

        msgs = app.getMessages();
        assertEquals("Incorrect message count. Should be 110, actual " + msgs.size(), 110, msgs.size());
        app.clear();

        // now log 100 errors, they should all get through because the filter level is set at info
        for (int i = 0; i < 110; i++) {
            logger.error("Logging 110 error messages, should see all of them because they are errors #" + (i + 1));
        }

        msgs = app.getMessages();
        assertEquals("Incorrect message count. Should be 110, actual " + msgs.size(), 110, msgs.size());
        app.clear();

        // now log 100 fatals, they should all get through because the filter level is set at info
        for (int i = 0; i < 110; i++) {
            logger.fatal("Logging 110 fatal messages, should see all of them because they are fatals #" + (i + 1));
        }

        msgs = app.getMessages();
        assertEquals("Incorrect message count. Should be 110, actual " + msgs.size(), 110, msgs.size());
        app.clear();

        // wait and make sure we can log messages again despite the fact we just logged a bunch of warns, errors, fatals
        Thread.sleep(3100);

        for (int i = 0; i < 110; i++) {
            logger.debug("Waited 3+ seconds, should see 100 logs #" + (i + 1));
        }
        msgs = app.getMessages();
        assertEquals("Incorrect message count. Should be 100, actual " + msgs.size(), 100, msgs.size());
        app.clear();

    }
}
