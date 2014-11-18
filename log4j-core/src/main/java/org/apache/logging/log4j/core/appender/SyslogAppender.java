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

import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.LoggerFields;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * The Syslog Appender.
 */
@Plugin(name = "Syslog", category = "Core", elementType = "appender", printObject = true)
public class SyslogAppender extends SocketAppender {

    private static final long serialVersionUID = 1L;
    protected static final String RFC5424 = "RFC5424";

    protected SyslogAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                             final boolean ignoreExceptions, final boolean immediateFlush,
                             final AbstractSocketManager manager, final Advertiser advertiser) {
        super(name, layout, filter, manager, ignoreExceptions, immediateFlush, advertiser);

    }

    /**
     * Create a SyslogAppender.
     * @param host The name of the host to connect to.
     * @param port The port to connect to on the target host.
     * @param protocolStr The Protocol to use.
     * @param sslConfig TODO
     * @param connectTimeoutMillis the connect timeout in milliseconds.
     * @param reconnectionDelayMillis The interval in which failed writes should be retried.
     * @param immediateFail True if the write should fail if no socket is immediately available.
     * @param name The name of the Appender.
     * @param immediateFlush "true" if data should be flushed on each write.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged;
     *                         otherwise they are propagated to the caller.
     * @param facility The Facility is used to try to classify the message.
     * @param id The default structured data id to use when formatting according to RFC 5424.
     * @param enterpriseNumber The IANA enterprise number.
     * @param includeMdc Indicates whether data from the ThreadContextMap will be included in the RFC 5424 Syslog
     * record. Defaults to "true:.
     * @param mdcId The id to use for the MDC Structured Data Element.
     * @param mdcPrefix The prefix to add to MDC key names.
     * @param eventPrefix The prefix to add to event key names.
     * @param newLine If true, a newline will be appended to the end of the syslog record. The default is false.
     * @param escapeNL String that should be used to replace newlines within the message text.
     * @param appName The value to use as the APP-NAME in the RFC 5424 syslog record.
     * @param msgId The default value to be used in the MSGID field of RFC 5424 syslog records.
     * @param excludes A comma separated list of mdc keys that should be excluded from the LogEvent.
     * @param includes A comma separated list of mdc keys that should be included in the FlumeEvent.
     * @param required A comma separated list of mdc keys that must be present in the MDC.
     * @param format If set to "RFC5424" the data will be formatted in accordance with RFC 5424. Otherwise,
     * it will be formatted as a BSD Syslog record.
     * @param filter A Filter to determine if the event should be handled by this Appender.
     * @param config The Configuration.
     * @param charsetName The character set to use when converting the syslog String to a byte array.
     * @param exceptionPattern The converter pattern to use for formatting exceptions.
     * @param loggerFields The logger fields
     * @param advertise Whether to advertise
     * @param connectTimeoutMillis the connect timeout in milliseconds.
     * @return A SyslogAppender.
     */
    @PluginFactory
    public static SyslogAppender createAppender(
            // @formatter:off
            @PluginAttribute("host") final String host,
            @PluginAttribute(value = "port", defaultInt = 0) final int port,
            @PluginAttribute("protocol") final String protocolStr,
            @PluginElement("SSL") final SslConfiguration sslConfig,
            @PluginAttribute(value = "connectTimeoutMillis", defaultInt = 0) final int connectTimeoutMillis,
            @PluginAliases("reconnectionDelay") // deprecated
            @PluginAttribute(value = "reconnectionDelayMillis", defaultInt = 0) final int reconnectionDelayMillis,
            @PluginAttribute(value = "immediateFail", defaultBoolean = true) final boolean immediateFail,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "immediateFlush", defaultBoolean = true) final boolean immediateFlush,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions,
            @PluginAttribute(value = "facility", defaultString = "LOCAL0") final Facility facility,
            @PluginAttribute("id") final String id,
            @PluginAttribute(value = "enterpriseNumber", defaultInt = Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER) final int enterpriseNumber,
            @PluginAttribute(value = "includeMdc", defaultBoolean = true) final boolean includeMdc,
            @PluginAttribute("mdcId") final String mdcId,
            @PluginAttribute("mdcPrefix") final String mdcPrefix,
            @PluginAttribute("eventPrefix") final String eventPrefix,
            @PluginAttribute(value = "newLine", defaultBoolean = false) final boolean newLine,
            @PluginAttribute("newLineEscape") final String escapeNL,
            @PluginAttribute("appName") final String appName,
            @PluginAttribute("messageId") final String msgId,
            @PluginAttribute("mdcExcludes") final String excludes,
            @PluginAttribute("mdcIncludes") final String includes,
            @PluginAttribute("mdcRequired") final String required,
            @PluginAttribute("format") final String format,
            @PluginElement("Filter") final Filter filter,
            @PluginConfiguration final Configuration config,
            @PluginAttribute(value = "charset", defaultString = "UTF-8") final Charset charsetName,
            @PluginAttribute("exceptionPattern") final String exceptionPattern,
            @PluginElement("LoggerFields") final LoggerFields[] loggerFields, @PluginAttribute(value = "advertise", defaultBoolean = false) final boolean advertise) {
        // @formatter:on

        // TODO: add Protocol to TypeConverters
        final Protocol protocol = EnglishEnums.valueOf(Protocol.class, protocolStr);
        final boolean useTlsMessageFormat = sslConfig != null || protocol == Protocol.SSL;
        final Layout<? extends Serializable> layout = RFC5424.equalsIgnoreCase(format) ?
            Rfc5424Layout.createLayout(facility, id, enterpriseNumber, includeMdc, mdcId, mdcPrefix, eventPrefix, newLine,
                escapeNL, appName, msgId, excludes, includes, required, exceptionPattern, useTlsMessageFormat, loggerFields,
                config) :
            SyslogLayout.createLayout(facility, newLine, escapeNL, charsetName);

        if (name == null) {
            LOGGER.error("No name provided for SyslogAppender");
            return null;
        }
        final AbstractSocketManager manager = createSocketManager(name, protocol, host, port, connectTimeoutMillis,
                sslConfig, reconnectionDelayMillis, immediateFail, layout);

        return new SyslogAppender(name, layout, filter, ignoreExceptions, immediateFlush, manager,
                advertise ? config.getAdvertiser() : null);
    }
}
