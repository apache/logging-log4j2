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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.helpers.Booleans;
import org.apache.logging.log4j.core.layout.LoggerFields;
import org.apache.logging.log4j.core.layout.RFC5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.TLSSocketManager;
import org.apache.logging.log4j.core.net.ssl.SSLConfiguration;

import java.io.Serializable;

/**
 *
 * Secure Syslog Appender.
 */
@Plugin(name = "TLSSyslog", category = "Core", elementType = "appender", printObject = true)
public final class TLSSyslogAppender extends SyslogAppender {


    protected TLSSyslogAppender(String name, Layout<? extends Serializable> layout, Filter filter,
                                boolean ignoreExceptions, boolean immediateFlush, AbstractSocketManager manager,
                                Advertiser advertiser) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, manager, advertiser);
    }

    /**
     * Create a SyslogAppender.
     * @param host The name of the host to connect to.
     * @param portNum The port to connect to on the target host.
     * @param sslConfig   The SSL configuration
     * @param delay The interval in which failed writes should be retried.
     * @param immediateFail True if the write should fail if no socket is immediately available.
     * @param name The name of the Appender.
     * @param immediateFlush "true" if data should be flushed on each write.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param facility The Facility is used to try to classify the message.
     * @param id The default structured data id to use when formatting according to RFC 5424.
     * @param ein The IANA enterprise number.
     * @param includeMDC Indicates whether data from the ThreadContextMap will be included in the RFC 5424 Syslog
     * record. Defaults to "true:.
     * @param mdcId The id to use for the MDC Structured Data Element.
     * @param mdcPrefix The prefix to add to MDC key names.
     * @param eventPrefix The prefix to add to event key names.
     * @param includeNL If true, a newline will be appended to the end of the syslog record. The default is false.
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
     * @return A TLSSyslogAppender.
     */
    @PluginFactory
    public static TLSSyslogAppender createAppender(@PluginAttribute("host") final String host,
                                                   @PluginAttribute("port") final String portNum,
                                                   @PluginElement("ssl") final SSLConfiguration sslConfig,
                                                   @PluginAttribute("reconnectionDelay") final String delay,
                                                   @PluginAttribute("immediateFail") final String immediateFail,
                                                   @PluginAttribute("name") final String name,
                                                   @PluginAttribute("immediateFlush") final String immediateFlush,
                                                   @PluginAttribute("ignoreExceptions") final String ignore,
                                                   @PluginAttribute("facility") final String facility,
                                                   @PluginAttribute("id") final String id,
                                                   @PluginAttribute("enterpriseNumber") final String ein,
                                                   @PluginAttribute("includeMDC") final String includeMDC,
                                                   @PluginAttribute("mdcId") final String mdcId,
                                                   @PluginAttribute("mdcPrefix") final String mdcPrefix,
                                                   @PluginAttribute("eventPrefix") final String eventPrefix,
                                                   @PluginAttribute("newLine") final String includeNL,
                                                   @PluginAttribute("newLineEscape") final String escapeNL,
                                                   @PluginAttribute("appName") final String appName,
                                                   @PluginAttribute("messageId") final String msgId,
                                                   @PluginAttribute("mdcExcludes") final String excludes,
                                                   @PluginAttribute("mdcIncludes") final String includes,
                                                   @PluginAttribute("mdcRequired") final String required,
                                                   @PluginAttribute("format") final String format,
                                                   @PluginElement("filters") final Filter filter,
                                                   @PluginConfiguration final Configuration config,
                                                   @PluginAttribute("charset") final String charsetName,
                                                   @PluginAttribute("exceptionPattern") final String exceptionPattern,
                                                   @PluginElement("LoggerFields") final LoggerFields[] loggerFields,
                                                   @PluginAttribute("advertise") final String advertise) {
        final boolean isFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final int reconnectDelay = AbstractAppender.parseInt(delay, 0);
        final boolean fail = Booleans.parseBoolean(immediateFail, true);
        final int port = AbstractAppender.parseInt(portNum, 0);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        @SuppressWarnings("unchecked")
        final Layout<? extends Serializable> layout = (RFC5424.equalsIgnoreCase(format) ?
                RFC5424Layout.createLayout(facility, id, ein, includeMDC, mdcId, mdcPrefix, eventPrefix, includeNL,
                    escapeNL, appName, msgId, excludes, includes, required, exceptionPattern, "true" ,loggerFields,
                    config) :
                SyslogLayout.createLayout(facility, includeNL, escapeNL, charsetName));

        if (name == null) {
            LOGGER.error("No name provided for TLSSyslogAppender");
            return null;
        }
        final AbstractSocketManager manager = createSocketManager(sslConfig, host, port, reconnectDelay, fail, layout);
        if (manager == null) {
            return null;
        }

        return new TLSSyslogAppender(name, layout, filter, ignoreExceptions, isFlush, manager,
                isAdvertise ? config.getAdvertiser() : null);
    }

    public static AbstractSocketManager createSocketManager(SSLConfiguration sslConf, String host, int port,
                                                            int reconnectDelay, boolean fail,
                                                            Layout<? extends Serializable> layout) {
        return TLSSocketManager.getSocketManager(sslConf, host, port, reconnectDelay, fail, layout);
    }
}
