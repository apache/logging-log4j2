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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.logging.dumbster.smtp.SimpleSmtpServer;
import org.apache.logging.dumbster.smtp.SmtpMessage;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.MimeMessageBuilder;
import org.apache.logging.log4j.core.net.SmtpManager;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.*;

@Category(Appenders.Smtp.class)
public class SmtpAppenderTest {

    private static final String HOST = "localhost";

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
    public void testDelivery() {
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
        assertTrue(server.getReceivedEmailSize() == 2);
        final Iterator<SmtpMessage> messages = server.getReceivedEmail();
        final SmtpMessage email = messages.next();

        assertEquals("to@example.com", email.getHeaderValue("To"));
        assertEquals("cc@example.com", email.getHeaderValue("Cc"));
        // assertEquals("bcc@example.com", email.getHeaderValue("Bcc")); // BCC
        // can't be tested with Dumpster 1.6
        assertEquals("from@example.com", email.getHeaderValue("From"));
        assertEquals("replyTo@example.com", email.getHeaderValue("Reply-To"));
        assertEquals("Subject Pattern " + subjectValue +" Error with", email.getHeaderValue("Subject"));

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

        assertEquals("Subject Pattern " + subjectValue +" Error mess", email2.getHeaderValue("Subject"));

        final String body2 = email2.getBody();
        assertFalse(body2.contains("Debug message #4"));
        assertFalse(body2.contains("Error with exception"));
        assertTrue(body2.contains("Error message #2"));
    }

    @Test // delivering email to a fake server is slow, instead quickly verify msg structure
    public void testNoAttachment() {
        String one = UUID.randomUUID().toString();
        String two = UUID.randomUUID().toString();
        String three = UUID.randomUUID().toString();

        List<MimeMessageParser> messages = testAttachment(
                builder -> {
                    builder.setBufferSize(3); },
                log -> {
                    log.debug(one);
                    log.error(two, new RuntimeException(three)); });

        assertTrue(messages.size() == 1);
        MimeMessageParser message = messages.get(0);
        assertNull(message.getPlainContent()); // defaults to HTML
        assertNotNull(message.getHtmlContent()); // defaults to HTML
        assertTrue(message.getHtmlContent().contains(one));
        assertTrue(message.getHtmlContent().contains(two));
        assertTrue(message.getHtmlContent().contains(three));

        assertTrue(message.getAttachmentList().size() == 0);
    }

    @Test // delivering email to a fake server is slow, instead quickly verify msg structure
    public void testAttachmentHtml() throws IOException {
        String bodyUuid = UUID.randomUUID().toString();
        String exceptionUuid = UUID.randomUUID().toString();
        String attachmentUuid = UUID.randomUUID().toString();

        List<MimeMessageParser> messages = testAttachment(
                builder -> {
                    builder.setBufferSize(3);
                    builder.setAttachEvents(true); },
                log -> {
                    log.debug(attachmentUuid);
                    log.error(bodyUuid, new RuntimeException(exceptionUuid)); });

        assertTrue(messages.size() == 1);
        MimeMessageParser message = messages.get(0);
        assertNull(message.getPlainContent()); // defaults to HTML
        assertNotNull(message.getHtmlContent()); // defaults to HTML
        assertTrue(message.getHtmlContent().contains(bodyUuid));
        assertTrue(message.getHtmlContent().contains(exceptionUuid));
        assertFalse(message.getHtmlContent().contains(attachmentUuid));

        assertTrue(message.getAttachmentList().size() == 1);
        DataSource attachment = message.getAttachmentList().get(0);
        assertTrue(attachment.getContentType().contains("text/html"));
        assertTrue(attachment.getName().equals("logEvents.html"));

        String logEventsHtml = IOUtils.toString(attachment.getInputStream(), "UTF-8");
        assertTrue(logEventsHtml.contains(bodyUuid));
        assertTrue(logEventsHtml.contains(exceptionUuid));
        assertTrue(logEventsHtml.contains(attachmentUuid));
    }

    @Test // delivering email to a fake server is slow, instead quickly verify msg structure
    public void testAttachmentTxt() throws IOException {
        String bodyUuid = UUID.randomUUID().toString();
        String exceptionUuid = UUID.randomUUID().toString();
        String attachmentUuid = UUID.randomUUID().toString();

        List<MimeMessageParser> messages = testAttachment(
                builder -> {
                    builder.setBufferSize(3);
                    builder.setAttachEvents(true);
                    builder.setLayout(PatternLayout.createDefaultLayout()); },
                log -> {
                    log.debug(attachmentUuid);
                    log.error(bodyUuid, new RuntimeException(exceptionUuid)); });

        assertTrue(messages.size() == 1);
        MimeMessageParser message = messages.get(0);
        assertNotNull(message.getPlainContent());
        assertNull(message.getHtmlContent());
        assertTrue(message.getPlainContent().contains(bodyUuid));
        assertTrue(message.getPlainContent().contains(exceptionUuid));
        assertFalse(message.getPlainContent().contains(attachmentUuid));

        assertTrue(message.getAttachmentList().size() == 1);
        DataSource attachment = message.getAttachmentList().get(0);
        assertTrue(attachment.getContentType().contains("text/plain"));
        assertTrue(attachment.getName().equals("logEvents.txt"));

        String logEventsTxt = IOUtils.toString(attachment.getInputStream(), "UTF-8");
        assertTrue(logEventsTxt.contains(bodyUuid));
        assertTrue(logEventsTxt.contains(exceptionUuid));
        assertTrue(logEventsTxt.contains(attachmentUuid));
        assertFalse(logEventsTxt.toLowerCase().contains("doctype"));
    }

