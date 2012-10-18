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
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class LoggerTest {

    TestLogger logger = (TestLogger) LogManager.getLogger("LoggerTest");
    List<String> results = logger.getEntries();

    @Before
    public void setup() {
        results.clear();
    }

    @Test
    public void basicFlow() {
        logger.entry();
        logger.exit();
        assertEquals(2, results.size());
        assertTrue("Incorrect Entry", results.get(0).startsWith(" TRACE  entry"));
        assertTrue("incorrect Exit", results.get(1).startsWith(" TRACE  exit"));

    }

    @Test
    public void throwing() {
        logger.throwing(new IllegalArgumentException("Test Exception"));
        assertEquals(1, results.size());
        assertTrue("Incorrect Throwing",
            results.get(0).startsWith(" ERROR throwing java.lang.IllegalArgumentException: Test Exception"));
    }

    @Test
    public void catching() {
        try {
            throw new NullPointerException();
        } catch (Exception e) {
            logger.catching(e);
            assertEquals(1, results.size());
            assertTrue("Incorrect Catching",
                results.get(0).startsWith(" ERROR catching java.lang.NullPointerException"));
        }
    }

    @Test
    public void debug() {
        logger.debug("Debug message");
        assertEquals(1, results.size());
        assertTrue("Incorrect message", results.get(0).startsWith(" DEBUG Debug message"));
    }

    @Test
    public void debugObject() {
        logger.debug(new Date());
        assertEquals(1, results.size());
        assertTrue("Invalid length", results.get(0).length() > 7);
    }

    @Test
    public void debugWithParms() {
        logger.debug("Hello, {}", "World");
        assertEquals(1, results.size());
        assertTrue("Incorrect substitution", results.get(0).startsWith(" DEBUG Hello, World"));
    }

    @Test
    public void debugWithParmsAndThrowable() {
        logger.debug("Hello, {}", "World", new RuntimeException("Test Exception"));
        assertEquals(1, results.size());
        assertTrue("Unexpected results: " + results.get(0),
            results.get(0).startsWith(" DEBUG Hello, World java.lang.RuntimeException: Test Exception"));
    }

    @Test
    public void mdc() {

        ThreadContext.put("TestYear", new Integer(2010).toString());
        logger.debug("Debug message");
        ThreadContext.clear();
        logger.debug("Debug message");
        assertEquals(2, results.size());
        assertTrue("Incorrect MDC: " + results.get(0),
            results.get(0).startsWith(" DEBUG Debug message {TestYear=2010}"));
        assertTrue("MDC not cleared?: " + results.get(1),
            results.get(1).startsWith(" DEBUG Debug message"));
    }

    @Test
    public void structuredData() {
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        StructuredDataMessage msg = new StructuredDataMessage("Audit@18060", "Transfer Complete", "Transfer");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        logger.info(MarkerManager.getMarker("EVENT"), msg);
        ThreadContext.clear();
        assertEquals(1, results.size());
        assertTrue("Incorrect structured data: " + results.get(0),results.get(0).startsWith(
            " INFO Transfer [Audit@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"] Transfer Complete"));
    }

    @Test
    public void getLoggerByClass() {
        Logger classLogger = LogManager.getLogger(LoggerTest.class);
        assertNotNull(classLogger);
    }

    public void getLoggerByNullClass() {
        // Returns a SimpleLogger
        Assert.assertNotNull(LogManager.getLogger((Class<?>) null));
    }

    public void getLoggerByNullObject() {
        // Returns a SimpleLogger
        Assert.assertNotNull(LogManager.getLogger((Object) null));
    }

    @Test
    public void getLoggerByNullString() {
        // Returns a SimpleLogger
        Assert.assertNotNull(LogManager.getLogger((String) null));
    }

    @Test
    public void getLoggerByObject() {
        Logger classLogger = LogManager.getLogger(this);
        assertNotNull(classLogger);
        assertEquals(classLogger, LogManager.getLogger(LoggerTest.class));
    }
}
