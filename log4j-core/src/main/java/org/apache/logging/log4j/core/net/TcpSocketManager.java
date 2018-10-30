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

    private static final TcpSocketManagerFactory<TcpSocketManager, FactoryData> FACTORY = new TcpSocketManagerFactory<>();

    private final int reconnectionDelayMillis;

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
     * @param reconnectionDelayMillis
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
            final int reconnectionDelayMillis, final boolean immediateFail, final Layout<? extends Serializable> layout,
            final int bufferSize) {
        this(name, os, socket, inetAddress, host, port, connectTimeoutMillis, reconnectionDelayMillis, immediateFail,
                layout, bufferSize, null);
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
     * @param reconnectionDelayMillis
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
            final int reconnectionDelayMillis, final boolean immediateFail, final Layout<? extends Serializable> layout,
            final int bufferSize, final SocketOptions socketOptions) {
        super(name, os, inetAddress, host, port, layout, true, bufferSize);
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.reconnectionDelayMillis = reconnectionDelayMillis;
        this.socket = socket;
        this.immediateFail = immediateFail;
        this.retry = reconnectionDelayMillis > 0;
        if (socket == null) {
            this.reconnector = createReconnector();
            this.reconnector.start();
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

    @SuppressWarnings("sync-override") // synchronization on "this" is done within the method
    @Override
    protected void write(final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
        if (socket == null) {
            if (reconnector != null && !immediateFail) {
                reconnector.latch();
            }
            if (socket == null) {
                throw new AppenderLoggingException("Error writing to " + getName() + ": socket not available");
            }
        }
        synchronized (this) {
            try {
                writeAndFlush(bytes, offset, length, immediateFlush);
            } catch (final IOException causeEx) {
                if (retry && reconnector == null) {
                    final String config = inetAddress + ":" + port;
                    reconnector = createReconnector();
                    try {
                        reconnector.reconnect();
                    } catch (final IOException reconnEx) {
                        LOGGER.debug("Cannot reestablish socket connection to {}: {}; starting reconnector thread {}",
                                config, reconnEx.getLocalizedMessage(), reconnector.getName(), reconnEx);
                        reconnector.start();
                        throw new AppenderLoggingException(
                                String.format("Error sending to %s for %s", getName(), config), causeEx);
                    }
                    try {
                        writeAndFlush(bytes, offset, length, immediateFlush);
                    } catch (final IOException e) {
                        throw new AppenderLoggingException(
                                String.format("Error writing to %s after reestablishing connection for %s", getName(),
                                        config),
                                causeEx);
                    }
                }
            }
        }
    }

    private void writeAndFlush(final byte[] bytes, final int offset, final int length, final boolean immediateFlush)
            throws IOException {
        @SuppressWarnings("resource") // outputStream is managed by this class
        final OutputStream outputStream = getOutputStream();
        outputStream.write(bytes, offset, length);
        if (immediateFlush) {
            outputStream.flush();
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
     * Handles reconnecting to a Socket on a Thread.
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
                    sleep(reconnectionDelayMillis);
                    reconnect();
                } catch (final InterruptedException ie) {
                    LOGGER.debug("Reconnection interrupted.");
                } catch (final ConnectException ex) {
                    LOGGER.debug("{}:{} refused connection", host, port);
                } catch (final IOException ioe) {
                    LOGGER.debug("Unable to reconnect to {}:{}", host, port);
                } finally {
                    latch.countDown();
                }
            }
        }

        void reconnect() throws IOException {
            final Socket sock = createSocket(inetAddress.getHostName(), port);
            @SuppressWarnings("resource") // newOS is managed by the enclosing Manager.
            final OutputStream newOS = sock.getOutputStream();
            synchronized (owner) {
                Closer.closeSilently(getOutputStream());
                setOutputStream(newOS);
                socket = sock;
                reconnector = null;
                shutdown = true;
            }
            LOGGER.debug("Connection to {}:{} reestablished: {}", host, port, socket);
        }

        @Override
        public String toString() {
            return "Reconnector [latch=" + latch + ", shutdown=" + shutdown + ", owner=" + owner + "]";
        }
    }

    private Reconnector createReconnector() {
        final Reconnector recon = new Reconnector(this);
        recon.setDaemon(true);
        recon.setPriority(Thread.MIN_PRIORITY);
        return recon;
    }

    protected Socket createSocket(final String host, final int port) throws IOException {
        return createSocket(host, port, socketOptions, connectTimeoutMillis);
    }

    protected static Socket createSocket(final String host, final int port, final SocketOptions socketOptions,
            final int connectTimeoutMillis) throws IOException {
        LOGGER.debug("Creating socket {}:{}", host, port);
        final Socket newSocket = new Socket();
        if (socketOptions != null) {
            // Not sure which options must be applied before or after the connect() call.
            socketOptions.apply(newSocket);
        }
        newSocket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
        if (socketOptions != null) {
            // Not sure which options must be applied before or after the connect() call.
            socketOptions.apply(newSocket);
        }
        return newSocket;
    }

    /**
     * Data for the factory.
     */
    static class FactoryData {
        protected final String host;
        protected final int port;
        protected final int connectTimeoutMillis;
        protected final int reconnectDelayMillis;
        protected final boolean immediateFail;
        protected final Layout<? extends Serializable> layout;
        protected final int bufferSize;
        protected final SocketOptions socketOptions;

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

        @Override
        public String toString() {
            return "FactoryData [host=" + host + ", port=" + port + ", connectTimeoutMillis=" + connectTimeoutMillis
                    + ", reconnectDelayMillis=" + reconnectDelayMillis + ", immediateFail=" + immediateFail
                    + ", layout=" + layout + ", bufferSize=" + bufferSize + ", socketOptions=" + socketOptions + "]";
        }
    }

    /**
     * Factory to create a TcpSocketManager.
     *
     * @param <M>
     *            The manager type.
     * @param <T>
     *            The factory data type.
     */
    protected static class TcpSocketManagerFactory<M extends TcpSocketManager, T extends FactoryData>
            implements ManagerFactory<M, T> {

        @SuppressWarnings("resource")
        @Override
        public M createManager(final String name, final T data) {
            InetAddress inetAddress;
            OutputStream os;
            try {
                inetAddress = InetAddress.getByName(data.host);
            } catch (final UnknownHostException ex) {
                LOGGER.error("Could not find address of {}: {}", data.host, ex, ex);
                return null;
            }
            Socket socket = null;
            try {
                // LOG4J2-1042
                socket = createSocket(data);
                os = socket.getOutputStream();
                return createManager(name, os, socket, inetAddress, data);
            } catch (final IOException ex) {
                LOGGER.error("TcpSocketManager ({}) caught exception and will continue:", name, ex, ex);
                os = NullOutputStream.getInstance();
            }
            if (data.reconnectDelayMillis == 0) {
                Closer.closeSilently(socket);
                return null;
            }
            return createManager(name, os, null, inetAddress, data);
        }

        @SuppressWarnings("unchecked")
        M createManager(final String name, final OutputStream os, final Socket socket, final InetAddress inetAddress, final T data) {
            return (M) new TcpSocketManager(name, os, socket, inetAddress, data.host, data.port,
                    data.connectTimeoutMillis, data.reconnectDelayMillis, data.immediateFail, data.layout,
                    data.bufferSize, data.socketOptions);
        }

        Socket createSocket(final T data) throws IOException {
            return TcpSocketManager.createSocket(data.host, data.port, data.socketOptions, data.connectTimeoutMillis);
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

    public int getReconnectionDelayMillis() {
        return reconnectionDelayMillis;
    }

    @Override
    public String toString() {
        return "TcpSocketManager [reconnectionDelayMillis=" + reconnectionDelayMillis + ", reconnector=" + reconnector
                + ", socket=" + socket + ", socketOptions=" + socketOptions + ", retry=" + retry + ", immediateFail="
                + immediateFail + ", connectTimeoutMillis=" + connectTimeoutMillis + ", inetAddress=" + inetAddress
                + ", host=" + host + ", port=" + port + ", layout=" + layout + ", byteBuffer=" + byteBuffer + ", count="
                + count + "]";
    }

}
