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
package org.apache.logging.log4j.core.net;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.util.Strings;

/**
 * Manager of TCP Socket connections.
 */
public class TcpSocketManager extends AbstractSocketManager {
    /**
     * The default reconnection delay (30000 milliseconds or 30 seconds).
     */
    public static final int DEFAULT_RECONNECTION_DELAY_MILLIS = 30000;
    /**
     * The default port number of remote logging server (4560).
     */
    private static final int DEFAULT_PORT = 4560;

    private static final TcpSocketManagerFactory FACTORY = new TcpSocketManagerFactory();

    private final int reconnectionDelay;

    private Reconnector reconnector;

    private Socket socket;

    private final SocketOptions socketOptions;

    private final boolean retry;

    private final boolean immediateFail;

    private final int connectTimeoutMillis;

    /**
     * Constructs.
     * 
     * @param name
     *            The unique name of this connection.
     * @param os
     *            The OutputStream.
     * @param socket
     *            The Socket.
     * @param inetAddress
     *            The Internet address of the host.
     * @param host
     *            The name of the host.
     * @param port
     *            The port number on the host.
     * @param connectTimeoutMillis
     *            the connect timeout in milliseconds.
     * @param delay
     *            Reconnection interval.
     * @param immediateFail
     *            True if the write should fail if no socket is immediately available.
     * @param layout
     *            The Layout.
     * @param bufferSize
     *            The buffer size.
     * @deprecated Use
     *             {@link TcpSocketManager#TcpSocketManager(String, OutputStream, Socket, InetAddress, String, int, int, int, boolean, Layout, int, SocketOptions)}.
     */
    @Deprecated
    public TcpSocketManager(final String name, final OutputStream os, final Socket socket,
            final InetAddress inetAddress, final String host, final int port, final int connectTimeoutMillis,
            final int delay, final boolean immediateFail, final Layout<? extends Serializable> layout,
            final int bufferSize) {
        this(name, os, socket, inetAddress, host, port, connectTimeoutMillis, delay, immediateFail, layout, bufferSize,
                null);
    }

    /**
     * Constructs.
     * 
     * @param name
     *            The unique name of this connection.
     * @param os
     *            The OutputStream.
     * @param socket
     *            The Socket.
     * @param inetAddress
     *            The Internet address of the host.
     * @param host
     *            The name of the host.
     * @param port
     *            The port number on the host.
     * @param connectTimeoutMillis
     *            the connect timeout in milliseconds.
     * @param delay
     *            Reconnection interval.
     * @param immediateFail
     *            True if the write should fail if no socket is immediately available.
     * @param layout
     *            The Layout.
     * @param bufferSize
     *            The buffer size.
     */
    public TcpSocketManager(final String name, final OutputStream os, final Socket socket,
            final InetAddress inetAddress, final String host, final int port, final int connectTimeoutMillis,
            final int delay, final boolean immediateFail, final Layout<? extends Serializable> layout,
            final int bufferSize, final SocketOptions socketOptions) {
        super(name, os, inetAddress, host, port, layout, true, bufferSize);
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.reconnectionDelay = delay;
        this.socket = socket;
        this.immediateFail = immediateFail;
        retry = delay > 0;
        if (socket == null) {
            reconnector = createReconnector();
            reconnector.start();
        }
        this.socketOptions = socketOptions;
    }

    /**
     * Obtains a TcpSocketManager.
     * 
     * @param host
     *            The host to connect to.
     * @param port
     *            The port on the host.
     * @param connectTimeoutMillis
     *            the connect timeout in milliseconds
     * @param reconnectDelayMillis
     *            The interval to pause between retries.
     * @param bufferSize
     *            The buffer size.
     * @return A TcpSocketManager.
     * @deprecated Use {@link #getSocketManager(String, int, int, int, boolean, Layout, int, SocketOptions)}.
     */
    @Deprecated
    public static TcpSocketManager getSocketManager(final String host, final int port, final int connectTimeoutMillis,
            final int reconnectDelayMillis, final boolean immediateFail, final Layout<? extends Serializable> layout,
            final int bufferSize) {
        return getSocketManager(host, port, connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout,
                bufferSize, null);
    }

