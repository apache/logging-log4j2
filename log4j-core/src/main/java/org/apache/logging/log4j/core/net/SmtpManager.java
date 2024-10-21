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
package org.apache.logging.log4j.core.net;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLSocketFactory;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.CyclicBuffer;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Manager for sending SMTP events.
 */
public class SmtpManager extends MailManager {
    public static final SMTPManagerFactory FACTORY = new SMTPManagerFactory();

    private final Session session;

    private final CyclicBuffer<LogEvent> buffer;

    private volatile MimeMessage message;

    private final FactoryData data;

    private static MimeMessage createMimeMessage(
            final FactoryData data, final Session session, final LogEvent appendEvent) throws MessagingException {
        return new MimeMessageBuilder(session)
                .setFrom(data.getFrom())
                .setReplyTo(data.getReplyTo())
                .setRecipients(Message.RecipientType.TO, data.getTo())
                .setRecipients(Message.RecipientType.CC, data.getCc())
                .setRecipients(Message.RecipientType.BCC, data.getBcc())
                .setSubject(data.getSubjectSerializer().toSerializable(appendEvent))
                .build();
    }

    protected SmtpManager(final String name, final Session session, final MimeMessage message, final FactoryData data) {
        super(null, name);
        this.session = session;
        this.message = message;
        this.data = data;
        this.buffer = new CyclicBuffer<>(LogEvent.class, data.getBufferSize());
    }

    @Override
    public void add(final LogEvent event) {
        buffer.add(event.toImmutable());
    }

    @Deprecated
    public static SmtpManager getSmtpManager(
            final Configuration config,
            final String to,
            final String cc,
            final String bcc,
            final String from,
            final String replyTo,
            final String subject,
            final String protocol,
            final String host,
            final int port,
            final String username,
            final String password,
            final boolean isDebug,
            final String filterName,
            final int numElements,
            final SslConfiguration sslConfiguration) {
        final Serializer subjectSerializer = PatternLayout.newSerializerBuilder()
                .setConfiguration(config)
                .setPattern(subject)
                .build();
        final FactoryData data = new FactoryData(
                to,
                cc,
                bcc,
                from,
                replyTo,
                subject,
                subjectSerializer,
                protocol,
                host,
                port,
                username,
                password,
                isDebug,
                numElements,
                sslConfiguration,
                filterName);

        return (SmtpManager) getManager(data.getManagerName(), FACTORY, data);
    }

    @Override
    public void sendEvents(final Layout<?> layout, final LogEvent appendEvent) {
        if (message == null) {
            connect(appendEvent);
        }
        try {
            final LogEvent[] priorEvents = removeAllBufferedEvents();
            // LOG4J-310: log appendEvent even if priorEvents is empty

            final byte[] rawBytes = formatContentToBytes(priorEvents, appendEvent, layout);

            final String contentType = layout.getContentType();
            final String encoding = getEncoding(rawBytes, contentType);
            final byte[] encodedBytes = encodeContentToBytes(rawBytes, encoding);

            final InternetHeaders headers = getHeaders(contentType, encoding);
            final MimeMultipart mp = getMimeMultipart(encodedBytes, headers);

            final String subject = data.getSubjectSerializer().toSerializable(appendEvent);

            sendMultipartMessage(message, mp, subject);
        } catch (final MessagingException | IOException | RuntimeException e) {
            logError("Caught exception while sending e-mail notification.", e);
            throw new LoggingException("Error occurred while sending email", e);
        }
    }

    LogEvent[] removeAllBufferedEvents() {
        return buffer.removeAll();
    }

