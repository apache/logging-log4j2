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

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HTMLLayout;
import org.apache.logging.log4j.core.net.SMTPManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Send an e-mail when a specific logging event occurs, typically on errors or
 * fatal errors.
 *
 * <p>
 * The number of logging events delivered in this e-mail depend on the value of
 * <b>BufferSize</b> option. The <code>SMTPAppender</code> keeps only the last
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
@Plugin(name = "SMTP", type = "Core", elementType = "appender", printObject = true)
public final class SMTPAppender extends AbstractAppender {

    private static final int DEFAULT_BUFFER_SIZE = 512;

    /** The SMTP Manager */
    protected final SMTPManager manager;

    private SMTPAppender(final String name, final Filter filter, final Layout<?> layout, final SMTPManager manager,
                         final boolean handleExceptions) {
        super(name, filter, layout, handleExceptions);
        this.manager = manager;
    }

    /**
     * Create a SMTPAppender.
     *
     * @param name
     *            The name of the Appender.
     * @param to
     *            The comma-separated list of recipient email addresses.
     * @param cc
     *            The comma-separated list of CC email addresses.
     * @param bcc
     *            The comma-separated list of BCC email addresses.
     * @param from
     *            The email address of the sender.
     * @param replyTo
     *            The comma-separated list of reply-to email addresses.
     * @param subject The subject of the email message.
     * @param smtpProtocol The SMTP transport protocol (such as "smtps", defaults to "smtp").
     * @param smtpHost
     *            The SMTP hostname to send to.
     * @param smtpPortNum
     *            The SMTP port to send to.
     * @param smtpUsername
     *            The username required to authenticate against the SMTP server.
     * @param smtpPassword
     *            The password required to authenticate against the SMTP server.
     * @param smtpDebug
     *            Enable mail session debuging on STDOUT.
     * @param bufferSizeNum
     *            How many log events should be buffered for inclusion in the
     *            message?
     * @param layout
     *            The layout to use (defaults to HTMLLayout).
     * @param filter
     *            The Filter or null (defaults to ThresholdFilter, level of
     *            ERROR).
     * @param suppressExceptions
     *            "true" if exceptions should be hidden from the application,
     *            "false" otherwise (defaults to "true").
     * @return The SMTPAppender.
     */
    @PluginFactory
    public static SMTPAppender createAppender(@PluginAttr("name") final String name,
                                              @PluginAttr("to") final String to,
                                              @PluginAttr("cc") final String cc,
                                              @PluginAttr("bcc") final String bcc,
                                              @PluginAttr("from") final String from,
                                              @PluginAttr("replyTo") final String replyTo,
                                              @PluginAttr("subject") final String subject,
                                              @PluginAttr("smtpProtocol") final String smtpProtocol,
                                              @PluginAttr("smtpHost") final String smtpHost,
                                              @PluginAttr("smtpPort") final String smtpPortNum,
                                              @PluginAttr("smtpUsername") final String smtpUsername,
                                              @PluginAttr("smtpPassword") final String smtpPassword,
                                              @PluginAttr("smtpDebug") final String smtpDebug,
                                              @PluginAttr("bufferSize") final String bufferSizeNum,
                                              @PluginElement("layout") Layout<?> layout,
                                              @PluginElement("filter") Filter filter,
                                              @PluginAttr("suppressExceptions") final String suppressExceptions) {
        if (name == null) {
            LOGGER.error("No name provided for SMTPAppender");
            return null;
        }

        final boolean isHandleExceptions = suppressExceptions == null ? true : Boolean.valueOf(suppressExceptions);
        final int smtpPort = smtpPortNum == null ? 0 : Integer.parseInt(smtpPortNum);
        final boolean isSmtpDebug = smtpDebug == null ? false : Boolean.valueOf(smtpDebug);
        final int bufferSize = bufferSizeNum == null ? DEFAULT_BUFFER_SIZE : Integer.valueOf(bufferSizeNum);

        if (layout == null) {
            layout = HTMLLayout.createLayout(null, null, null, null, null, null);
        }
        if (filter == null) {
            filter = ThresholdFilter.createFilter(null, null, null);
        }

        final SMTPManager manager = SMTPManager.getSMTPManager(to, cc, bcc, from, replyTo, subject, smtpProtocol,
            smtpHost, smtpPort, smtpUsername, smtpPassword, isSmtpDebug, filter.toString(),  bufferSize);
        if (manager == null) {
            return null;
        }

        return new SMTPAppender(name, filter, layout, manager, isHandleExceptions);
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
     * Perform SMTPAppender specific appending actions, mainly adding the event
     * to a cyclic buffer and checking if the event triggers an e-mail to be
     * sent.
     * @param event The Log event.
     */
    public void append(final LogEvent event) {
        manager.sendEvents(getLayout(), event);
    }
}
