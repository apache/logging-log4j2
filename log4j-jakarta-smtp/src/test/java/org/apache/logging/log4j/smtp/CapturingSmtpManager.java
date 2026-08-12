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

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.logging.log4j.core.net.MailManager;

/**
 * Test {@link SmtpManager} that captures outbound messages instead of calling {@code Transport.send}.
 */
final class CapturingSmtpManager extends SmtpManager {

    private final List<CapturedEmail> capturedEmails = new ArrayList<>();
    private MessagingException sendFailure;

    CapturingSmtpManager(final String name, final Session session, final MailManager.FactoryData data) {
        super(name, session, null, data);
    }

    void setSendFailure(final MessagingException sendFailure) {
        this.sendFailure = sendFailure;
    }

    List<CapturedEmail> getCapturedEmails() {
        return capturedEmails;
    }

    @Override
    protected void sendMultipartMessage(final MimeMessage msg, final MimeMultipart mp, final String subject)
            throws MessagingException {
        if (sendFailure != null) {
            throw sendFailure;
        }
        synchronized (msg) {
            msg.setContent(mp);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            try {
                capturedEmails.add(new CapturedEmail(msg, subject));
            } catch (Exception e) {
                throw new MessagingException("Unable to capture SMTP message for testing", e);
            }
        }
    }

    static final class CapturedEmail {
        private final MimeMessage message;
        private final String subject;
        private final String body;

        CapturedEmail(final MimeMessage message, final String subject) throws Exception {
            this.message = message;
            this.subject = subject;
            this.body = SmtpTestSupport.extractBody(message);
        }

        MimeMessage getMessage() {
            return message;
        }

        String getSubject() {
            return subject;
        }

        String getBody() {
            return body;
        }
    }
}
