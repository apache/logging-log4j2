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

package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.net.SmtpManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Booleans;

import java.io.Serializable;

/**
 * Send an e-mail when a specific logging event occurs, typically on errors or
 * fatal errors.
 *
 * <p>
 * The number of logging events delivered in this e-mail depend on the value of
 * <b>BufferSize</b> option. The <code>SmtpAppender</code> keeps only the last
 * <code>BufferSize</code> logging events in its cyclic buffer. This keeps
 * memory requirements at a reasonable level while still delivering useful
 * application context.
 *
 * By default, an email message will formatted as HTML. This can be modified by
 * setting a layout for the appender.
 *
 * By default, an email message will be sent when an ERROR or higher severity
 * message is appended. This can be modified by setting a filter for the
 * appender.
 */
@Plugin(name = "SMTP", category = "Core", elementType = "appender", printObject = true)
public final class SmtpAppender extends AbstractAppender {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_BUFFER_SIZE = 512;

    /** The SMTP Manager */
    private final SmtpManager manager;

    private SmtpAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final SmtpManager manager,
                         final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<SmtpAppender> {
        @PluginBuilderAttribute
        @Required(message = "No name provided for SmtpAppender")
        private String name;

        @PluginElement("Filter")
        private Filter filter = ThresholdFilter.createFilter(null, null, null);

        @PluginElement("Layout")
        private Layout<? extends Serializable> layout = HtmlLayout.createDefaultLayout();

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        @PluginBuilderAttribute
        private String to;

        @PluginBuilderAttribute
        private String cc;

        @PluginBuilderAttribute
        private String bcc;

        @PluginBuilderAttribute
        private String from;

        @PluginBuilderAttribute
        private String replyTo;

        @PluginBuilderAttribute
        private String subject;

        @PluginBuilderAttribute
        private String smtpProtocol = "smtp";

        @PluginBuilderAttribute
        private String smtpHost;

        @PluginBuilderAttribute
        private int smtpPort;

        @PluginBuilderAttribute
        private String smtpUsername;

        @PluginBuilderAttribute(sensitive = true)
        private String smtpPassword;

        @PluginBuilderAttribute
        private boolean smtpDebug;

        @PluginBuilderAttribute
        private int bufferSize = DEFAULT_BUFFER_SIZE;

        @PluginElement("SSL")
        private SslConfiguration sslConfiguration;

        /**
         * Name of the SmtpAppender.
         */
        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Overrides the default filter. Default filter is a {@link ThresholdFilter} of level {@link Level#ERROR}.
         */
        public Builder setFilter(final Filter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Overrides the default layout. Default layout is the {@linkplain HtmlLayout#createDefaultLayout() default HTML layout}.
         */
        public Builder setLayout(final Layout<? extends Serializable> layout) {
            this.layout = layout;
            return this;
        }

        /**
         * Whether to ignore exceptions thrown by the SmtpAppender. Defaults to {@code true}.
         */
        public Builder setIgnoreExceptions(final boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        /**
         * Comma-separated list of recipient email addresses.
         */
        public Builder setTo(final String to) {
            this.to = to;
            return this;
        }

        /**
         * Comma-separated list of CC email addresses.
         */
        public Builder setCc(final String cc) {
            this.cc = cc;
            return this;
        }

        /**
         * Comma-separated list of BCC email addresses.
         */
        public Builder setBcc(final String bcc) {
            this.bcc = bcc;
            return this;
        }

        /**
         * Email address of the sender.
         */
        public Builder setFrom(final String from) {
            this.from = from;
            return this;
        }

        /**
         * Comma-separated list of Reply-To email addresses.
         */
        public Builder setReplyTo(final String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        /**
         * Subject template for the email messages.
         * @see org.apache.logging.log4j.core.layout.PatternLayout
         */
        public Builder setSubject(final String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * Transport protocol to use for SMTP such as "smtp" or "smtps". Defaults to "smtp".
         */
        public Builder setSmtpProtocol(final String smtpProtocol) {
            this.smtpProtocol = smtpProtocol;
            return this;
        }

        /**
         * Host name of SMTP server to send messages through.
         */
        public Builder setSmtpHost(final String smtpHost) {
            this.smtpHost = smtpHost;
            return this;
        }

        /**
         * Port number of SMTP server to send messages through.
         */
        public Builder setSmtpPort(final int smtpPort) {
            this.smtpPort = smtpPort;
            return this;
        }

        /**
         * Username to authenticate with SMTP server.
         */
        public Builder setSmtpUsername(final String smtpUsername) {
            this.smtpUsername = smtpUsername;
            return this;
        }

        /**
         * Password to authenticate with SMTP server.
         */
        public Builder setSmtpPassword(final String smtpPassword) {
            this.smtpPassword = smtpPassword;
            return this;
        }

        /**
         * Enables or disables mail session debugging on STDOUT. Disabled by default.
         */
        public Builder setSmtpDebug(final boolean smtpDebug) {
            this.smtpDebug = smtpDebug;
            return this;
        }

        /**
         * Number of log events to buffer before sending an email. Defaults to {@value #DEFAULT_BUFFER_SIZE}.
         */
        public Builder setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Specifies an SSL configuration for smtps connections.
         */
        public Builder setSslConfiguration(final SslConfiguration sslConfiguration) {
            this.sslConfiguration = sslConfiguration;
            return this;
        }

        @Override
        public SmtpAppender build() {
            if (layout == null) {
                layout = HtmlLayout.createDefaultLayout();
            }
            if (filter == null) {
                filter = ThresholdFilter.createFilter(null, null, null);
            }
            final SmtpManager smtpManager =
                    SmtpManager.getSMTPManager(to, cc, bcc, from, replyTo, subject, smtpProtocol, smtpHost, smtpPort,
                            smtpUsername, smtpPassword, smtpDebug, filter.toString(), bufferSize, sslConfiguration);
            return new SmtpAppender(name, filter, layout, smtpManager, ignoreExceptions);
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Deprecated
    public static SmtpAppender createAppender(final String name, final String to, final String cc, final String bcc,
            final String from, final String replyTo, final String subject, final String smtpProtocol, final String smtpHost,
            final String smtpPortStr, final String smtpUsername, final String smtpPassword, final String smtpDebug,
            final String bufferSizeStr, Layout<? extends Serializable> layout, Filter filter, final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for SmtpAppender");
            return null;
        }

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final int smtpPort = AbstractAppender.parseInt(smtpPortStr, 0);
        final boolean isSmtpDebug = Boolean.parseBoolean(smtpDebug);
        final int bufferSize = bufferSizeStr == null ? DEFAULT_BUFFER_SIZE : Integer.parseInt(bufferSizeStr);

        if (layout == null) {
            layout = HtmlLayout.createDefaultLayout();
        }
        if (filter == null) {
            filter = ThresholdFilter.createFilter(null, null, null);
        }

        final SmtpManager manager = SmtpManager.getSMTPManager(to, cc, bcc, from, replyTo, subject, smtpProtocol,
            smtpHost, smtpPort, smtpUsername, smtpPassword, isSmtpDebug, filter.toString(),  bufferSize);
        if (manager == null) {
            return null;
        }

        return new SmtpAppender(name, filter, layout, manager, ignoreExceptions);
    }

    /**
     * Capture all events in CyclicBuffer.
     * @param event The Log event.
     * @return true if the event should be filtered.
     */
    @Override
    public boolean isFiltered(final LogEvent event) {
        final boolean filtered = super.isFiltered(event);
        if (filtered) {
            manager.add(event);
        }
        return filtered;
    }

    /**
     * Perform SmtpAppender specific appending actions, mainly adding the event
     * to a cyclic buffer and checking if the event triggers an e-mail to be
     * sent.
     * @param event The Log event.
     */
    @Override
    public void append(final LogEvent event) {
        manager.sendEvents(getLayout(), event);
    }
}
