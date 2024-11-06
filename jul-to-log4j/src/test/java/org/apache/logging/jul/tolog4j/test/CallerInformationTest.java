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
package org.apache.logging.jul.tolog4j.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.jul.tolog4j.LogManager;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class CallerInformationTest {

    private static final String PARAM_1 = "PARAM_1";
    private static final String[] PARAMS = {PARAM_1, "PARAM_2"};
    private static final String SOURCE_CLASS = "SourceClass";
    private static final String SOURCE_METHOD = "sourceMethod";

    @Rule
    public final LoggerContextRule ctx = new LoggerContextRule("CallerInformationTest.xml");

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
    }

    @Test
    public void testClassLogger() {
        final ListAppender app = ctx.getListAppender("Class").clear();
        final Logger logger = Logger.getLogger("ClassLogger");
        // Eager methods
        logger.severe("CATASTROPHE INCOMING!");
        logger.warning("ZOMBIES!!!");
        logger.info("brains~~~");
        logger.config("Config!");
        logger.fine("Itchy. Tasty.");
        logger.finer("Finer message.");
        logger.finest("Finest message.");
        logger.log(Level.FINEST, "Finest message.");
        logger.log(Level.FINEST, "Message of level {1}.", Level.FINEST);
        logger.log(Level.FINEST, "Hello {1} and {2}!.", new Object[] {"foo", "bar"});
        // Lazy methods
        logger.severe(() -> "CATASTROPHE INCOMING!");
        logger.warning(() -> "ZOMBIES!!!");
        logger.info(() -> "brains~~~");
        logger.config(() -> "Config!");
        logger.fine(() -> "Itchy. Tasty.");
        logger.finer(() -> "Finer message.");
        logger.finest(() -> "Finest message.");
        logger.log(Level.FINEST, () -> "Finest message.");
        logger.log(Level.FINEST, new RuntimeException(), () -> "Message with exception.");
        List<String> messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 19, messages.size());
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            assertEquals(
                    "Incorrect caller class name for message " + i,
                    this.getClass().getName(),
                    message);
        }

        // Test passing the location information directly
        app.clear();
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1}!", PARAM_1);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1} and {2}!", PARAMS);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!", new RuntimeException());
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, () -> "Hello" + PARAM_1 + "!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, new RuntimeException(), () -> "Hello " + PARAM_1 + "!");
        logger.entering(SOURCE_CLASS, SOURCE_METHOD);
        logger.entering(SOURCE_CLASS, SOURCE_METHOD, PARAM_1);
        logger.entering(SOURCE_CLASS, SOURCE_METHOD, PARAMS);
        logger.exiting(SOURCE_CLASS, SOURCE_METHOD);
        logger.exiting(SOURCE_CLASS, SOURCE_METHOD, PARAM_1);
        logger.throwing(SOURCE_CLASS, SOURCE_METHOD, new RuntimeException());
        messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 12, messages.size());
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            assertEquals("Incorrect caller class name for message " + i, SOURCE_CLASS, message);
        }
    }

    @Test
    public void testMethodLogger() {
        final ListAppender app = ctx.getListAppender("Method").clear();
        final Logger logger = Logger.getLogger("MethodLogger");
        // Eager methods
        logger.severe("CATASTROPHE INCOMING!");
        logger.warning("ZOMBIES!!!");
        logger.info("brains~~~");
        logger.config("Config!");
        logger.fine("Itchy. Tasty.");
        logger.finer("Finer message.");
        logger.finest("Finest message.");
        logger.log(Level.FINEST, "Finest message.");
        logger.log(Level.FINEST, "Message of level {1}.", Level.FINEST);
        logger.log(Level.FINEST, "Hello {1} and {2}!.", new Object[] {"foo", "bar"});
        // Lazy methods
        logger.severe(() -> "CATASTROPHE INCOMING!");
        logger.warning(() -> "ZOMBIES!!!");
        logger.info(() -> "brains~~~");
        logger.config(() -> "Config!");
        logger.fine(() -> "Itchy. Tasty.");
        logger.finer(() -> "Finer message.");
        logger.finest(() -> "Finest message.");
        logger.log(Level.FINEST, () -> "Finest message.");
        logger.log(Level.FINEST, new RuntimeException(), () -> "Message with exception.");
        List<String> messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 19, messages.size());
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            assertEquals("Incorrect caller class name for message " + i, "testMethodLogger", message);
        }

        // Test passing the location information directly
        app.clear();
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1}!", PARAM_1);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello {1} and {2}!", PARAMS);
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, "Hello!", new RuntimeException());
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, () -> "Hello " + PARAM_1 + "!");
        logger.logp(Level.INFO, SOURCE_CLASS, SOURCE_METHOD, new RuntimeException(), () -> "Hello " + PARAM_1 + "!");
        logger.entering(SOURCE_CLASS, SOURCE_METHOD);
        logger.entering(SOURCE_CLASS, SOURCE_METHOD, PARAM_1);
        logger.entering(SOURCE_CLASS, SOURCE_METHOD, PARAMS);
        logger.exiting(SOURCE_CLASS, SOURCE_METHOD);
        logger.exiting(SOURCE_CLASS, SOURCE_METHOD, PARAM_1);
        logger.throwing(SOURCE_CLASS, SOURCE_METHOD, new RuntimeException());
        messages = app.getMessages();
        assertEquals("Incorrect number of messages.", 12, messages.size());
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            assertEquals("Incorrect caller class name for message " + i, SOURCE_METHOD, message);
        }
    }
}
