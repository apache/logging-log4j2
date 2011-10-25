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
 *
 */
@Plugin(name="Syslog",type="Core",elementType="appender",printObject=true)
public class SyslogAppender extends SocketAppender {

    public static final String BSD = "bsd";

    public static final String RFC5424 = "RFC5424";

    public SyslogAppender(String name, Layout layout, Filter filter,
                          boolean handleException, boolean immediateFlush, AbstractSocketManager manager) {
        super(name, layout, filter, manager, handleException, immediateFlush);

    }


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

        boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);;
        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        int reconnectDelay = delay == null ? 0 : Integer.parseInt(delay);
        int port = portNum == null ? 0 : Integer.parseInt(portNum);
        Charset c = Charset.isSupported("UTF-8") ? Charset.forName("UTF-8") : Charset.defaultCharset();
        if (charset != null) {
            if (Charset.isSupported(charset)) {
                c = Charset.forName(charset);
            } else {
                logger.error("Charset " + charset + " is not supported for layout, using " + c.displayName());
            }
        }
        Layout layout = (format.equalsIgnoreCase(RFC5424)) ?
            RFC5424Layout.createLayout(facility, id, ein, includeMDC, mdcId, includeNL, appName,  msgId,
                excludes, includes, required, charset, config) :
            SyslogLayout.createLayout(facility, includeNL, charset);

        if (name == null) {
            logger.error("No name provided for SyslogAppender");
            return null;
        }
        AbstractSocketManager manager = createSocketManager(protocol, host, port, reconnectDelay);
        if (manager == null) {
            return null;
        }

        return new SyslogAppender(name, layout, filter, handleExceptions, isFlush, manager);
    }


}
