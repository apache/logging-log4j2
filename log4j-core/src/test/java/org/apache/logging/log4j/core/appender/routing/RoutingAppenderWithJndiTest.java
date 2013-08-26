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
package org.apache.logging.log4j.core.appender.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockejb.jndi.MockContextFactory;

/**
 * RoutingAppenderWithJndiTest
 */
public class RoutingAppenderWithJndiTest {

    private static final String CONFIG = "log4j-routing-by-jndi.xml";
    private static Configuration config;
    private static ListAppender listAppender1;
    private static ListAppender listAppender2;
    private static LoggerContext ctx;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();
        for (final Map.Entry<String, Appender> entry : config.getAppenders().entrySet()) {
            if (entry.getKey().equals("List1")) {
                listAppender1 = (ListAppender) entry.getValue();
            }
            if (entry.getKey().equals("List2")) {
                listAppender2 = (ListAppender) entry.getValue();
            }
        }
    }

    @Before
    public void before() throws NamingException {
        MockContextFactory.setAsInitial();
    }

    @After
    public void after() {
        MockContextFactory.revertSetAsInitial();
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void routingTest() throws NamingException {
        // default route when there's no jndi resource
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a message from unknown context", "Context");
        EventLogger.logEvent(msg);
        File defaultLogFile = new File("target/routingbyjndi/routingbyjnditest-default.log");
        assertTrue("The default log file was not created", defaultLogFile.exists());

        // now set jndi resource to Application1
        Context context = new InitialContext();
        context.bind("java:comp/env/logging/context-name", "Application1");

        msg = new StructuredDataMessage("Test", "This is a message from Application1", "Context");
        EventLogger.logEvent(msg);
        assertNotNull("No events generated", listAppender1.getEvents());
        assertTrue("Incorrect number of events. Expected 1, got " + listAppender1.getEvents().size(), listAppender1.getEvents().size() == 1);

        // now set jndi resource to Application2
        context.rebind("java:comp/env/logging/context-name", "Application2");

        msg = new StructuredDataMessage("Test", "This is a message from Application2", "Context");
        EventLogger.logEvent(msg);
        assertNotNull("No events generated", listAppender2.getEvents());
        assertTrue("Incorrect number of events. Expected 1, got " + listAppender2.getEvents().size(), listAppender2.getEvents().size() == 1);
        assertTrue("Incorrect number of events. Expected 1, got " + listAppender1.getEvents().size(), listAppender1.getEvents().size() == 1);

        msg = new StructuredDataMessage("Test", "This is another message from Application2", "Context");
        EventLogger.logEvent(msg);
        assertNotNull("No events generated", listAppender2.getEvents());
        assertTrue("Incorrect number of events. Expected 2, got " + listAppender2.getEvents().size(), listAppender2.getEvents().size() == 2);
        assertTrue("Incorrect number of events. Expected 1, got " + listAppender1.getEvents().size(), listAppender1.getEvents().size() == 1);
    }
}
