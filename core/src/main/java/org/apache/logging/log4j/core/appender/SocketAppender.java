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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.DatagramSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.TCPSocketManager;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * An Appender that delivers events over socket connections. Supports both TCP and UDP.
 */
@Plugin(name = "Socket", type = "Core", elementType = "appender", printObject = true)
public class SocketAppender extends AbstractOutputStreamAppender {


    protected SocketAppender(final String name, final Layout layout, final Filter filter,
                             final AbstractSocketManager manager, final boolean handleException,
                             final boolean immediateFlush) {
        super(name, layout, filter, handleException, immediateFlush, manager);

    }

    /**
     *
     * @param host The name of the host to connect to.
     * @param portNum The port to connect to on the target host.
     * @param protocol The Protocol to use.
     * @param delay The interval in which failed writes should be retried.
     * @param name The name of the Appender.
     * @param immediateFlush "true" if data should be flushed on each write.
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @param layout The layout to use (defaults to SerializedLayout).
     * @param filter The Filter or null.
     * @return A SocketAppender.
     */
    @PluginFactory
    public static SocketAppender createAppender(@PluginAttr("host") final String host,
                                                @PluginAttr("port") final String portNum,
                                                @PluginAttr("protocol") final String protocol,
                                                @PluginAttr("reconnectionDelay") final String delay,
                                                @PluginAttr("name") final String name,
                                                @PluginAttr("immediateFlush") final String immediateFlush,
                                                @PluginAttr("suppressExceptions") final String suppress,
                                                @PluginElement("layout") Layout layout,
                                                @PluginElement("filters") final Filter filter) {

        final boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);
        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        final int reconnectDelay = delay == null ? 0 : Integer.parseInt(delay);
        final int port = portNum == null ? 0 : Integer.parseInt(portNum);
        if (layout == null) {
            layout = SerializedLayout.createLayout();
        }

        if (name == null) {
            LOGGER.error("No name provided for SocketAppender");
            return null;
        }

        final String prot = protocol != null ? protocol : Protocol.TCP.name();

        final AbstractSocketManager manager = createSocketManager(prot, host, port, reconnectDelay);
        if (manager == null) {
            return null;
        }
        return new SocketAppender(name, layout, filter, manager, handleExceptions, isFlush);
    }

    protected static AbstractSocketManager createSocketManager(final String protocol, final String host, final int port,
                                                               final int delay) {
        final Protocol p = EnglishEnums.valueOf(Protocol.class, protocol);
        switch (p) {
            case TCP:
                return TCPSocketManager.getSocketManager(host, port, delay);
            case UDP:
                return DatagramSocketManager.getSocketManager(host, port);
            default:
                return null;
        }
    }
}
