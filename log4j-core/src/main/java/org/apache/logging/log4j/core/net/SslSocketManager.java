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
package org.apache.logging.log4j.core.net;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;

public class SslSocketManager extends TcpSocketManager {

    public static final int DEFAULT_PORT = 6514;

    private static final SslSocketManagerFactory FACTORY = new SslSocketManagerFactory();

    private final SslConfiguration sslConfig;

    /**
     *
     *
     * @param name          the unique name of this connection
     * @param os            the OutputStream
     * @param sock          the Socket
     * @param inetAddress          the Internet address of the host
     * @param host          the name of the host
     * @param port          the port number on the host
     * @param connectTimeoutMillis the connect timeout in milliseconds
     * @param reconnectionDelayMillis         Reconnection interval.
     * @param immediateFail True if the write should fail if no socket is immediately available.
     * @param layout        the Layout
     * @param bufferSize The buffer size.
     * @deprecated Use {@link #SslSocketManager(String, OutputStream, Socket, SslConfiguration, InetAddress, String, int, int, int, boolean, Layout, int, SocketOptions)}.
     */
    @Deprecated
    public SslSocketManager(
            final String name,
            final OutputStream os,
            final Socket sock,
            final SslConfiguration sslConfig,
            final InetAddress inetAddress,
            final String host,
            final int port,
            final int connectTimeoutMillis,
            final int reconnectionDelayMillis,
            final boolean immediateFail,
            final Layout<? extends Serializable> layout,
            final int bufferSize) {
        super(
                name,
                os,
                sock,
                inetAddress,
                host,
                port,
                connectTimeoutMillis,
                reconnectionDelayMillis,
                immediateFail,
                layout,
                bufferSize,
                null);
        this.sslConfig = sslConfig;
    }

    /**
     *
     *
     * @param name          The unique name of this connection.
     * @param os            The OutputStream.
     * @param sock          The Socket.
     * @param inetAddress          The Internet address of the host.
     * @param host          The name of the host.
     * @param port          The port number on the host.
     * @param connectTimeoutMillis the connect timeout in milliseconds.
     * @param reconnectionDelayMillis         Reconnection interval.
     * @param immediateFail True if the write should fail if no socket is immediately available.
     * @param layout        The Layout.
     * @param bufferSize The buffer size.
     */
    public SslSocketManager(
            final String name,
            final OutputStream os,
            final Socket sock,
            final SslConfiguration sslConfig,
            final InetAddress inetAddress,
            final String host,
            final int port,
            final int connectTimeoutMillis,
            final int reconnectionDelayMillis,
            final boolean immediateFail,
            final Layout<? extends Serializable> layout,
            final int bufferSize,
            final SocketOptions socketOptions) {
        super(
                name,
                os,
                sock,
                inetAddress,
                host,
                port,
                connectTimeoutMillis,
                reconnectionDelayMillis,
                immediateFail,
                layout,
                bufferSize,
                socketOptions);
        this.sslConfig = sslConfig;
    }

    private static class SslFactoryData extends FactoryData {
        protected SslConfiguration sslConfiguration;

        public SslFactoryData(
                final SslConfiguration sslConfiguration,
                final String host,
                final int port,
                final int connectTimeoutMillis,
                final int reconnectDelayMillis,
                final boolean immediateFail,
                final Layout<? extends Serializable> layout,
                final int bufferSize,
                final SocketOptions socketOptions) {
            super(
                    host,
                    port,
                    connectTimeoutMillis,
                    reconnectDelayMillis,
                    immediateFail,
                    layout,
                    bufferSize,
                    socketOptions);
            this.sslConfiguration = sslConfiguration;
        }

        @Override
        public String toString() {
            return "SslFactoryData [sslConfiguration=" + sslConfiguration + ", host=" + host + ", port=" + port
                    + ", connectTimeoutMillis=" + connectTimeoutMillis + ", reconnectDelayMillis="
                    + reconnectDelayMillis + ", immediateFail=" + immediateFail + ", layout=" + layout + ", bufferSize="
                    + bufferSize + ", socketOptions=" + socketOptions + "]";
        }
    }