    @Test // delivering email to a fake server is slow, instead quickly verify msg structure
    public void testAttachmentZipHtml() throws IOException {
        String bodyUuid = UUID.randomUUID().toString();
        String exceptionUuid = UUID.randomUUID().toString();
        String attachmentUuid = UUID.randomUUID().toString();

        List<MimeMessageParser> messages = testAttachment(
                builder -> {
                    builder.setBufferSize(3);
                    builder.setAttachEvents(true);
                    builder.setAttachEventsCompression(SmtpManager.AttachEventsCompression.ZIP); },
                log -> {
                    log.debug(attachmentUuid);
                    log.error(bodyUuid, new RuntimeException(exceptionUuid)); });

        assertTrue(messages.size() == 1);
        MimeMessageParser message = messages.get(0);
        assertNull(message.getPlainContent());
        assertNotNull(message.getHtmlContent());
        assertTrue(message.getHtmlContent().contains(bodyUuid));
        assertTrue(message.getHtmlContent().contains(exceptionUuid));
        assertFalse(message.getHtmlContent().contains(attachmentUuid));

        assertTrue(message.getAttachmentList().size() == 1);
        DataSource attachment = message.getAttachmentList().get(0);
        assertTrue(attachment.getContentType().contains("application/zip"));
        assertTrue(attachment.getName().equals("logEvents.html.zip"));

        String logEventsHtml = decompressZipAndAssert(attachment.getInputStream());
        assertTrue(logEventsHtml.contains(bodyUuid));
        assertTrue(logEventsHtml.contains(exceptionUuid));
        assertTrue(logEventsHtml.contains(attachmentUuid));
        assertTrue(logEventsHtml.toLowerCase().contains("doctype"));
    }

    @Test // delivering email to a fake server is slow, instead quickly verify msg structure
    public void testAttachmentZipTxt() throws IOException {
        String bodyUuid = UUID.randomUUID().toString();
        String exceptionUuid = UUID.randomUUID().toString();
        String attachmentUuid = UUID.randomUUID().toString();

        List<MimeMessageParser> messages = testAttachment(
                builder -> {
                    builder.setBufferSize(3);
                    builder.setAttachEvents(true);
                    builder.setLayout(PatternLayout.createDefaultLayout());
                    builder.setAttachEventsCompression(SmtpManager.AttachEventsCompression.ZIP); },
                log -> {
                    log.debug(attachmentUuid);
                    log.error(bodyUuid, new RuntimeException(exceptionUuid)); });

        assertTrue(messages.size() == 1);
        MimeMessageParser message = messages.get(0);
        assertNotNull(message.getPlainContent());
        assertNull(message.getHtmlContent());
        assertTrue(message.getPlainContent().contains(bodyUuid));
        assertTrue(message.getPlainContent().contains(exceptionUuid));
        assertFalse(message.getPlainContent().contains(attachmentUuid));

        assertTrue(message.getAttachmentList().size() == 1);
        DataSource attachment = message.getAttachmentList().get(0);
        assertTrue(attachment.getContentType().contains("application/zip"));
        assertTrue(attachment.getName().equals("logEvents.txt.zip"));

        String logEventsTxt = decompressZipAndAssert(attachment.getInputStream());
        assertTrue(logEventsTxt.contains(bodyUuid));
        assertTrue(logEventsTxt.contains(exceptionUuid));
        assertTrue(logEventsTxt.contains(attachmentUuid));
        assertFalse(logEventsTxt.toLowerCase().contains("doctype"));
    }

    @Test // delivering email to a fake server is slow, instead quickly verify msg structure
    public void testAttachmentGzHtml() throws IOException {
        String bodyUuid = UUID.randomUUID().toString();
        String exceptionUuid = UUID.randomUUID().toString();
        String attachmentUuid = UUID.randomUUID().toString();

        List<MimeMessageParser> messages = testAttachment(
                builder -> {
                    builder.setBufferSize(3);
                    builder.setAttachEvents(true);
                    builder.setAttachEventsCompression(SmtpManager.AttachEventsCompression.GZ); },
                log -> {
                    log.debug(attachmentUuid);
                    log.error(bodyUuid, new RuntimeException(exceptionUuid)); });

        assertTrue(messages.size() == 1);
        MimeMessageParser message = messages.get(0);
        assertNull(message.getPlainContent());
        assertNotNull(message.getHtmlContent());
        assertTrue(message.getHtmlContent().contains(bodyUuid));
        assertTrue(message.getHtmlContent().contains(exceptionUuid));
        assertFalse(message.getHtmlContent().contains(attachmentUuid));

        assertTrue(message.getAttachmentList().size() == 1);
        DataSource attachment = message.getAttachmentList().get(0);
        assertTrue(attachment.getContentType().contains("application/gzip"));
        assertTrue(attachment.getName().equals("logEvents.html.gz"));

        String logEventsHtml = IOUtils.toString(new GZIPInputStream(attachment.getInputStream()), "UTF-8");
        assertTrue(logEventsHtml.contains(bodyUuid));
        assertTrue(logEventsHtml.contains(exceptionUuid));
        assertTrue(logEventsHtml.contains(attachmentUuid));
        assertTrue(logEventsHtml.toLowerCase().contains("doctype"));
    }

