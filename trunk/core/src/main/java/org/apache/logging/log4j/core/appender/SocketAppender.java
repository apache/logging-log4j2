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
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.DatagramSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.TCPSocketManager;
import org.apache.logging.log4j.util.EnglishEnums;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An Appender that delivers events over socket connections. Supports both TCP and UDP.
 */
@Plugin(name = "Socket", category = "Core", elementType = "appender", printObject = true)
public class SocketAppender<T extends Serializable> extends AbstractOutputStreamAppender<T> {
    private Object advertisement;
    private final Advertiser advertiser;

    protected SocketAppender(final String name, final Layout<T> layout, final Filter filter,
                             final AbstractSocketManager manager, final boolean handleException,
                             final boolean immediateFlush, Advertiser advertiser) {
        super(name, layout, filter, handleException, immediateFlush, manager);
        if (advertiser != null)
        {
            Map<String, String> configuration = new HashMap<String, String>(layout.getContentFormat());
            configuration.putAll(manager.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            advertisement = advertiser.advertise(configuration);
        }
        this.advertiser = advertiser;
    }

    @Override
    public void stop() {
        super.stop();
        if (advertiser != null) {
            advertiser.unadvertise(advertisement);
        }
    }

    /**
     *
     * @param host The name of the host to connect to.
     * @param portNum The port to connect to on the target host.
     * @param protocol The Protocol to use.
     * @param delay The interval in which failed writes should be retried.
     * @param immediateFail True if the write should fail if no socket is immediately available.
     * @param name The name of the Appender.
     * @param immediateFlush "true" if data should be flushed on each write.
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @param layout The layout to use (defaults to SerializedLayout).
     * @param filter The Filter or null.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param config The Configuration
     * @return A SocketAppender.
     */
    @PluginFactory
    public static <S extends Serializable> SocketAppender<S> createAppender(@PluginAttr("host") final String host,
                                                @PluginAttr("port") final String portNum,
                                                @PluginAttr("protocol") final String protocol,
                                                @PluginAttr("reconnectionDelay") final String delay,
                                                @PluginAttr("immediateFail") final String immediateFail,
                                                @PluginAttr("name") final String name,
                                                @PluginAttr("immediateFlush") final String immediateFlush,
                                                @PluginAttr("suppressExceptions") final String suppress,
                                                @PluginElement("layout") Layout<S> layout,
                                                @PluginElement("filters") final Filter filter,
                                                @PluginAttr("advertise") final String advertise,
                                                @PluginConfiguration final Configuration config) {

        boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);
        boolean isAdvertise = advertise == null ? false : Boolean.valueOf(advertise);
        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        final boolean fail = immediateFail == null ? true : Boolean.valueOf(immediateFail);
        final int reconnectDelay = delay == null ? 0 : Integer.parseInt(delay);
        final int port = portNum == null ? 0 : Integer.parseInt(portNum);
        if (layout == null) {
            @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
            Layout<S> l = (Layout<S>) SerializedLayout.createLayout();
            layout = l;
        }

        if (name == null) {
            LOGGER.error("No name provided for SocketAppender");
            return null;
        }

        final String prot = protocol != null ? protocol : Protocol.TCP.name();
        final Protocol p = EnglishEnums.valueOf(Protocol.class, protocol);
        if (p.equals(Protocol.UDP)) {
            isFlush = true;
        }

        final AbstractSocketManager manager = createSocketManager(p, host, port, reconnectDelay, fail, layout);
        if (manager == null) {
            return null;
        }

        return new SocketAppender<S>(name, layout, filter, manager, handleExceptions, isFlush,
                isAdvertise ? config.getAdvertiser() : null);
    }

    protected static AbstractSocketManager createSocketManager(final Protocol p, final String host, final int port,
                                                               final int delay, final boolean immediateFail,
                                                               final Layout layout) {
        switch (p) {
            case TCP:
                return TCPSocketManager.getSocketManager(host, port, delay, immediateFail, layout);
            case UDP:
                return DatagramSocketManager.getSocketManager(host, port, layout);
            default:
                return null;
        }
    }
}
