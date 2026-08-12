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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

class SmtpManagerIntegrationTest {

    @Test
    void endToEndSendUsesMockTransportFixtures() throws Exception {
        final SmtpMockFixtures fixtures = SmtpMockFixtures.create();
        final CapturingSmtpManager manager =
                SmtpTestSupport.createCapturingManager(SmtpTestSupport.createDefaultFactoryData());
        final PatternLayout layout = SmtpTestSupport.createLayout();

        manager.add(SmtpTestSupport.createLogEvent("integration-buffer"));
        manager.sendEvents(layout, SmtpTestSupport.createLogEvent("integration-trigger"));

        assertEquals(1, manager.getCapturedEmails().size());
        final CapturingSmtpManager.CapturedEmail captured =
                manager.getCapturedEmails().get(0);
        fixtures.recordSentMessage(captured.getMessage());

        assertEquals(1, fixtures.getSentMessages().size());
        assertTrue(captured.getBody().contains("integration-buffer"));
        assertTrue(captured.getBody().contains("integration-trigger"));
    }

    @Test
    void transportFailureThrowsLoggingExceptionWithoutLeakingCredentials() throws Exception {
        final CapturingSmtpManager manager =
                SmtpTestSupport.createCapturingManager(SmtpTestSupport.createDefaultFactoryData());
        manager.setSendFailure(new MessagingException("SMTP connection refused"));
        final PatternLayout layout = SmtpTestSupport.createLayout();

        final LoggingException thrown = assertThrows(
                LoggingException.class,
                () -> manager.sendEvents(layout, SmtpTestSupport.createLogEvent("failure-trigger")));

        assertTrue(thrown.getMessage().contains("Error occurred while sending email"));
        assertFalse(String.valueOf(thrown.getCause()).contains(SmtpMockFixtures.MOCK_PASSWORD));
    }

    @Test
    void mockTransportCanBeConnectedAndClosed() throws Exception {
        final SmtpMockFixtures fixtures = SmtpMockFixtures.create();
        final Transport transport = fixtures.getTransport();

        transport.connect(SmtpMockFixtures.MOCK_HOST, SmtpMockFixtures.MOCK_USERNAME, SmtpMockFixtures.MOCK_PASSWORD);
        transport.close();

        then(transport)
                .should(times(1))
                .connect(SmtpMockFixtures.MOCK_HOST, SmtpMockFixtures.MOCK_USERNAME, SmtpMockFixtures.MOCK_PASSWORD);
        then(transport).should(times(1)).close();
    }
}
