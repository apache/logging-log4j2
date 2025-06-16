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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.MimeMessageBuilder;
import org.apache.logging.log4j.core.net.SmtpManager;
import org.apache.logging.log4j.core.test.AvailablePortFinder;
import org.apache.logging.log4j.core.test.smtp.SimpleSmtpServer;
import org.apache.logging.log4j.core.test.smtp.SmtpMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Smtp")
class SmtpAppenderTest {

    private static final String HOST = "localhost";

    @Test
    void testMessageFactorySetFrom() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String address = "testing@example.com";

        assertNull(builder.build().getFrom());

        builder.setFrom(null);
        Address[] array = null;
        final Address addr = InternetAddress.getLocalAddress(null);
        if (addr != null) {
            array = new Address[] {addr};
        }
        assertArrayEquals(array, builder.build().getFrom());

        builder.setFrom(address);
        assertArrayEquals(
                new Address[] {new InternetAddress(address)}, builder.build().getFrom());
    }

    @Test
    void testMessageFactorySetReplyTo() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String addresses = "testing1@example.com,testing2@example.com";

        assertNull(builder.build().getReplyTo());

        builder.setReplyTo(null);
        assertNull(builder.build().getReplyTo());

        builder.setReplyTo(addresses);
        assertArrayEquals(InternetAddress.parse(addresses), builder.build().getReplyTo());
    }

    @Test
    void testMessageFactorySetRecipients() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String addresses = "testing1@example.com,testing2@example.com";

        assertNull(builder.build().getRecipients(Message.RecipientType.TO));

        builder.setRecipients(Message.RecipientType.TO, null);
        assertNull(builder.build().getRecipients(Message.RecipientType.TO));

        builder.setRecipients(Message.RecipientType.TO, addresses);
        assertArrayEquals(InternetAddress.parse(addresses), builder.build().getRecipients(Message.RecipientType.TO));
    }

    @Test
    void testMessageFactorySetSubject() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String subject = "Test Subject";

        assertNull(builder.build().getSubject());

        builder.setSubject(null);
        assertNull(builder.build().getSubject());

        builder.setSubject(subject);
        assertEquals(subject, builder.build().getSubject());
    }

    @Test
    void testDelivery() {
        final String subjectKey = getClass().getName();
        final String subjectValue = "SubjectValue1";
        ThreadContext.put(subjectKey, subjectValue);
        final int smtpPort = AvailablePortFinder.getNextAvailable();
        final SmtpAppender appender = SmtpAppender.newBuilder()
                .setName("Test")
                .setTo("to@example.com")
                .setCc("cc@example.com")
                .setBcc("bcc@example.com")
                .setFrom("from@example.com")
                .setReplyTo("replyTo@example.com")
                .setSubject("Subject Pattern %X{" + subjectKey + "} %maxLen{%m}{10}")
                .setSmtpHost(HOST)
                .setSmtpPort(smtpPort)
                .setBufferSize(3)
                .build();
        assertNotNull(appender);
        assertInstanceOf(SmtpManager.class, appender.getManager());
        appender.start();

        final LoggerContext context = LoggerContext.getContext();
        final Logger root = context.getLogger("SMTPAppenderTest");
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);

        final SimpleSmtpServer server = SimpleSmtpServer.start(smtpPort);

        root.debug("Debug message #1");
        root.debug("Debug message #2");
        root.debug("Debug message #3");
        root.debug("Debug message #4");
        root.error("Error with exception", new RuntimeException("Exception message"));
        root.error("Error message #2");

        server.stop();
        assertEquals(2, server.getReceivedEmailSize());
        final Iterator<SmtpMessage> messages = server.getReceivedEmail();
        final SmtpMessage email = messages.next();

        assertEquals("to@example.com", email.getHeaderValue("To"));
        assertEquals("cc@example.com", email.getHeaderValue("Cc"));
        // assertEquals("bcc@example.com", email.getHeaderValue("Bcc")); // BCC
        // can't be tested with Dumpster 1.6
        assertEquals("from@example.com", email.getHeaderValue("From"));
        assertEquals("replyTo@example.com", email.getHeaderValue("Reply-To"));
        assertEquals("Subject Pattern " + subjectValue + " Error with", email.getHeaderValue("Subject"));

        final String body = email.getBody();
        assertFalse(body.contains("Debug message #1"));
        assertTrue(body.contains("Debug message #2"));
        assertTrue(body.contains("Debug message #3"));
        assertTrue(body.contains("Debug message #4"));
        assertTrue(body.contains("Error with exception"));
        assertTrue(body.contains("RuntimeException"));
        assertTrue(body.contains("Exception message"));
        assertFalse(body.contains("Error message #2"));

        final SmtpMessage email2 = messages.next();

        assertEquals("Subject Pattern " + subjectValue + " Error mess", email2.getHeaderValue("Subject"));

        final String body2 = email2.getBody();
        assertFalse(body2.contains("Debug message #4"));
        assertFalse(body2.contains("Error with exception"));
        assertTrue(body2.contains("Error message #2"));
    }
}
