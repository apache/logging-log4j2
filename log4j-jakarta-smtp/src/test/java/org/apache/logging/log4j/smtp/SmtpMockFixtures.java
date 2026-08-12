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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Reusable Mockito fixtures for jakarta.mail types used by SMTP manager tests.
 */
public final class SmtpMockFixtures {

    public static final String MOCK_HOST = "mock.smtp.example.com";
    public static final int MOCK_PORT = 2525;
    public static final String MOCK_USERNAME = "smtp-user";
    public static final String MOCK_PASSWORD = "smtp-secret";
    public static final String MOCK_FROM = "from@example.com";
    public static final String MOCK_TO = "to@example.com";
    public static final String MOCK_CC = "cc@example.com";
    public static final String MOCK_BCC = "bcc@example.com";
    public static final String MOCK_REPLY_TO = "reply@example.com";

    private final Session session = mock(Session.class);
    private final Transport transport = mock(Transport.class);
    private final List<MimeMessage> sentMessages = new ArrayList<>();

    private SmtpMockFixtures() throws MessagingException {
        configureSuccessfulTransport();
    }

    public static SmtpMockFixtures create() throws MessagingException {
        return new SmtpMockFixtures();
    }

    public static Properties loadMockSessionProperties() throws IOException {
        final Properties properties = new Properties();
        try (InputStream in =
                SmtpMockFixtures.class.getClassLoader().getResourceAsStream("mock-mail-session.properties")) {
            if (in != null) {
                properties.load(in);
            }
        }
        return properties;
    }

    public static Session createMailSession() throws IOException {
        return Session.getInstance(loadMockSessionProperties());
    }

    private void configureSuccessfulTransport() throws MessagingException {
        given(session.getTransport()).willReturn(transport);
        given(session.getTransport(any(String.class))).willReturn(transport);
        given(transport.isConnected()).willReturn(false, true);
    }

    public Session getSession() {
        return session;
    }

    public Transport getTransport() {
        return transport;
    }

    public List<MimeMessage> getSentMessages() {
        return sentMessages;
    }

    public void recordSentMessage(final MimeMessage message) {
        sentMessages.add(message);
    }

    public static MimeMessage createSampleMimeMessage(final Session session) throws MessagingException {
        return new MimeMessageBuilder(session)
                .setFrom(MOCK_FROM)
                .setReplyTo(MOCK_REPLY_TO)
                .setRecipients(Message.RecipientType.TO, MOCK_TO)
                .setRecipients(Message.RecipientType.CC, MOCK_CC)
                .setRecipients(Message.RecipientType.BCC, MOCK_BCC)
                .setSubject("Mock Subject")
                .build();
    }

    public static Address[] parseAddresses(final String addresses) throws MessagingException {
        return InternetAddress.parse(addresses);
    }
}
