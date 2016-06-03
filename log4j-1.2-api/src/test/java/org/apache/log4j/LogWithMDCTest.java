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
package org.apache.log4j;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Test logging with MDC values.
 */
public class LogWithMDCTest {

    private static final String CONFIG = "logWithMDC.xml";

    @ClassRule
    public static final LoggerContextRule CTX = new LoggerContextRule(CONFIG);

    @Test
    public void testMDC() throws Exception {
        MDC.put("Key1", "John");
        MDC.put("Key2", "Smith");
        final Logger logger = Logger.getLogger("org.apache.test.logging");
        logger.debug("This is a test");
        final ListAppender listApp = (ListAppender) CTX.getAppender("List");
        assertNotNull(listApp);
        final List<String> msgs = listApp.getMessages();
        assertNotNull("No messages received", msgs);
        assertTrue(msgs.size() == 1);
        assertTrue("Key1 is missing", msgs.get(0).contains("Key1=John"));
        assertTrue("Key2 is missing", msgs.get(0).contains("Key2=Smith"));
    }
}
