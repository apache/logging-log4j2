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

import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.MailManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.message.SimpleMessage;

final class SmtpTestSupport {

    private SmtpTestSupport() {}

    static MailManager.FactoryData createFactoryData(
            final String to,
            final String cc,
            final String bcc,
            final String from,
            final String replyTo,
            final String subjectPattern,
            final String smtpProtocol,
            final String smtpHost,
            final int smtpPort,
            final String smtpUsername,
            final String smtpPassword,
            final boolean smtpDebug,
            final int bufferSize,
            final SslConfiguration sslConfiguration) {
        return new MailManager.FactoryData(
                to,
                cc,
                bcc,
                from,
                replyTo,
                subjectPattern,
                PatternLayout.newSerializerBuilder().setPattern(subjectPattern).build(),
                smtpProtocol,
                smtpHost,
                smtpPort,
                smtpUsername,
                smtpPassword,
                smtpDebug,
                bufferSize,
                sslConfiguration,
                null);
    }

    static MailManager.FactoryData createDefaultFactoryData() {
        return createFactoryData(
                SmtpMockFixtures.MOCK_TO,
                SmtpMockFixtures.MOCK_CC,
                SmtpMockFixtures.MOCK_BCC,
                SmtpMockFixtures.MOCK_FROM,
                SmtpMockFixtures.MOCK_REPLY_TO,
                "Subject %m",
                "smtp",
                SmtpMockFixtures.MOCK_HOST,
                SmtpMockFixtures.MOCK_PORT,
                SmtpMockFixtures.MOCK_USERNAME,
                SmtpMockFixtures.MOCK_PASSWORD,
                false,
                3,
                null);
    }

    static SmtpManager createManager(final MailManager.FactoryData data) {
        return new SmtpManager.SMTPManagerFactory().createManager("test-smtp-manager", data);
    }

    static CapturingSmtpManager createCapturingManager(final MailManager.FactoryData data) {
        final SmtpManager manager = createManager(data);
        return new CapturingSmtpManager(manager.getName(), getSession(manager), data);
    }

    static Session getSession(final SmtpManager manager) {
        try {
            final Field sessionField = SmtpManager.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            return (Session) sessionField.get(manager);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read SmtpManager session", e);
        }
    }

    static LogEvent createLogEvent(final String message) {
        return new Log4jLogEvent.Builder()
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(message))
                .build();
    }

    static PatternLayout createLayout() {
        return PatternLayout.newBuilder().setPattern("%m%n").build();
    }

    static String extractBody(final MimeMessage message) throws Exception {
        final Object content = message.getContent();
        if (!(content instanceof MimeMultipart)) {
            return content == null ? "" : content.toString();
        }
        final MimeMultipart multipart = (MimeMultipart) content;
        final MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(0);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            part.writeTo(out);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static String readIso8859(final java.io.InputStream in) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[256];
        int read;
        while ((read = in.read(chunk)) >= 0) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.ISO_8859_1);
    }
}