    /**
     * @deprecated Use {@link SslSocketManager#getSocketManager(SslConfiguration, String, int, int, int, boolean, Layout, int, SocketOptions)}.
     */
    @Deprecated
    public static SslSocketManager getSocketManager(
            final SslConfiguration sslConfig,
            final String host,
            final int port,
            final int connectTimeoutMillis,
            final int reconnectDelayMillis,
            final boolean immediateFail,
            final Layout<? extends Serializable> layout,
            final int bufferSize) {
        return getSocketManager(
                sslConfig,
                host,
                port,
                connectTimeoutMillis,
                reconnectDelayMillis,
                immediateFail,
                layout,
                bufferSize,
                null);
    }

    public static SslSocketManager getSocketManager(
            final SslConfiguration sslConfig,
            final String host,
            int port,
            final int connectTimeoutMillis,
            int reconnectDelayMillis,
            final boolean immediateFail,
            final Layout<? extends Serializable> layout,
            final int bufferSize,
            final SocketOptions socketOptions) {

        // Check arguments
        if (Strings.isEmpty(host)) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (reconnectDelayMillis == 0) {
            reconnectDelayMillis = DEFAULT_RECONNECTION_DELAY_MILLIS;
        }

        // Create an ID associated with the SSL configuration. This is necessary to make sure a new `name` is generated
        // (and consequently a new connection pool is created) upon reconfiguration with a different configuration;
        // e.g., change in the SSL certificate content, even though the certificate file locations are still the same.
        // See #2767 and LOG4J2-2988 for details.
        final String sslConfigId = createSslConfigurationId(sslConfig);
        final String name = String.format("%s:%s:%d:%s", sslConfig.getProtocol(), host, port, sslConfigId);

        return (SslSocketManager) getManager(
                name,
                new SslFactoryData(
                        sslConfig,
                        host,
                        port,
                        connectTimeoutMillis,
                        reconnectDelayMillis,
                        immediateFail,
                        layout,
                        bufferSize,
                        socketOptions),
                FACTORY);
    }

    /**
     * Creates a unique identifier using the certificate issuers and serial numbers of the given SSL configuration.
     *
     * @param sslConfig an SSL configuration
     * @return a unique identifier extracted from the given SSL configuration
     */
    private static String createSslConfigurationId(final SslConfiguration sslConfig) {
        return String.valueOf(Stream.of(sslConfig.getKeyStoreConfig(), sslConfig.getTrustStoreConfig())
                .filter(Objects::nonNull)
                .flatMap(keyStoreConfig -> {
                    final Enumeration<String> aliases;
                    try {
                        aliases = keyStoreConfig.getKeyStore().aliases();
                    } catch (final KeyStoreException error) {
                        LOGGER.error(
                                "Failed reading the aliases for the key store located at `{}`",
                                keyStoreConfig.getLocation(),
                                error);
                        return Stream.empty();
                    }
                    return Collections.list(aliases).stream().sorted().flatMap(alias -> {
                        final X509Certificate certificate;
                        try {
                            certificate = (X509Certificate)
                                    keyStoreConfig.getKeyStore().getCertificate(alias);
                        } catch (final KeyStoreException error) {
                            LOGGER.error(
                                    "Failed reading the certificate of alias `{}` for the key store located at `{}`",
                                    alias,
                                    keyStoreConfig.getLocation(),
                                    error);
                            return Stream.empty();
                        }
                        final String issuer =
                                certificate.getIssuerX500Principal().getName();
                        final String serialNumber =
                                certificate.getSerialNumber().toString();
                        return Stream.of(issuer, serialNumber);
                    });
                })
                .collect(Collectors.toList())
                .hashCode());
    }

    @Override
    protected Socket createSocket(final InetSocketAddress socketAddress) throws IOException {
        return createSocket(getHost(), socketAddress, getConnectTimeoutMillis(), sslConfig, getSocketOptions());
    }

