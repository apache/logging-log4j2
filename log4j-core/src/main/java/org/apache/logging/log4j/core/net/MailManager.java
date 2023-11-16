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

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;

/**
 * Parent of all managers that send e-mails.
 *
 */
public abstract class MailManager extends AbstractManager {

    /**
     * Creates a unique-per-configuration name for an smtp manager using the
     * specified the parameters.<br>
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
            final String smtpProtocol,
            final String smtpHost,
            final int smtpPort,
            final String smtpUsername,
            final boolean smtpDebug,
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
        sb.append(smtpProtocol)
                .append(':')
                .append(smtpHost)
                .append(':')
                .append(smtpPort)
                .append(':');
        if (smtpUsername != null) {
            sb.append(smtpUsername);
        }
        sb.append(smtpDebug ? ":debug:" : "::");
        sb.append(filterName);

        return "SMTP:" + sb.toString();
    }

    public static class FactoryData {
        private final String to;
        private final String cc;
        private final String bcc;
        private final String from;
        private final String replyTo;
        private final String subject;
        private final Serializer subjectSerializer;
        private final String smtpProtocol;
        private final String smtpHost;
        private final int smtpPort;
        private final String smtpUsername;
        private final String smtpPassword;
        private final boolean smtpDebug;
        private final int bufferSize;
        private final SslConfiguration sslConfiguration;
        private final String filterName;
        private final String managerName;

        public FactoryData(
                final String to,
                final String cc,
                final String bcc,
                final String from,
                final String replyTo,
                final String subject,
                final Serializer subjectSerializer,
                final String smtpProtocol,
                final String smtpHost,
                final int smtpPort,
                final String smtpUsername,
                final String smtpPassword,
                final boolean smtpDebug,
                final int bufferSize,
                final SslConfiguration sslConfiguration,
                final String filterName) {
            this.to = to;
            this.cc = cc;
            this.bcc = bcc;
            this.from = from;
            this.replyTo = replyTo;
            this.subject = subject;
            this.subjectSerializer = subjectSerializer;
            this.smtpProtocol = smtpProtocol;
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.smtpUsername = smtpUsername;
            this.smtpPassword = smtpPassword;
            this.smtpDebug = smtpDebug;
            this.bufferSize = bufferSize;
            this.sslConfiguration = sslConfiguration;
            this.filterName = filterName;
            this.managerName = createManagerName(
                    to,
                    cc,
                    bcc,
                    from,
                    replyTo,
                    subject,
                    smtpProtocol,
                    smtpHost,
                    smtpPort,
                    smtpUsername,
                    smtpDebug,
                    filterName);
        }

        public String getTo() {
            return to;
        }

        public String getCc() {
            return cc;
        }

        public String getBcc() {
            return bcc;
        }

        public String getFrom() {
            return from;
        }

        public String getReplyTo() {
            return replyTo;
        }

        public String getSubject() {
            return subject;
        }

        public Serializer getSubjectSerializer() {
            return subjectSerializer;
        }

        public String getSmtpProtocol() {
            return smtpProtocol;
        }

        public String getSmtpHost() {
            return smtpHost;
        }

        public int getSmtpPort() {
            return smtpPort;
        }

        public String getSmtpUsername() {
            return smtpUsername;
        }

        public String getSmtpPassword() {
            return smtpPassword;
        }

        public boolean isSmtpDebug() {
            return smtpDebug;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public SslConfiguration getSslConfiguration() {
            return sslConfiguration;
        }

        public String getFilterName() {
            return filterName;
        }

        public String getManagerName() {
            return managerName;
        }
    }

    public MailManager(final LoggerContext loggerContext, final String name) {
        super(loggerContext, name);
    }

    /**
     * Adds an event to the cyclic buffer.
     *
     * @param event The event to add.
     */
    public abstract void add(LogEvent event);

    /**
     * Send the contents of the cyclic buffer as an e-mail message.
     *
     * @param layout      The layout for formatting the events.
     * @param appendEvent The event that triggered the send.
     */
    public abstract void sendEvents(final Layout<?> layout, final LogEvent appendEvent);
}