    protected byte[] formatContentToBytes(
            final LogEvent[] priorEvents, final LogEvent appendEvent, final Layout<?> layout) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        writeContent(priorEvents, appendEvent, layout, raw);
        return raw.toByteArray();
    }

    private void writeContent(
            final LogEvent[] priorEvents,
            final LogEvent appendEvent,
            final Layout<?> layout,
            final ByteArrayOutputStream out)
            throws IOException {
        writeHeader(layout, out);
        writeBuffer(priorEvents, appendEvent, layout, out);
        writeFooter(layout, out);
    }

    protected void writeHeader(final Layout<?> layout, final OutputStream out) throws IOException {
        final byte[] header = layout.getHeader();
        if (header != null) {
            out.write(header);
        }
    }

    protected void writeBuffer(
            final LogEvent[] priorEvents, final LogEvent appendEvent, final Layout<?> layout, final OutputStream out)
            throws IOException {
        for (final LogEvent priorEvent : priorEvents) {
            final byte[] bytes = layout.toByteArray(priorEvent);
            out.write(bytes);
        }

        final byte[] bytes = layout.toByteArray(appendEvent);
        out.write(bytes);
    }

    protected void writeFooter(final Layout<?> layout, final OutputStream out) throws IOException {
        final byte[] footer = layout.getFooter();
        if (footer != null) {
            out.write(footer);
        }
    }

    protected String getEncoding(final byte[] rawBytes, final String contentType) {
        final DataSource dataSource = new ByteArrayDataSource(rawBytes, contentType);
        return MimeUtility.getEncoding(dataSource);
    }

    protected byte[] encodeContentToBytes(final byte[] rawBytes, final String encoding)
            throws MessagingException, IOException {
        final ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encodeContent(rawBytes, encoding, encoded);
        return encoded.toByteArray();
    }

    protected void encodeContent(final byte[] bytes, final String encoding, final ByteArrayOutputStream out)
            throws MessagingException, IOException {
        try (final OutputStream encoder = MimeUtility.encode(out, encoding)) {
            encoder.write(bytes);
        }
    }

    protected InternetHeaders getHeaders(final String contentType, final String encoding) {
        final InternetHeaders headers = new InternetHeaders();
        headers.setHeader("Content-Type", contentType + "; charset=UTF-8");
        headers.setHeader("Content-Transfer-Encoding", encoding);
        return headers;
    }

    protected MimeMultipart getMimeMultipart(final byte[] encodedBytes, final InternetHeaders headers)
            throws MessagingException {
        final MimeMultipart mp = new MimeMultipart();
        final MimeBodyPart part = new MimeBodyPart(headers, encodedBytes);
        mp.addBodyPart(part);
        return mp;
    }

    /**
     * @deprecated Please use the {@link #sendMultipartMessage(MimeMessage, MimeMultipart, String)} method instead.
     */
    @Deprecated
    protected void sendMultipartMessage(final MimeMessage msg, final MimeMultipart mp) throws MessagingException {
        synchronized (msg) {
            msg.setContent(mp);
            msg.setSentDate(new Date());
            Transport.send(msg);
        }
    }

    @SuppressFBWarnings(
            value = "SMTP_HEADER_INJECTION",
            justification = "False positive, since MimeMessage#setSubject does actually escape new lines.")
    protected void sendMultipartMessage(final MimeMessage msg, final MimeMultipart mp, final String subject)
            throws MessagingException {
        synchronized (msg) {
            msg.setContent(mp);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            Transport.send(msg);
        }
    }

    private synchronized void connect(final LogEvent appendEvent) {
        if (message != null) {
            return;
        }
        try {
            message = createMimeMessage(data, session, appendEvent);
        } catch (final MessagingException e) {
            logError("Could not set SmtpAppender message options", e);
            message = null;
        }
    }

    /**
     * Factory to create the SMTP Manager.
     */
    public static class SMTPManagerFactory implements MailManagerFactory {

        @Override
        public SmtpManager createManager(final String name, final FactoryData data) {
            final String smtpProtocol = data.getSmtpProtocol();
            final String prefix = "mail." + smtpProtocol;

            final Properties properties = PropertiesUtil.getSystemProperties();
            properties.setProperty("mail.transport.protocol", smtpProtocol);
            if (properties.getProperty("mail.host") == null) {
                // Prevent an UnknownHostException in Java 7
                properties.setProperty("mail.host", NetUtils.getLocalHostname());
            }

            final String smtpHost = data.getSmtpHost();
            if (null != smtpHost) {
                properties.setProperty(prefix + ".host", smtpHost);
            }
            if (data.getSmtpPort() > 0) {
                properties.setProperty(prefix + ".port", String.valueOf(data.getSmtpPort()));
            }

            final Authenticator authenticator = buildAuthenticator(data.getSmtpUsername(), data.getSmtpPassword());
            if (null != authenticator) {
                properties.setProperty(prefix + ".auth", "true");
            }

            if (smtpProtocol.equals("smtps")) {
                final SslConfiguration sslConfiguration = data.getSslConfiguration();
                if (sslConfiguration != null) {
                    final SSLSocketFactory sslSocketFactory = sslConfiguration.getSslSocketFactory();
                    properties.put(prefix + ".ssl.socketFactory", sslSocketFactory);
                    properties.setProperty(
                            prefix + ".ssl.checkserveridentity", Boolean.toString(sslConfiguration.isVerifyHostName()));
                }
            }

            final Session session = Session.getInstance(properties, authenticator);
            session.setProtocolForAddress("rfc822", smtpProtocol);
            session.setDebug(data.isSmtpDebug());
            return new SmtpManager(name, session, null, data);
        }

        private Authenticator buildAuthenticator(final String username, final String password) {
            if (null != password && null != username) {
                return new Authenticator() {
                    private final PasswordAuthentication passwordAuthentication =
                            new PasswordAuthentication(username, password);

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return passwordAuthentication;
                    }
                };
            }
            return null;
        }
    }
}
