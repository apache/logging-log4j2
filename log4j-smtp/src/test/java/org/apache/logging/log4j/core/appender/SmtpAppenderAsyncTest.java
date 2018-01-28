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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.dumbster.smtp.SimpleSmtpServer;
import org.apache.logging.dumbster.smtp.SmtpMessage;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SmtpAppenderAsyncTest {

    private static int PORT;

    private SimpleSmtpServer smtpServer;

    @BeforeClass
    public static void setupClass() {
        PORT = AvailablePortFinder.getNextAvailable();
        System.setProperty("smtp.port", String.valueOf(PORT));
    }

    @Before
    public void setup() {
        smtpServer = SimpleSmtpServer.start(PORT);
    }

    @Rule
    public LoggerContextRule ctx = new LoggerContextRule("SmtpAppenderAsyncTest.xml");

    @Test
    public void testSync() {
        testSmtpAppender(ctx.getLogger("sync"));
    }

    @Test
    public void testAsync() {
        testSmtpAppender(ctx.getLogger("async"));
    }

    private void testSmtpAppender(Logger logger) {
        ThreadContext.put("MDC1", "mdc1");
        logger.error("the message");
        ctx.getLoggerContext().stop();
        smtpServer.stop();

        assertEquals(1, smtpServer.getReceivedEmailSize());
        final Iterator<SmtpMessage> messages = smtpServer.getReceivedEmail();
        final SmtpMessage email = messages.next();

        assertEquals("to@example.com", email.getHeaderValue("To"));
        assertEquals("from@example.com", email.getHeaderValue("From"));
        assertEquals("[mdc1]", email.getHeaderValue("Subject"));

        String body = email.getBody();
        if (!body.contains("Body:[mdc1]")) {
            fail(body);
        }
    }

    @After
    public void teardown() {
        if (smtpServer != null) {
            smtpServer.stop();
        }
    }

    @AfterClass
    public static void teardownClass() {
        System.clearProperty("smtp.port");
    }
}