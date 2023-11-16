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

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.MailManager;
import org.apache.logging.log4j.core.net.MailManager.FactoryData;
import org.apache.logging.log4j.core.net.MailManagerFactory;
import org.apache.logging.log4j.core.net.SmtpManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.apache.logging.log4j.util.Strings;

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
@Plugin(name = "SMTP", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class SmtpAppender extends AbstractAppender {

    private static final int DEFAULT_BUFFER_SIZE = 512;

    /** The SMTP Manager */
    private final MailManager manager;

    private SmtpAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final MailManager manager,
            final boolean ignoreExceptions,
            final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = manager;
    }

    public MailManager getManager() {
        return manager;
    }

    /**
     * @since 2.13.2
     */
    public static class Builder extends AbstractAppender.Builder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<SmtpAppender> {
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
        @ValidPort
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

        /**
         * Specifies the layout used for the email message body. By default, this uses the
         * {@linkplain HtmlLayout#createDefaultLayout() default HTML layout}.
         */
        @Override
        public Builder setLayout(final Layout<? extends Serializable> layout) {
            return super.setLayout(layout);
        }

        /**
         * Specifies the filter used for this appender. By default, uses a {@link ThresholdFilter} with a level of
         * ERROR.
         */
        @Override
        public Builder setFilter(final Filter filter) {
            return super.setFilter(filter);
        }

        @Override
        public SmtpAppender build() {
            if (getLayout() == null) {
                setLayout(HtmlLayout.createDefaultLayout());
            }
            if (getFilter() == null) {
                setFilter(ThresholdFilter.createFilter(null, null, null));
            }
            if (Strings.isEmpty(smtpProtocol)) {
                smtpProtocol = "smtp";
            }
            final Serializer subjectSerializer = PatternLayout.newSerializerBuilder()
                    .setConfiguration(getConfiguration())
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
                    smtpProtocol,
                    smtpHost,
                    smtpPort,
                    smtpUsername,
                    smtpPassword,
                    smtpDebug,
                    bufferSize,
                    sslConfiguration,
                    getFilter().toString());
            final MailManagerFactory factory = ServiceLoaderUtil.loadServices(
                            MailManagerFactory.class, MethodHandles.lookup())
                    .findAny()
                    .orElseGet(() -> SmtpManager.FACTORY);
            final MailManager smtpManager = AbstractManager.getManager(data.getManagerName(), factory, data);
            if (smtpManager == null) {
                LOGGER.error("Unabled to instantiate SmtpAppender named {}", getName());
                return null;
            }

            return new SmtpAppender(
                    getName(), getFilter(), getLayout(), smtpManager, isIgnoreExceptions(), getPropertyArray());
        }
    }

    /**
     * @since 2.13.2
     */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Create a SmtpAppender.
     * @deprecated Use {@link #newBuilder()} to create and configure a {@link Builder} instance.
     * @see Builder
     */
    @Deprecated
    public static SmtpAppender createAppender(
            @PluginConfiguration final Configuration config,
            @PluginAttribute("name") @Required final String name,
            @PluginAttribute("to") final String to,
            @PluginAttribute("cc") final String cc,
            @PluginAttribute("bcc") final String bcc,
            @PluginAttribute("from") final String from,
            @PluginAttribute("replyTo") final String replyTo,
            @PluginAttribute("subject") final String subject,
            @PluginAttribute("smtpProtocol") final String smtpProtocol,
            @PluginAttribute("smtpHost") final String smtpHost,
            @PluginAttribute(value = "smtpPort", defaultString = "0") @ValidPort final String smtpPortStr,
            @PluginAttribute("smtpUsername") final String smtpUsername,
            @PluginAttribute(value = "smtpPassword", sensitive = true) final String smtpPassword,
            @PluginAttribute("smtpDebug") final String smtpDebug,
            @PluginAttribute("bufferSize") final String bufferSizeStr,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") Filter filter,
            @PluginAttribute("ignoreExceptions") final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for SmtpAppender");
            return null;
        }
        return SmtpAppender.newBuilder()
                .setIgnoreExceptions(Booleans.parseBoolean(ignore, true))
                .setSmtpPort(AbstractAppender.parseInt(smtpPortStr, 0))
                .setSmtpDebug(Boolean.parseBoolean(smtpDebug))
                .setBufferSize(bufferSizeStr == null ? DEFAULT_BUFFER_SIZE : Integers.parseInt(bufferSizeStr))
                .setLayout(layout)
                .setFilter(filter)
                .setConfiguration(config != null ? config : new DefaultConfiguration())
                .build();
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