    /**
     * Obtains a TcpSocketManager.
     * 
     * @param host
     *            The host to connect to.
     * @param port
     *            The port on the host.
     * @param connectTimeoutMillis
     *            the connect timeout in milliseconds
     * @param reconnectDelayMillis
     *            The interval to pause between retries.
     * @param bufferSize
     *            The buffer size.
     * @return A TcpSocketManager.
     */
    public static TcpSocketManager getSocketManager(final String host, int port, final int connectTimeoutMillis,
            int reconnectDelayMillis, final boolean immediateFail, final Layout<? extends Serializable> layout,
            final int bufferSize, final SocketOptions socketOptions) {
        if (Strings.isEmpty(host)) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (reconnectDelayMillis == 0) {
            reconnectDelayMillis = DEFAULT_RECONNECTION_DELAY_MILLIS;
        }
        return (TcpSocketManager) getManager("TCP:" + host + ':' + port, new FactoryData(host, port,
                connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout, bufferSize, socketOptions), FACTORY);
    }

    @Override
    protected void write(final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
        if (socket == null) {
            if (reconnector != null && !immediateFail) {
                reconnector.latch();
            }
            if (socket == null) {
                final String msg = "Error writing to " + getName() + " socket not available";
                throw new AppenderLoggingException(msg);
            }
        }
        synchronized (this) {
            try {
                final OutputStream outputStream = getOutputStream();
                outputStream.write(bytes, offset, length);
                if (immediateFlush) {
                    outputStream.flush();
                }
            } catch (final IOException ex) {
                if (retry && reconnector == null) {
                    reconnector = createReconnector();
                    reconnector.start();
                }
                final String msg = "Error writing to " + getName();
                throw new AppenderLoggingException(msg, ex);
            }
        }
    }

