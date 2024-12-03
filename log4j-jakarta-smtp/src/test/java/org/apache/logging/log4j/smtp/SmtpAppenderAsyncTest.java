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
package org.apache.logging.log4j.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.AvailablePortFinder;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.smtp.SimpleSmtpServer;
import org.apache.logging.log4j.core.test.smtp.SmtpMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SmtpAppenderAsyncTest {

    private static int PORT;

    private SimpleSmtpServer smtpServer;

    @BeforeAll
    static void setupAll() {
        PORT = AvailablePortFinder.getNextAvailable();
        System.setProperty("smtp.port", String.valueOf(PORT));
    }

    @BeforeEach
    void setup() {
        smtpServer = SimpleSmtpServer.start(PORT);
    }

    @Test
    @LoggerContextSource("SmtpAppenderAsyncTest.xml")
    void testSync(final LoggerContext ctx) {
        testSmtpAppender(ctx.getLogger("sync"), ctx);
    }

    @Test
    @LoggerContextSource("SmtpAppenderAsyncTest.xml")
    void testAsync(final LoggerContext ctx) {
        testSmtpAppender(ctx.getLogger("async"), ctx);
    }

    private void testSmtpAppender(final Logger logger, final LoggerContext ctx) {
        ThreadContext.put("MDC1", "mdc1");
        logger.error("the message");
        ctx.stop();
        smtpServer.stop();

        assertEquals(1, smtpServer.getReceivedEmailSize());
        final Iterator<SmtpMessage> messages = smtpServer.getReceivedEmail();
        final SmtpMessage email = messages.next();

        assertEquals("to@example.com", email.getHeaderValue("To"));
        assertEquals("from@example.com", email.getHeaderValue("From"));
        assertEquals("[mdc1]", email.getHeaderValue("Subject"));

        final String body = email.getBody();
        if (!body.contains("Body:[mdc1]")) {
            fail(body);
        }
    }

    @AfterEach
    void teardown() {
        if (smtpServer != null) {
            smtpServer.stop();
        }
    }

    @AfterAll
    static void teardownAll() {
        System.clearProperty("smtp.port");
    }
}
