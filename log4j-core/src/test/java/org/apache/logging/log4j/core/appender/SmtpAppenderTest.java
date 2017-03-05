/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import java.util.Iterator;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.logging.dumbster.smtp.SimpleSmtpServer;
import org.apache.logging.dumbster.smtp.SmtpMessage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.MimeMessageBuilder;
import org.apache.logging.log4j.core.util.CyclicBuffer;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Smtp.class)
public class SmtpAppenderTest {

    private static final String HOST = "localhost";
    private static final int PORTNUM = AvailablePortFinder.getNextAvailable();
    private static final String PORT = String.valueOf(PORTNUM);

    @Test
    public void testMessageFactorySetFrom() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String address = "testing@example.com";

        assertNull(builder.build().getFrom());

        builder.setFrom(null);
        Address[] array = null;
        final Address addr = InternetAddress.getLocalAddress(null);
        if (addr != null) {
            array = new Address[] { addr };
        }
        assertArrayEquals(array, builder.build().getFrom());

        builder.setFrom(address);
        assertArrayEquals(new Address[] { new InternetAddress(address) },
                builder.build().getFrom());
    }

    @Test
    public void testMessageFactorySetReplyTo() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String addresses = "testing1@example.com,testing2@example.com";

        assertNull(builder.build().getReplyTo());

        builder.setReplyTo(null);
        assertNull(builder.build().getReplyTo());

        builder.setReplyTo(addresses);
        assertArrayEquals(InternetAddress.parse(addresses), builder
                .build().getReplyTo());
    }

    @Test
    public void testMessageFactorySetRecipients() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String addresses = "testing1@example.com,testing2@example.com";

        assertNull(builder.build().getRecipients(
                Message.RecipientType.TO));

        builder.setRecipients(Message.RecipientType.TO, null);
        assertNull(builder.build().getRecipients(
                Message.RecipientType.TO));

        builder.setRecipients(Message.RecipientType.TO, addresses);
        assertArrayEquals(InternetAddress.parse(addresses), builder
                .build().getRecipients(Message.RecipientType.TO));
    }

    @Test
    public void testMessageFactorySetSubject() throws MessagingException {
        final MimeMessageBuilder builder = new MimeMessageBuilder(null);
        final String subject = "Test Subject";

        assertNull(builder.build().getSubject());

        builder.setSubject(null);
        assertNull(builder.build().getSubject());

        builder.setSubject(subject);
        assertEquals(subject, builder.build().getSubject());
    }

    @Test
    public void testCyclicBuffer() {
        final CyclicBuffer<Integer> buffer = new CyclicBuffer<>(
                Integer.class, 3);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        assertFalse(buffer.isEmpty());
        Integer[] items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 1);

        assertTrue(buffer.isEmpty());
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        items = buffer.removeAll();
        assertTrue("Incorrect number of items", items.length == 3);
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void testDelivery() {
        final String subjectKey = getClass().getName();
        final String subjectValue = "SubjectValue1";
        ThreadContext.put(subjectKey, subjectValue);
        final SmtpAppender appender = SmtpAppender.createAppender(null, "Test", "to@example.com", "cc@example.com",
                "bcc@example.com", "from@example.com", "replyTo@example.com", "Subject Pattern %X{" + subjectKey + "}",
                null, HOST, PORT, null, null, "false", "3", null, null, "true");
        appender.start();

        final LoggerContext context = LoggerContext.getContext();
        final Logger root = context.getLogger("SMTPAppenderTest");
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);

        final SimpleSmtpServer server = SimpleSmtpServer.start(PORTNUM);

        root.debug("Debug message #1");
        root.debug("Debug message #2");
        root.debug("Debug message #3");
        root.debug("Debug message #4");
        root.error("Error with exception", new RuntimeException("Exception message"));
        root.error("Error message #2");

        server.stop();
        assertTrue(server.getReceivedEmailSize() == 2);
        final Iterator<SmtpMessage> messages = server.getReceivedEmail();
        final SmtpMessage email = messages.next();

        assertEquals("to@example.com", email.getHeaderValue("To"));
        assertEquals("cc@example.com", email.getHeaderValue("Cc"));
        // assertEquals("bcc@example.com", email.getHeaderValue("Bcc")); // BCC
        // can't be tested with Dumpster 1.6
        assertEquals("from@example.com", email.getHeaderValue("From"));
        assertEquals("replyTo@example.com", email.getHeaderValue("Reply-To"));
        final String headerValue = email.getHeaderValue("Subject");
        assertEquals(headerValue, "Subject Pattern " + subjectValue);

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
        final String body2 = email2.getBody();
        assertFalse(body2.contains("Debug message #4"));
        assertFalse(body2.contains("Error with exception"));
        assertTrue(body2.contains("Error message #2"));
    }
}
