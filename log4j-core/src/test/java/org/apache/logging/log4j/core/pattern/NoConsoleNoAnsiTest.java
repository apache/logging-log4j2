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
package org.apache.logging.log4j.core.pattern;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NoConsoleNoAnsiTest {

    private static final String EXPECTED =
            "ERROR LoggerTest o.a.l.l.c.p.NoConsoleNoAnsiTest org.apache.logging.log4j.core.pattern.NoConsoleNoAnsiTest"
            + Strings.LINE_SEPARATOR;

    @Rule
    public LoggerContextRule init = new LoggerContextRule("log4j2-console-noConsoleNoAnsi.xml");

    private Logger logger;
    private ListAppender app;

    @Before
    public void setUp() throws Exception {
        this.logger = this.init.getLogger("LoggerTest");
        this.app = this.init.getListAppender("List").clear();
    }

    @Test
    public void testReplacement() {
        logger.error(this.getClass().getName());

        final List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals("Incorrect number of messages. Should be 1 is " + msgs.size(), 1, msgs.size());
        assertTrue("Replacement failed - expected ending " + EXPECTED + ", actual " + msgs.get(0), msgs.get(0).endsWith(EXPECTED));
    }
    
}
