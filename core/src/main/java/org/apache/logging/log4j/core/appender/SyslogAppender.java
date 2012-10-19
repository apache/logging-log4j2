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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.RFC5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;

import java.nio.charset.Charset;

/**
 * The Syslog Appender.
 */
@Plugin(name = "Syslog", type = "Core", elementType = "appender", printObject = true)
public class SyslogAppender extends SocketAppender {

    private static final String BSD = "bsd";

    private static final String RFC5424 = "RFC5424";

    protected SyslogAppender(String name, Layout layout, Filter filter,
                          boolean handleException, boolean immediateFlush, AbstractSocketManager manager) {
        super(name, layout, filter, manager, handleException, immediateFlush);

    }

    /**
     * Create a SyslogAppender.
     * @param host The name of the host to connect to.
     * @param portNum The port to connect to on the target host.
     * @param protocol The Protocol to use.
     * @param delay The interval in which failed writes should be retried.
     * @param name The name of the Appender.
     * @param immediateFlush "true" if data should be flushed on each write.
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @param facility The Facility is used to try to classify the message.
     * @param id The default structured data id to use when formatting according to RFC 5424.
     * @param ein The IANA enterprise number.
     * @param includeMDC Indicates whether data from the ThreadContextMap will be included in the RFC 5424 Syslog
     * record. Defaults to "true:.
     * @param mdcId The id to use for the MDC Structured Data Element.
     * @param includeNL If true, a newline will be appended to the end of the syslog record. The default is false.
     * @param appName The value to use as the APP-NAME in the RFC 5424 syslog record.
     * @param msgId The default value to be used in the MSGID field of RFC 5424 syslog records.
     * @param excludes A comma separated list of mdc keys that should be excluded from the LogEvent.
     * @param includes A comma separated list of mdc keys that should be included in the FlumeEvent.
     * @param required A comma separated list of mdc keys that must be present in the MDC.
     * @param format If set to "RFC5424" the data will be formatted in accordance with RFC 5424. Otherwise,
     * it will be formatted as a BSD Syslog record.
     * @param filter A Filter to determine if the event should be handled by this Appender.
     * @param config The Configuration.
     * @param charset The character set to use when converting the syslog String to a byte array.
     * @return A SyslogAppender.
     */
    @PluginFactory
    public static SyslogAppender createAppender(@PluginAttr("host") String host,
                                                @PluginAttr("port") String portNum,
                                                @PluginAttr("protocol") String protocol,
                                                @PluginAttr("reconnectionDelay") String delay,
                                                @PluginAttr("name") String name,
                                                @PluginAttr("immediateFlush") String immediateFlush,
                                                @PluginAttr("suppressExceptions") String suppress,
                                                @PluginAttr("facility") String facility,
                                                @PluginAttr("id") String id,
                                                @PluginAttr("enterpriseNumber") String ein,
                                                @PluginAttr("includeMDC") String includeMDC,
                                                @PluginAttr("mdcId") String mdcId,
                                                @PluginAttr("newLine") String includeNL,
                                                @PluginAttr("appName") String appName,
                                                @PluginAttr("messageId") String msgId,
                                                @PluginAttr("mdcExcludes") String excludes,
                                                @PluginAttr("mdcIncludes") String includes,
                                                @PluginAttr("mdcRequired") String required,
                                                @PluginAttr("format") String format,
                                                @PluginElement("filters") Filter filter,
                                                @PluginConfiguration Configuration config,
                                                @PluginAttr("charset") String charset) {

        boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);
        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        int reconnectDelay = delay == null ? 0 : Integer.parseInt(delay);
        int port = portNum == null ? 0 : Integer.parseInt(portNum);
        Charset c = Charset.isSupported("UTF-8") ? Charset.forName("UTF-8") : Charset.defaultCharset();
        if (charset != null) {
            if (Charset.isSupported(charset)) {
                c = Charset.forName(charset);
            } else {
                LOGGER.error("Charset " + charset + " is not supported for layout, using " + c.displayName());
            }
        }
        Layout layout = (RFC5424.equalsIgnoreCase(format)) ?
            RFC5424Layout.createLayout(facility, id, ein, includeMDC, mdcId, includeNL, appName,  msgId,
                excludes, includes, required, charset, config) :
            SyslogLayout.createLayout(facility, includeNL, charset);

        if (name == null) {
            LOGGER.error("No name provided for SyslogAppender");
            return null;
        }
        AbstractSocketManager manager = createSocketManager(protocol, host, port, reconnectDelay);
        if (manager == null) {
            return null;
        }

        return new SyslogAppender(name, layout, filter, handleExceptions, isFlush, manager);
    }


}
