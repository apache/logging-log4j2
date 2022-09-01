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
package org.apache.logging.log4j.jul.test;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.jul.LogManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class CallerInformationTest {

    // config from log4j-core test-jar
    private static final String CONFIG = "log4j2-calling-class.xml";

    private LoggerContext ctx;
    private ListAppender app;


    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
    }

    @Before
    public void beforeEach() throws Exception {
        URI uri = this.getClass().getClassLoader().getResource(CONFIG).toURI();
        ctx = (LoggerContext)org.apache.logging.log4j.LogManager.getContext(null, false, uri);
        assertNotNull("No LoggerContext", ctx);
    }

    @After
    public void afterEach() throws Exception {
        if (ctx != null) {
            ctx.stop();
            ctx = null;
            app = null;
        }
    }

    @Test
    public void testClassLogger() throws Exception {
        app = ctx.getConfiguration().getAppender("Class");
        assertNotNull("No ListAppender", app);
        app.clear();
        final Logger logger = Logger.getLogger("ClassLogger");
        logger.info("Ignored message contents.");
        logger.warning("Verifying the caller class is still correct.");
        logger.severe("Hopefully nobody breaks me!");
        final List<String> messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 3, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller class name.", this.getClass().getName(), message);
        }
    }

    @Test
    public void testMethodLogger() throws Exception {
        app = ctx.getConfiguration().getAppender("Method");
        assertNotNull("No ListAppender", app);
        app.clear();
        final Logger logger = Logger.getLogger("MethodLogger");
        logger.info("More messages.");
        logger.warning("CATASTROPHE INCOMING!");
        logger.severe("ZOMBIES!!!");
        logger.warning("brains~~~");
        logger.info("Itchy. Tasty.");
        final List<String> messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 5, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller method name.", "testMethodLogger", message);
        }
    }
}
