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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.DatagramSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.SslSocketManager;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * An Appender that delivers events over socket connections. Supports both TCP and UDP.
 */
@Plugin(name = "Socket", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class SocketAppender extends AbstractOutputStreamAppender<AbstractSocketManager> {

    /**
     * Subclasses can extend this abstract Builder.
     * <h2>Defaults</h2>
     * <ul>
     * <li>host: "localhost"</li>
     * <li>protocol: "TCP"</li>
     * </ul>
     * <h2>Changes</h2>
     * <ul>
     * <li>Removed deprecated "delayMillis", use "reconnectionDelayMillis".</li>
     * <li>Removed deprecated "reconnectionDelay", use "reconnectionDelayMillis".</li>
     * </ul>
     *
     * @param <B>
     *            The type to build.
     */
    public abstract static class AbstractBuilder<B extends AbstractBuilder<B>>
            extends AbstractOutputStreamAppender.Builder<B> {

        @PluginBuilderAttribute
        private boolean advertise;

        @PluginBuilderAttribute
        private int connectTimeoutMillis;

        @PluginBuilderAttribute
        @ValidHost
        private String host = "localhost";

        @PluginBuilderAttribute
        private boolean immediateFail = true;

        @PluginBuilderAttribute
        @ValidPort
        private int port;

        @PluginBuilderAttribute
        private Protocol protocol = Protocol.TCP;

        @PluginBuilderAttribute
        @PluginAliases({"reconnectDelay", "reconnectionDelay", "delayMillis", "reconnectionDelayMillis"})
        private int reconnectDelayMillis;

        @PluginElement("SocketOptions")
        private SocketOptions socketOptions;

        @PluginElement("SslConfiguration")
        @PluginAliases({"SslConfig"})
        private SslConfiguration sslConfiguration;

        public boolean getAdvertise() {
            return advertise;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public Protocol getProtocol() {
            return protocol;
        }

        public SslConfiguration getSslConfiguration() {
            return sslConfiguration;
        }

        public boolean getImmediateFail() {
            return immediateFail;
        }

        public B setAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        public B setConnectTimeoutMillis(final int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return asBuilder();
        }

        public B setHost(final String host) {
            this.host = host;
            return asBuilder();
        }

        public B setImmediateFail(final boolean immediateFail) {
            this.immediateFail = immediateFail;
            return asBuilder();
        }

        public B setPort(final int port) {
            this.port = port;
            return asBuilder();
        }

        public B setProtocol(final Protocol protocol) {
            this.protocol = protocol;
            return asBuilder();
        }

        public B setReconnectDelayMillis(final int reconnectDelayMillis) {
            this.reconnectDelayMillis = reconnectDelayMillis;
            return asBuilder();
        }

        public B setSocketOptions(final SocketOptions socketOptions) {
            this.socketOptions = socketOptions;
            return asBuilder();
        }

        public B setSslConfiguration(final SslConfiguration sslConfiguration) {
            this.sslConfiguration = sslConfiguration;
            return asBuilder();
        }

        @Deprecated
        public B withAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        @Deprecated
        public B withConnectTimeoutMillis(final int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return asBuilder();
        }

        @Deprecated
        public B withHost(final String host) {
            this.host = host;
            return asBuilder();
        }

        @Deprecated
        public B withImmediateFail(final boolean immediateFail) {
            this.immediateFail = immediateFail;
            return asBuilder();
        }

        @Deprecated
        public B withPort(final int port) {
            this.port = port;
            return asBuilder();
        }

        @Deprecated
        public B withProtocol(final Protocol protocol) {
            this.protocol = protocol;
            return asBuilder();
        }

        @Deprecated
        public B withReconnectDelayMillis(final int reconnectDelayMillis) {
            this.reconnectDelayMillis = reconnectDelayMillis;
            return asBuilder();
        }

        @Deprecated
        public B withSocketOptions(final SocketOptions socketOptions) {
            this.socketOptions = socketOptions;
            return asBuilder();
        }

        @Deprecated
        public B withSslConfiguration(final SslConfiguration sslConfiguration) {
            this.sslConfiguration = sslConfiguration;
            return asBuilder();
        }

        public int getReconnectDelayMillis() {
            return reconnectDelayMillis;
        }

        public SocketOptions getSocketOptions() {
            return socketOptions;
        }
    }

    /**
     * Builds a SocketAppender.
     * <ul>
     * <li>Removed deprecated "delayMillis", use "reconnectionDelayMillis".</li>
     * <li>Removed deprecated "reconnectionDelay", use "reconnectionDelayMillis".</li>
     * </ul>
     */
    public static class Builder extends AbstractBuilder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<SocketAppender> {

        @SuppressWarnings("resource")
        @Override
        public SocketAppender build() {
            boolean immediateFlush = isImmediateFlush();
            final boolean bufferedIo = isBufferedIo();
            final Layout<? extends Serializable> layout = getLayout();
            if (layout == null) {
                AbstractLifeCycle.LOGGER.error("No layout provided for SocketAppender");
                return null;
            }

            final String name = getName();
            if (name == null) {
                AbstractLifeCycle.LOGGER.error("No name provided for SocketAppender");
                return null;
            }

            final Protocol protocol = getProtocol();
            final Protocol actualProtocol = protocol != null ? protocol : Protocol.TCP;
            if (actualProtocol == Protocol.UDP) {
                immediateFlush = true;
            }

            final AbstractSocketManager manager = SocketAppender.createSocketManager(
                    name,
                    actualProtocol,
                    getHost(),
                    getPort(),
                    getConnectTimeoutMillis(),
                    getSslConfiguration(),
                    getReconnectDelayMillis(),
                    getImmediateFail(),
                    layout,
                    getBufferSize(),
                    getSocketOptions());

            return new SocketAppender(
                    name,
                    layout,
                    getFilter(),
                    manager,
                    isIgnoreExceptions(),
                    !bufferedIo || immediateFlush,
                    getAdvertise() ? getConfiguration().getAdvertiser() : null,
                    getPropertyArray());
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private final Object advertisement;
    private final Advertiser advertiser;

    protected SocketAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final AbstractSocketManager manager,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final Advertiser advertiser,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, properties, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
            configuration.putAll(manager.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            this.advertisement = advertiser.advertise(configuration);
        } else {
            this.advertisement = null;
        }
        this.advertiser = advertiser;
    }

    /**
     * @deprecated {@link #SocketAppender(String, Layout, Filter, AbstractSocketManager, boolean, boolean, Advertiser, Property[])}.
     */
    @Deprecated
    protected SocketAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final AbstractSocketManager manager,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final Advertiser advertiser) {
        this(name, layout, filter, manager, ignoreExceptions, immediateFlush, advertiser, Property.EMPTY_ARRAY);
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        if (this.advertiser != null) {
            this.advertiser.unadvertise(this.advertisement);
        }
        setStopped();
        return true;
    }

    /**
     * Creates a socket appender.
     *
     * @param host
     *            The name of the host to connect to.
     * @param port
     *            The port to connect to on the target host.
     * @param protocol
     *            The Protocol to use.
     * @param sslConfig
     *            The SSL configuration file for TCP/SSL, ignored for UPD.
     * @param connectTimeoutMillis
     *            the connect timeout in milliseconds.
     * @param reconnectDelayMillis
     *            The interval in which failed writes should be retried.
     * @param immediateFail
     *            True if the write should fail if no socket is immediately available.
     * @param name
     *            The name of the Appender.
     * @param immediateFlush
     *            "true" if data should be flushed on each write.
     * @param ignoreExceptions
     *            If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @param layout
     *            The layout to use. Required, there is no default.
     * @param filter
     *            The Filter or null.
     * @param advertise
     *            "true" if the appender configuration should be advertised, "false" otherwise.
     * @param configuration
     *            The Configuration
     * @return A SocketAppender.
     * @deprecated Deprecated in 2.7; use {@link #newBuilder()}
     */
    @Deprecated
    @PluginFactory
    public static SocketAppender createAppender(
            // @formatter:off
            final String host,
            final int port,
            final Protocol protocol,
            final SslConfiguration sslConfig,
            final int connectTimeoutMillis,
            final int reconnectDelayMillis,
            final boolean immediateFail,
            final String name,
            final boolean immediateFlush,
            final boolean ignoreExceptions,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final boolean advertise,
            final Configuration configuration) {
        // @formatter:on

        // @formatter:off
        return newBuilder()
                .setAdvertise(advertise)
                .setConfiguration(configuration)
                .setConnectTimeoutMillis(connectTimeoutMillis)
                .setFilter(filter)
                .setHost(host)
                .setIgnoreExceptions(ignoreExceptions)
                .setImmediateFail(immediateFail)
                .setLayout(layout)
                .setName(name)
                .setPort(port)
                .setProtocol(protocol)
                .setReconnectDelayMillis(reconnectDelayMillis)
                .setSslConfiguration(sslConfig)
                .build();
        // @formatter:on
    }

    /**
     * Creates a socket appender.
     *
     * @param host
     *            The name of the host to connect to.
     * @param portNum
     *            The port to connect to on the target host.
     * @param protocolIn
     *            The Protocol to use.
     * @param sslConfig
     *            The SSL configuration file for TCP/SSL, ignored for UPD.
     * @param connectTimeoutMillis
     *            the connect timeout in milliseconds.
     * @param delayMillis
     *            The interval in which failed writes should be retried.
     * @param immediateFail
     *            True if the write should fail if no socket is immediately available.
     * @param name
     *            The name of the Appender.
     * @param immediateFlush
     *            "true" if data should be flushed on each write.
     * @param ignore
     *            If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @param layout
     *            The layout to use. Required, there is no default.
     * @param filter
     *            The Filter or null.
     * @param advertise
     *            "true" if the appender configuration should be advertised, "false" otherwise.
     * @param config
     *            The Configuration
     * @return A SocketAppender.
     * @deprecated Deprecated in 2.5; use {@link #newBuilder()}
     */
    @Deprecated
    public static SocketAppender createAppender(
            // @formatter:off
            final String host,
            final String portNum,
            final String protocolIn,
            final SslConfiguration sslConfig,
            final int connectTimeoutMillis,
            // deprecated
            final String delayMillis,
            final String immediateFail,
            final String name,
            final String immediateFlush,
            final String ignore,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final String advertise,
            final Configuration config) {
        // @formatter:on
        final boolean isFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final boolean fail = Booleans.parseBoolean(immediateFail, true);
        final int reconnectDelayMillis = AbstractAppender.parseInt(delayMillis, 0);
        final int port = AbstractAppender.parseInt(portNum, 0);
        final Protocol p = protocolIn == null ? Protocol.UDP : Protocol.valueOf(protocolIn);
        return createAppender(
                host,
                port,
                p,
                sslConfig,
                connectTimeoutMillis,
                reconnectDelayMillis,
                fail,
                name,
                isFlush,
                ignoreExceptions,
                layout,
                filter,
                isAdvertise,
                config);
    }

    /**
     * Creates an AbstractSocketManager for TCP, UDP, and SSL.
     *
     * @throws IllegalArgumentException
     *             if the protocol cannot be handled.
     * @deprecated Use {@link #createSocketManager(String, Protocol, String, int, int, SslConfiguration, int, boolean, Layout, int, SocketOptions)}.
     */
    @Deprecated
    protected static AbstractSocketManager createSocketManager(
            final String name,
            final Protocol protocol,
            final String host,
            final int port,
            final int connectTimeoutMillis,
            final SslConfiguration sslConfig,
            final int reconnectDelayMillis,
            final boolean immediateFail,
            final Layout<? extends Serializable> layout,
            final int bufferSize) {
        return createSocketManager(
                name,
                protocol,
                host,
                port,
                connectTimeoutMillis,
                sslConfig,
                reconnectDelayMillis,
                immediateFail,
                layout,
                bufferSize,
                null);
    }

    /**
     * Creates an AbstractSocketManager for TCP, UDP, and SSL.
     *
     * @throws IllegalArgumentException
     *             if the protocol cannot be handled.
     */
    protected static AbstractSocketManager createSocketManager(
            final String name,
            Protocol protocol,
            final String host,
            final int port,
            final int connectTimeoutMillis,
            final SslConfiguration sslConfig,
            final int reconnectDelayMillis,
            final boolean immediateFail,
            final Layout<? extends Serializable> layout,
            final int bufferSize,
            final SocketOptions socketOptions) {
        if (protocol == Protocol.TCP && sslConfig != null) {
            // Upgrade TCP to SSL if an SSL config is specified.
            protocol = Protocol.SSL;
        }
        if (protocol != Protocol.SSL && sslConfig != null) {
            LOGGER.info("Appender {} ignoring SSL configuration for {} protocol", name, protocol);
        }
        switch (protocol) {
            case TCP:
                return TcpSocketManager.getSocketManager(
                        host,
                        port,
                        connectTimeoutMillis,
                        reconnectDelayMillis,
                        immediateFail,
                        layout,
                        bufferSize,
                        socketOptions);
            case UDP:
                return DatagramSocketManager.getSocketManager(host, port, layout, bufferSize);
            case SSL:
                return SslSocketManager.getSocketManager(
                        sslConfig,
                        host,
                        port,
                        connectTimeoutMillis,
                        reconnectDelayMillis,
                        immediateFail,
                        layout,
                        bufferSize,
                        socketOptions);
            default:
                throw new IllegalArgumentException(protocol.toString());
        }
    }

    @Override
    protected void directEncodeEvent(final LogEvent event) {
        // Disable garbage-free logging for now:
        // problem with UDP: 8K buffer size means that largish messages get broken up into chunks
        writeByteArrayToManager(event); // revert to classic (non-garbage free) logging
    }
}
