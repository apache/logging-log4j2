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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.DatagramSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.SslSocketManager;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * An Appender that delivers events over socket connections. Supports both TCP and UDP.
 */
@Plugin(name = "Socket", category = "Core", elementType = "appender", printObject = true)
public class SocketAppender extends AbstractOutputStreamAppender<AbstractSocketManager> {

    private static final long serialVersionUID = 1L;

    private Object advertisement;
    private final Advertiser advertiser;

    protected SocketAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
            final AbstractSocketManager manager, final boolean ignoreExceptions, final boolean immediateFlush,
            final Advertiser advertiser) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<String, String>(layout.getContentFormat());
            configuration.putAll(manager.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            this.advertisement = advertiser.advertise(configuration);
        }
        this.advertiser = advertiser;
    }

    @Override
    public void stop() {
        super.stop();
        if (this.advertiser != null) {
            this.advertiser.unadvertise(this.advertisement);
        }
    }

    /**
     * 
     * @param host
     *        The name of the host to connect to.
     * @param portNum
     *        The port to connect to on the target host.
     * @param protocolStr
     *        The Protocol to use.
     * @param sslConfig
     *        The SSL configuration file for TCP/SSL, ignored for UPD.
     * @param connectTimeoutMillis
     *        the connect timeout in milliseconds.
     * @param delayMillis
     *        The interval in which failed writes should be retried.
     * @param immediateFail
     *        True if the write should fail if no socket is immediately available.
     * @param name
     *        The name of the Appender.
     * @param immediateFlush
     *        "true" if data should be flushed on each write.
     * @param ignore
     *        If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they are
     *        propagated to the caller.
     * @param layout
     *        The layout to use (defaults to SerializedLayout).
     * @param filter
     *        The Filter or null.
     * @param advertise
     *        "true" if the appender configuration should be advertised, "false" otherwise.
     * @param config
     *        The Configuration
     * @return A SocketAppender.
     */
    @PluginFactory
    public static SocketAppender createAppender(
            // @formatter:off
            @PluginAttribute("host") final String host,
            @PluginAttribute("port") final String portNum,
            @PluginAttribute("protocol") final String protocolStr,
            @PluginElement("SSL") final SslConfiguration sslConfig,
            @PluginAttribute(value = "connectTimeoutMillis", defaultInt = 0) final int connectTimeoutMillis,
            @PluginAliases("reconnectionDelay") // deprecated
            @PluginAttribute("reconnectionDelayMillis") final String delayMillis,
            @PluginAttribute("immediateFail") final String immediateFail,
            @PluginAttribute("name") final String name,
            @PluginAttribute("immediateFlush") final String immediateFlush,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter, 
            @PluginAttribute("advertise") final String advertise, @PluginConfiguration final Configuration config) {
            // @formatter:on
        boolean isFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final boolean fail = Booleans.parseBoolean(immediateFail, true);
        final int reconnectDelayMillis = AbstractAppender.parseInt(delayMillis, 0);
        final int port = AbstractAppender.parseInt(portNum, 0);
        if (layout == null) {
            layout = SerializedLayout.createLayout();
        }

        if (name == null) {
            LOGGER.error("No name provided for SocketAppender");
            return null;
        }

        final Protocol protocol = EnglishEnums.valueOf(Protocol.class,
                protocolStr != null ? protocolStr : Protocol.TCP.name());
        if (protocol == Protocol.UDP) {
            isFlush = true;
        }

        final AbstractSocketManager manager = createSocketManager(name, protocol, host, port, connectTimeoutMillis,
                sslConfig, reconnectDelayMillis, fail, layout);

        return new SocketAppender(name, layout, filter, manager, ignoreExceptions, isFlush,
                isAdvertise ? config.getAdvertiser() : null);
    }

    /**
     * Creates an AbstractSocketManager for TCP, UDP, and SSL.
     * 
     * @throws IllegalArgumentException
     *         if the protocol cannot be handled.
     */
    protected static AbstractSocketManager createSocketManager(final String name, Protocol protocol, final String host,
            final int port, int connectTimeoutMillis, final SslConfiguration sslConfig, final int delayMillis,
            final boolean immediateFail, final Layout<? extends Serializable> layout) {
        if (protocol == Protocol.TCP && sslConfig != null) {
            // Upgrade TCP to SSL if an SSL config is specified.
            protocol = Protocol.SSL;
        }
        if (protocol != Protocol.SSL && sslConfig != null) {
            LOGGER.info("Appender {} ignoring SSL configuration for {} protocol", name, protocol);
        }
        switch (protocol) {
        case TCP:
            return TcpSocketManager.getSocketManager(host, port, connectTimeoutMillis, delayMillis, immediateFail,
                    layout);
        case UDP:
            return DatagramSocketManager.getSocketManager(host, port, layout);
        case SSL:
            return SslSocketManager.getSocketManager(sslConfig, host, port, connectTimeoutMillis, delayMillis,
                    immediateFail, layout);
        default:
            throw new IllegalArgumentException(protocol.toString());
        }
    }
}