    @Override
    protected synchronized boolean closeOutputStream() {
        final boolean closed = super.closeOutputStream();
        if (reconnector != null) {
            reconnector.shutdown();
            reconnector.interrupt();
            reconnector = null;
        }
        final Socket oldSocket = socket;
        socket = null;
        if (oldSocket != null) {
            try {
                oldSocket.close();
            } catch (final IOException e) {
                LOGGER.error("Could not close socket {}", socket);
                return false;
            }
        }
        return closed;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /**
     * Gets this TcpSocketManager's content format. Specified by:
     * <ul>
     * <li>Key: "protocol" Value: "tcp"</li>
     * <li>Key: "direction" Value: "out"</li>
     * </ul>
     * 
     * @return Map of content format keys supporting TcpSocketManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(super.getContentFormat());
        result.put("protocol", "tcp");
        result.put("direction", "out");
        return result;
    }

    /**
     * Handles reconnecting to a Thread.
     */
    private class Reconnector extends Log4jThread {

        private final CountDownLatch latch = new CountDownLatch(1);

        private boolean shutdown = false;

        private final Object owner;

        public Reconnector(final OutputStreamManager owner) {
            super("TcpSocketManager-Reconnector");
            this.owner = owner;
        }

        public void latch() {
            try {
                latch.await();
            } catch (final InterruptedException ex) {
                // Ignore the exception.
            }
        }

        public void shutdown() {
            shutdown = true;
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    sleep(reconnectionDelay);
                    final Socket sock = createSocket(inetAddress, port);
                    final OutputStream newOS = sock.getOutputStream();
                    synchronized (owner) {
                        try {
                            getOutputStream().close();
                        } catch (final IOException ioe) {
                            // Ignore this.
                        }

                        setOutputStream(newOS);
                        socket = sock;
                        reconnector = null;
                        shutdown = true;
                    }
                    LOGGER.debug("Connection to " + host + ':' + port + " reestablished.");
                } catch (final InterruptedException ie) {
                    LOGGER.debug("Reconnection interrupted.");
                } catch (final ConnectException ex) {
                    LOGGER.debug(host + ':' + port + " refused connection");
                } catch (final IOException ioe) {
                    LOGGER.debug("Unable to reconnect to " + host + ':' + port);
                } finally {
                    latch.countDown();
                }
            }
        }
    }

    private Reconnector createReconnector() {
        final Reconnector recon = new Reconnector(this);
        recon.setDaemon(true);
        recon.setPriority(Thread.MIN_PRIORITY);
        return recon;
    }

    protected Socket createSocket(final InetAddress host, final int port) throws IOException {
        return createSocket(host.getHostName(), port);
    }

    protected Socket createSocket(final String host, final int port) throws IOException {
        final Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
        if (socketOptions != null) {
            socketOptions.apply(newSocket);
        }
        return newSocket;
    }

    /**
     * Data for the factory.
     */
    private static class FactoryData {
        private final String host;
        private final int port;
        private final int connectTimeoutMillis;
        private final int reconnectDelayMillis;
        private final boolean immediateFail;
        private final Layout<? extends Serializable> layout;
        private final int bufferSize;
        private final SocketOptions socketOptions;

        public FactoryData(final String host, final int port, final int connectTimeoutMillis,
                final int reconnectDelayMillis, final boolean immediateFail,
                final Layout<? extends Serializable> layout, final int bufferSize, final SocketOptions socketOptions) {
            this.host = host;
            this.port = port;
            this.connectTimeoutMillis = connectTimeoutMillis;
            this.reconnectDelayMillis = reconnectDelayMillis;
            this.immediateFail = immediateFail;
            this.layout = layout;
            this.bufferSize = bufferSize;
            this.socketOptions = socketOptions;
        }
    }

    /**
     * Factory to create a TcpSocketManager.
     */
    protected static class TcpSocketManagerFactory implements ManagerFactory<TcpSocketManager, FactoryData> {
        
        @SuppressWarnings("resource")
        @Override
        public TcpSocketManager createManager(final String name, final FactoryData data) {

            InetAddress inetAddress;
            OutputStream os;
            try {
                inetAddress = InetAddress.getByName(data.host);
            } catch (final UnknownHostException ex) {
                LOGGER.error("Could not find address of " + data.host, ex, ex);
                return null;
            }
            Socket socket = null;
            try {
                // LOG4J2-1042
                socket = createSocket(data);
                os = socket.getOutputStream();
                return new TcpSocketManager(name, os, socket, inetAddress, data.host, data.port,
                        data.connectTimeoutMillis, data.reconnectDelayMillis, data.immediateFail, data.layout,
                        data.bufferSize, data.socketOptions);
            } catch (final IOException ex) {
                LOGGER.error("TcpSocketManager (" + name + ") " + ex, ex);
                os = NullOutputStream.getInstance();
            }
            if (data.reconnectDelayMillis == 0) {
                Closer.closeSilently(socket);
                return null;
            }
            return new TcpSocketManager(name, os, null, inetAddress, data.host, data.port, data.connectTimeoutMillis,
                    data.reconnectDelayMillis, data.immediateFail, data.layout, data.bufferSize, data.socketOptions);
        }

        static Socket createSocket(final FactoryData data) throws IOException, SocketException {
            final Socket socket = new Socket();
            socket.connect(new InetSocketAddress(data.host, data.port), data.connectTimeoutMillis);
            final SocketOptions socketOptions = data.socketOptions;
            if (socketOptions != null) {
                socketOptions.apply(socket);
            }
            return socket;
        }

    }

    /**
     * USE AT YOUR OWN RISK, method is public for testing purpose only for now.
     */
    public SocketOptions getSocketOptions() {
        return socketOptions;
    }

    /**
     * USE AT YOUR OWN RISK, method is public for testing purpose only for now.
     */
    public Socket getSocket() {
        return socket;
    }

}
