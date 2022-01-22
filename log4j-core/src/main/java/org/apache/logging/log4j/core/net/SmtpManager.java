/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.net;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.CyclicBuffer;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Manager for sending SMTP events.
 */
public class SmtpManager extends AbstractManager {
    private static final SMTPManagerFactory FACTORY = new SMTPManagerFactory();

    private static final LogEvent[] EMPTY_ARRAY = {};

    /**
     * Used when AttachEvents is true
     */
    public enum AttachEventsCompression
    {
        NONE,
        ZIP,
        GZ;

        public static AttachEventsCompression lookup(final String value) {
            for (final AttachEventsCompression aec : values()) {
                if (aec.name().equalsIgnoreCase(value)) {
                    return aec;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private final Session session;

    private final CyclicBuffer<LogEvent> buffer;

    private volatile MimeMessage message;

    private final FactoryData data;

    private static MimeMessage createMimeMessage(final FactoryData data, final Session session, final LogEvent appendEvent)
            throws MessagingException {
        return new MimeMessageBuilder(session).setFrom(data.from).setReplyTo(data.replyto)
                .setRecipients(Message.RecipientType.TO, data.to).setRecipients(Message.RecipientType.CC, data.cc)
                .setRecipients(Message.RecipientType.BCC, data.bcc).setSubject(data.subject.toSerializable(appendEvent))
                .build();
    }

    protected SmtpManager(final String name, final Session session, final MimeMessage message,
                          final FactoryData data) {
        super(null, name);
        this.session = session;
        this.message = message;
        this.data = data;
        this.buffer = new CyclicBuffer<>(LogEvent.class, data.numElements);
    }

    public void add(LogEvent event) {
        buffer.add(event.toImmutable());
    }

    public static SmtpManager getSmtpManager(
            final Configuration config,
            final String to, final String cc, final String bcc,
            final String from, final String replyTo,
            final String subject, String protocol, final String host,
            final int port, final String username, final String password,
            final boolean isDebug, final String filterName, final int numElements,
            final SslConfiguration sslConfiguration,
            final boolean attachEvents, final AttachEventsCompression attachEventsCompression) {
        if (Strings.isEmpty(protocol)) {
            protocol = "smtp";
        }

        final String name = createManagerName(to, cc, bcc, from, replyTo, subject, protocol, host, port, username, isDebug, filterName);
        final Serializer subjectSerializer = PatternLayout.newSerializerBuilder().setConfiguration(config).setPattern(subject).build();

        return getManager(name, FACTORY, new FactoryData(to, cc, bcc, from, replyTo, subjectSerializer,
                protocol, host, port, username, password, isDebug, numElements, sslConfiguration, attachEvents, attachEventsCompression));
    }

    /**
     * Creates a unique-per-configuration name for an smtp manager using the specified the parameters.<br>
     * Using such a name allows us to maintain singletons per unique configurations.
     *
     * @return smtp manager name
     */
    static String createManagerName(
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
            final boolean isDebug,
            final String filterName) {

        final StringBuilder sb = new StringBuilder();

        if (to != null) {
            sb.append(to);
        }
        sb.append(':');
        if (cc != null) {
            sb.append(cc);
        }
        sb.append(':');
        if (bcc != null) {
            sb.append(bcc);
        }
        sb.append(':');
        if (from != null) {
            sb.append(from);
        }
        sb.append(':');
        if (replyTo != null) {
            sb.append(replyTo);
        }
        sb.append(':');
        if (subject != null) {
            sb.append(subject);
        }
        sb.append(':');
        sb.append(protocol).append(':').append(host).append(':').append(port).append(':');
        if (username != null) {
            sb.append(username);
        }
        sb.append(isDebug ? ":debug:" : "::");
        sb.append(filterName);

        return "SMTP:" + sb.toString();
    }

    /**
     * Send the contents of the cyclic buffer as an e-mail message.
     * @param layout The layout for formatting the events.
     * @param appendEvent The event that triggered the send.
     */
    public void sendEvents(final Layout<?> layout, final LogEvent appendEvent) {
        if (message == null) {
            connect(appendEvent);
        }
        try {
            final LogEvent[] priorEvents = removeAllBufferedEvents();
            // LOG4J-310: log appendEvent even if priorEvents is empty

            final byte[] rawBytes = formatContentToBytes(data.attachEvents ? EMPTY_ARRAY : priorEvents, appendEvent, layout);

            final String contentType = layout.getContentType();
            final String encoding = getEncoding(rawBytes, contentType);
            final byte[] encodedBytes = encodeContentToBytes(rawBytes, encoding);

            final InternetHeaders headers = getHeaders(contentType, encoding);
            final MimeMultipart mp = getMimeMultipart(encodedBytes, headers);

            if (data.attachEvents) {
                // implementation decision to include the current message in the attachment
                addAttachment(mp, priorEvents, appendEvent, layout);
            }

            final String subject = data.subject.toSerializable(appendEvent);

            sendMultipartMessage(message, mp, subject);
        } catch (final MessagingException | IOException | RuntimeException e) {
            logError("Caught exception while sending e-mail notification.", e);
            throw new LoggingException("Error occurred while sending email", e);
        }
    }

    LogEvent[] removeAllBufferedEvents() {
        return buffer.removeAll();
    }

    protected byte[] formatContentToBytes(final LogEvent[] priorEvents, final LogEvent appendEvent,
                                          final Layout<?> layout) throws IOException {
        final ByteArrayOutputStream raw = new ByteArrayOutputStream();
        writeContent(priorEvents, appendEvent, layout, raw);
        return raw.toByteArray();
    }

    private void writeContent(final LogEvent[] priorEvents, final LogEvent appendEvent, final Layout<?> layout,
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

    protected void writeBuffer(final LogEvent[] priorEvents, final LogEvent appendEvent, final Layout<?> layout,
                               final OutputStream out) throws IOException {
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

    private void addAttachment(final MimeMultipart mp, final LogEvent[] priorEvents, final LogEvent appendEvent,
                               final Layout<?> layout) throws IOException, MessagingException {
        final byte[] rawBytes = formatContentToBytes(priorEvents, appendEvent, layout);
        MimeBodyPart part = new MimeBodyPart();
        if (data.attachEventsCompression == AttachEventsCompression.NONE) {
            if (layout instanceof HtmlLayout) {
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(rawBytes, layout.getContentType())));
                part.setFileName("logEvents.html");
            } else {
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(rawBytes, "text/plain")));
                part.setFileName("logEvents.txt");
            }
        } else if (data.attachEventsCompression == AttachEventsCompression.ZIP) {
            if (layout instanceof HtmlLayout) {
                byte[] compressed = zipCompress(rawBytes, "logEvents.html");
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(compressed, "application/zip")));
                part.setFileName("logEvents.html.zip");
            } else {
                byte[] compressed = zipCompress(rawBytes, "logEvents.txt");
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(compressed, "application/zip")));
                part.setFileName("logEvents.txt.zip");
            }
        } else if (data.attachEventsCompression == AttachEventsCompression.GZ) {
            if (layout instanceof HtmlLayout) {
                byte[] compressed = gzipCompress(rawBytes);
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(compressed, "application/gzip")));
                part.setFileName("logEvents.html.gz");
            } else {
                byte[] compressed = gzipCompress(rawBytes);
                part.setDataHandler(new DataHandler(new ByteArrayDataSource(compressed, "application/gzip")));
                part.setFileName("logEvents.txt.gz");
            }
        }
        part.setDisposition(Part.ATTACHMENT);
        mp.addBodyPart(part);
    }

    private byte[] zipCompress(byte[] data, String fileName) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (final ZipOutputStream zos = new ZipOutputStream(baos)) {
                final ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                zos.write(data);
            }
            return baos.toByteArray();
        }
    }

    private byte[] gzipCompress(byte[] data) throws IOException {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (final GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data);
            }
            return baos.toByteArray();
        }
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

    protected void sendMultipartMessage(final MimeMessage msg, final MimeMultipart mp, final String subject) throws MessagingException {
        synchronized (msg) {
            msg.setContent(mp);
            msg.setSentDate(new Date());
            msg.setSubject(subject);
            Transport.send(msg);
        }
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        private final String to;
        private final String cc;
        private final String bcc;
        private final String from;
        private final String replyto;
        private final Serializer subject;
        private final String protocol;
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final boolean isDebug;
        private final int numElements;
        private final SslConfiguration sslConfiguration;
        private final boolean attachEvents;
        private final AttachEventsCompression attachEventsCompression;

        public FactoryData(final String to, final String cc, final String bcc, final String from, final String replyTo,
                           final Serializer subjectSerializer, final String protocol, final String host, final int port,
                           final String username, final String password, final boolean isDebug, final int numElements,
                           final SslConfiguration sslConfiguration, final boolean attachEvents, AttachEventsCompression attachEventsCompression) {
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
            this.from = from;
            this.replyto = replyTo;
            this.subject = subjectSerializer;
            this.protocol = protocol;
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.isDebug = isDebug;
            this.numElements = numElements;
            this.sslConfiguration = sslConfiguration;
            this.attachEvents = attachEvents;
            this.attachEventsCompression = attachEventsCompression;
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
    private static class SMTPManagerFactory implements ManagerFactory<SmtpManager, FactoryData> {

        @Override
        public SmtpManager createManager(final String name, final FactoryData data) {
            final String prefix = "mail." + data.protocol;

            final Properties properties = PropertiesUtil.getSystemProperties();
            properties.setProperty("mail.transport.protocol", data.protocol);
            if (properties.getProperty("mail.host") == null) {
                // Prevent an UnknownHostException in Java 7
                properties.setProperty("mail.host", NetUtils.getLocalHostname());
            }

            if (null != data.host) {
                properties.setProperty(prefix + ".host", data.host);
            }
            if (data.port > 0) {
                properties.setProperty(prefix + ".port", String.valueOf(data.port));
            }

            final Authenticator authenticator = buildAuthenticator(data.username, data.password);
            if (null != authenticator) {
                properties.setProperty(prefix + ".auth", "true");
            }

            if (data.protocol.equals("smtps")) {
                final SslConfiguration sslConfiguration = data.sslConfiguration;
                if (sslConfiguration != null) {
                    final SSLSocketFactory sslSocketFactory = sslConfiguration.getSslSocketFactory();
                    properties.put(prefix + ".ssl.socketFactory", sslSocketFactory);
                    properties.setProperty(prefix + ".ssl.checkserveridentity", Boolean.toString(sslConfiguration.isVerifyHostName()));
                }
            }

            final Session session = Session.getInstance(properties, authenticator);
            session.setProtocolForAddress("rfc822", data.protocol);
            session.setDebug(data.isDebug);
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