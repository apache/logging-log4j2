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
import java.util.List;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.util.Strings;

/**
 *
 */
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
        if (Strings.isEmpty(host)) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (reconnectDelayMillis == 0) {
            reconnectDelayMillis = DEFAULT_RECONNECTION_DELAY_MILLIS;
        }
        final String name = "TLS:" + host + ':' + port;
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

    @Override
    protected Socket createSocket(final InetSocketAddress socketAddress) throws IOException {
        final SSLSocketFactory socketFactory = createSslSocketFactory(sslConfig);
        final Socket newSocket = socketFactory.createSocket();
        newSocket.connect(socketAddress, getConnectTimeoutMillis());
        return newSocket;
    }

    private static SSLSocketFactory createSslSocketFactory(final SslConfiguration sslConf) {
        SSLSocketFactory socketFactory;

        if (sslConf != null) {
            socketFactory = sslConf.getSslSocketFactory();
        } else {
            socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        return socketFactory;
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
                            socketAddress, data.connectTimeoutMillis, data.sslConfiguration, data.socketOptions);
                } catch (IOException ex) {
                    ioe = ex;
                }
            }
            throw new IOException(errorMessage(data, socketAddresses), ioe);
        }
    }

    static Socket createSocket(
            final InetSocketAddress socketAddress,
            final int connectTimeoutMillis,
            final SslConfiguration sslConfiguration,
            final SocketOptions socketOptions)
            throws IOException {
        final SSLSocketFactory socketFactory = createSslSocketFactory(sslConfiguration);
        final SSLSocket socket = (SSLSocket) socketFactory.createSocket();
        if (socketOptions != null) {
            // Not sure which options must be applied before or after the connect() call.
            socketOptions.apply(socket);
        }
        socket.connect(socketAddress, connectTimeoutMillis);
        if (socketOptions != null) {
            // Not sure which options must be applied before or after the connect() call.
            socketOptions.apply(socket);
        }
        return socket;
    }
}
