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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

class SmtpManagerMimeMessageTest {

    @Test
    void sendEventsCreatesMimeMessageWithExpectedHeaders() throws Exception {
        final CapturingSmtpManager manager =
                SmtpTestSupport.createCapturingManager(SmtpTestSupport.createDefaultFactoryData());
        final PatternLayout layout = SmtpTestSupport.createLayout();
        final String triggerMessage = "trigger-event";

        manager.sendEvents(layout, SmtpTestSupport.createLogEvent(triggerMessage));

        assertEquals(1, manager.getCapturedEmails().size());
        final CapturingSmtpManager.CapturedEmail captured =
                manager.getCapturedEmails().get(0);
        final MimeMessage message = captured.getMessage();
        final String body = captured.getBody();

        assertArrayEquals(InternetAddress.parse(SmtpMockFixtures.MOCK_FROM), message.getFrom());
        assertArrayEquals(InternetAddress.parse(SmtpMockFixtures.MOCK_REPLY_TO), message.getReplyTo());
        assertArrayEquals(
                InternetAddress.parse(SmtpMockFixtures.MOCK_TO), message.getRecipients(Message.RecipientType.TO));
        assertArrayEquals(
                InternetAddress.parse(SmtpMockFixtures.MOCK_CC), message.getRecipients(Message.RecipientType.CC));
        assertArrayEquals(
                InternetAddress.parse(SmtpMockFixtures.MOCK_BCC), message.getRecipients(Message.RecipientType.BCC));
        assertEquals("Subject " + triggerMessage, message.getSubject());
        assertTrue(body.contains(triggerMessage));
        assertTrue(body.contains("Content-Type: text/plain; charset=UTF-8"));
    }

    @Test
    void mimeMessageBuilderMatchesJakartaHeaders() throws Exception {
        final MimeMessage message = SmtpMockFixtures.createSampleMimeMessage(SmtpMockFixtures.createMailSession());

        assertArrayEquals(InternetAddress.parse(SmtpMockFixtures.MOCK_FROM), message.getFrom());
        assertArrayEquals(InternetAddress.parse(SmtpMockFixtures.MOCK_REPLY_TO), message.getReplyTo());
        assertArrayEquals(
                InternetAddress.parse(SmtpMockFixtures.MOCK_TO), message.getRecipients(Message.RecipientType.TO));
        assertArrayEquals(
                InternetAddress.parse(SmtpMockFixtures.MOCK_CC), message.getRecipients(Message.RecipientType.CC));
        assertArrayEquals(
                InternetAddress.parse(SmtpMockFixtures.MOCK_BCC), message.getRecipients(Message.RecipientType.BCC));
        assertEquals("Mock Subject", message.getSubject());
    }
}