    @Test // delivering email to a fake server is slow, instead quickly verify msg structure
    public void testAttachmentGzTxt() throws IOException {
        String bodyUuid = UUID.randomUUID().toString();
        String exceptionUuid = UUID.randomUUID().toString();
        String attachmentUuid = UUID.randomUUID().toString();

        List<MimeMessageParser> messages = testAttachment(
                builder -> {
                    builder.setBufferSize(3);
                    builder.setAttachEvents(true);
                    builder.setLayout(PatternLayout.createDefaultLayout());
                    builder.setAttachEventsCompression(SmtpManager.AttachEventsCompression.GZ); },
                log -> {
                    log.debug(attachmentUuid);
                    log.error(bodyUuid, new RuntimeException(exceptionUuid)); });

        assertTrue(messages.size() == 1);
        MimeMessageParser message = messages.get(0);
        assertNotNull(message.getPlainContent());
        assertNull(message.getHtmlContent());
        assertTrue(message.getPlainContent().contains(bodyUuid));
        assertTrue(message.getPlainContent().contains(exceptionUuid));
        assertFalse(message.getPlainContent().contains(attachmentUuid));

        assertTrue(message.getAttachmentList().size() == 1);
        DataSource attachment = message.getAttachmentList().get(0);
        assertTrue(attachment.getContentType().contains("application/gz"));
        assertTrue(attachment.getName().equals("logEvents.txt.gz"));

        String logEventsTxt = IOUtils.toString(new GZIPInputStream(attachment.getInputStream()), "UTF-8");
        assertTrue(logEventsTxt.contains(bodyUuid));
        assertTrue(logEventsTxt.contains(exceptionUuid));
        assertTrue(logEventsTxt.contains(attachmentUuid));
        assertFalse(logEventsTxt.toLowerCase().contains("doctype"));
    }

    /**
     *  Used by testAttachment* to consistently arrange a test SmtpAppender and Logger
     *  so the test case can tweak the SmtpAppender before its built and verify the
     *  generated email messages without sending them through a server.
     *
     * @param smtpBuilderConsumer Allow test cases to customize the SmtpAppender before it's created
     * @param loggerConsumer Writes messages to a new LoggerContext each time this function is called
     * @return List of parsed MimeMessages
     */
    private List<MimeMessageParser> testAttachment(Consumer<SmtpAppender.Builder> smtpBuilderConsumer, Consumer<Logger> loggerConsumer) {
        SmtpAppender.Builder smtpBuilder = SmtpAppender.newBuilder()
                .setName("testAttachment")
                .setTo("to@example.com")
                .setCc("cc@example.com")
                .setBcc("bcc@example.com")
                .setFrom("from@example.com")
                .setReplyTo("replyTo@example.com")
                .setSubject("testAttachment")
                .setSmtpHost(HOST) // ignored, no emails sent
                .setSmtpPort(AvailablePortFinder.getNextAvailable()); // ignored, no emails sent
        smtpBuilderConsumer.accept(smtpBuilder);
        SmtpAppender appender = smtpBuilder.build();
        appender.start();

        LoggerContext context = (LoggerContext) LogManager.getContext();
        context.reconfigure(); // reset for each test
        Logger logger = (Logger) context.getLogger(SmtpAppenderTest.class);
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);

        /*
            Mocking static methods requires org.mockito:mockito-inline instead of
            org.mockito:mockito-core. mockito-inline is a convenience artifact that
            tweaks mockito-core. Instead of changing pom.xml, mockito-inline
            functionality has been enabled using
            /resources/mockito-extensions/org.mockito.plugins.MockMaker
         */
        List<MimeMessageParser> messages = new ArrayList<>();
        try (MockedStatic<Transport> ignored = Mockito.mockStatic(Transport.class, invocation -> {
            MimeMessage message = invocation.getArgument(0);
            MimeMessageParser parser = new MimeMessageParser(message);
            messages.add(parser.parse());
            return null; // capture all Transport.send calls and do nothing
        })) {
            loggerConsumer.accept(logger);
        }
        return messages;
    }

    /**
     * Decompresses the InputStream and does a quick check to make sure there's only a single file within the zip
     */
    private String decompressZipAndAssert(InputStream in) throws IOException {
        String result = null;
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry != null) {
                result = IOUtils.toString(zis, "UTF-8");
            }
            if (zis.getNextEntry() != null) {
                fail("There should only ever be one file within the zip file");
            }
        }
        return result;
    }
}