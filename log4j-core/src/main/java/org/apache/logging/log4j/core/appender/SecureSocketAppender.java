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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.TlsSocketManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.util.EnglishEnums;

/**
 * An Appender that delivers events over secure socket connections (SSL/TLS).
 */
@Plugin(name = "SecureSocket", category = "Core", elementType = "appender", printObject = true)
public class SecureSocketAppender extends SocketAppender {

    protected SecureSocketAppender(String name, Layout<? extends Serializable> layout, Filter filter,
            AbstractSocketManager manager, boolean ignoreExceptions, boolean immediateFlush, Advertiser advertiser) {
        super(name, layout, filter, manager, ignoreExceptions, immediateFlush, advertiser);
    }

    /**
     * 
     * @param host
     *        The name of the host to connect to.
     * @param portNum
     *        The port to connect to on the target host.
     * @param sslConfig 
     * @param protocol
     *        The Protocol to use.
     * @param delay
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
    public static SecureSocketAppender createAppender(
            // @formatter:off
            @PluginAttribute("host") final String host,
            @PluginAttribute("port") final String portNum,
            @PluginElement("Ssl") final SslConfiguration sslConfig,
            @PluginAttribute("protocol") final String protocol,
            @PluginAttribute("reconnectionDelay") final String delay,
            @PluginAttribute("immediateFail") final String immediateFail,
            @PluginAttribute("name") final String name,
            @PluginAttribute("immediateFlush") final String immediateFlush,
            @PluginAttribute("ignoreExceptions") final String ignore,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filters") final Filter filter,
            @PluginAttribute("advertise") final String advertise,
            @PluginConfiguration final Configuration config) {
            // @formatter:on
        boolean isFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final boolean fail = Booleans.parseBoolean(immediateFail, true);
        final int reconnectDelay = AbstractAppender.parseInt(delay, 0);
        final int port = AbstractAppender.parseInt(portNum, 0);
        if (layout == null) {
            layout = SerializedLayout.createLayout();
        }

        if (name == null) {
            LOGGER.error("No name provided for SecureSocketAppender");
            return null;
        }

        final Protocol p = EnglishEnums.valueOf(Protocol.class, protocol != null ? protocol : Protocol.TLS.name());

        final AbstractSocketManager manager = createSocketManager(sslConfig, p, host, port, reconnectDelay, fail, layout);
        if (manager == null) {
            return null;
        }

        return new SecureSocketAppender(name, layout, filter, manager, ignoreExceptions, isFlush,
                isAdvertise ? config.getAdvertiser() : null);
    }

    protected static AbstractSocketManager createSocketManager(final SslConfiguration sslConfig,
            final Protocol protocol, final String host, final int port, final int delay, final boolean immediateFail,
            final Layout<? extends Serializable> layout) {
        switch (protocol) {
        case TLS:
            return TlsSocketManager.getSocketManager(sslConfig, host, port, delay, immediateFail, layout);
        default:
            return null;
        }
    }
}