    private static SSLSocketFactory createSslSocketFactory(final SslConfiguration sslConf) {
        if (sslConf != null) {
            final SSLContext sslContext = sslConf.getSslContext();
            if (sslContext != null) {
                return sslContext.getSocketFactory();
            }
        }
        return (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private static class SslSocketManagerFactory extends TcpSocketManagerFactory<SslSocketManager, SslFactoryData> {

        @Override
        SslSocketManager createManager(
                final String name,
                final OutputStream os,
                final Socket socket,
                final InetAddress inetAddress,
                final SslFactoryData data) {
            return new SslSocketManager(
                    name,
                    os,
                    socket,
                    data.sslConfiguration,
                    inetAddress,
                    data.host,
                    data.port,
                    data.connectTimeoutMillis,
                    data.reconnectDelayMillis,
                    data.immediateFail,
                    data.layout,
                    data.bufferSize,
                    data.socketOptions);
        }

        @Override
        Socket createSocket(final SslFactoryData data) throws IOException {
            final List<InetSocketAddress> socketAddresses = RESOLVER.resolveHost(data.host, data.port);
            IOException ioe = null;
            for (InetSocketAddress socketAddress : socketAddresses) {
                try {
                    return SslSocketManager.createSocket(
                            data.host,
                            socketAddress,
                            data.connectTimeoutMillis,
                            data.sslConfiguration,
                            data.socketOptions);
                } catch (IOException ex) {
                    final String message = String.format(
                            "failed create a socket to `%s:%s` that is resolved to address `%s`",
                            data.host, data.port, socketAddress);
                    final IOException newEx = new IOException(message, ex);
                    if (ioe == null) {
                        ioe = newEx;
                    } else {
                        ioe.addSuppressed(newEx);
                    }
                }
            }
            throw new IOException(errorMessage(data, socketAddresses), ioe);
        }
    }

    private static Socket createSocket(
            final String hostName,
            final InetSocketAddress socketAddress,
            final int connectTimeoutMillis,
            final SslConfiguration sslConfiguration,
            final SocketOptions socketOptions)
            throws IOException {

        // Create the `SSLSocket`
        final SSLSocketFactory socketFactory = createSslSocketFactory(sslConfiguration);
        final SSLSocket socket = (SSLSocket) socketFactory.createSocket();

        // Apply socket options before `connect()`
        if (socketOptions != null) {
            socketOptions.apply(socket);
        }

        // Connect the socket
        socket.connect(socketAddress, connectTimeoutMillis);

        // Verify the host name
        if (sslConfiguration.isVerifyHostName()) {
            // Allowed endpoint identification algorithms: HTTPS and LDAPS.
            // https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#endpoint-identification-algorithms
            final SSLParameters sslParameters = socket.getSSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
            final SNIHostName serverName = createSniHostName(hostName);
            if (serverName != null) {
                sslParameters.setServerNames(Collections.singletonList(serverName));
            }
            socket.setSSLParameters(sslParameters);
        }

        // Force the handshake right after `connect()` instead of waiting for read/write to trigger it indirectly at a
        // later stage
        socket.startHandshake();

        return socket;
    }

    /**
     * {@return an {@link SNIHostName} instance if the provided host name is not an IP literal (RFC 6066), and constitutes a valid host name (RFC 1035); null otherwise}
     *
     * @param hostName a host name
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc6066.html#section-3">Literal IPv4 and IPv6 addresses are not permitted in "HostName" (RFC 6066)</a>
     * @see <a href="https://www.rfc-editor.org/rfc/rfc1035.html">Domain Names - Implementation and Specification (RFC 1035)</a>
     */
    @Nullable
    static SNIHostName createSniHostName(String hostName) {
        // The actual check should be
        //
        //     !isIPv4(h) && !isIPv6(h) && isValidHostName(h)
        //
        // Though we translate this into
        //
        //     !h.matches("\d+[.]\d+[.]\d+[.]\d+") && new SNIServerName(h)
        //
        // This simplification is possible because
        //
        // - The `\d+[.]\d+[.]\d+[.]\d+` is sufficient to eliminate IPv4 addresses.
        //   Any sequence of four numeric labels (e.g., `1234.2345.3456.4567`) is not a valid host name.
        //   Hence, false positives are not a problem, they would be eliminated by `isValidHostName()` anyway.
        //
        // - `SNIServerName::new` throws an exception on invalid host names.
        //   This check is performed using `IDN.toASCII(hostName, IDN.USE_STD3_ASCII_RULES)`.
        //   IPv6 literals don't qualify as a valid host name by `IDN::toASCII`.
        //   This assumption on `IDN` is unlikely to change in the foreseeable future.
        if (!hostName.matches("\\d+[.]\\d+[.]\\d+[.]\\d+")) {
            try {
                return new SNIHostName(hostName);
            } catch (IllegalArgumentException ignored) {
                // Do nothing
            }
        }
        return null;
    }
}
