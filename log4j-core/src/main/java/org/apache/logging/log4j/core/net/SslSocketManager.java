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

import java.io.ByteArrayOutputStream;
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

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public class SslSocketManager extends AbstractSocketManager {
    public static final int DEFAULT_RECONNECTION_DELAY_MILLIS = 30000;
    public static final int DEFAULT_PORT = 6514;
    private static final SslSocketManagerFactory FACTORY = new SslSocketManagerFactory();
    private final SslConfiguration sslConfig;
    private final int reconnectionDelayMillis;
    private final int connectTimeoutMillis;
    private final boolean immediateFail;
    private final boolean retry;
    private final SocketOptions socketOptions;
    private Socket socket;
    private SSLReconnector reconnector;

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
    * @param immediateFail
    * @param layout        The Layout.
    * @param bufferSize The buffer size.
    * @deprecated Use {@link #SslSocketManager(String, OutputStream, Socket, SslConfiguration, InetAddress, String, int, int, int, boolean, Layout, int, SocketOptions)}.
    */
   @Deprecated
   public SslSocketManager(final String name, final OutputStream os, final Socket sock,
           final SslConfiguration sslConfig, final InetAddress inetAddress, final String host, final int port,
           final int connectTimeoutMillis, final int reconnectionDelayMillis, final boolean immediateFail,
           final Layout<? extends Serializable> layout, final int bufferSize) {
       this(name, os, sock, sslConfig, inetAddress, host, port, connectTimeoutMillis, reconnectionDelayMillis, immediateFail, layout, bufferSize, null);
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
   * @param immediateFail
   * @param layout        The Layout.
   * @param bufferSize The buffer size.
   */
  public SslSocketManager(final String name, final OutputStream os, final Socket sock,
          final SslConfiguration sslConfig, final InetAddress inetAddress, final String host, final int port,
          final int connectTimeoutMillis, final int reconnectionDelayMillis, final boolean immediateFail,
          final Layout<? extends Serializable> layout, final int bufferSize, final SocketOptions socketOptions) {
      super(name, os, inetAddress, host, port, layout, true, bufferSize);
      this.sslConfig = sslConfig;
      this.socket = sock;
      this.reconnectionDelayMillis = reconnectionDelayMillis;
      this.connectTimeoutMillis = connectTimeoutMillis;
      this.immediateFail = immediateFail;
      this.retry = reconnectionDelayMillis > 0;
      if (socket == null) {
          this.reconnector = createReconnector();
          this.reconnector.start();
      }
      this.socketOptions = socketOptions;
  }

    private static class SslFactoryData extends FactoryData {
        protected SslConfiguration sslConfiguration;

        public SslFactoryData(final SslConfiguration sslConfiguration, final String host, final int port,
                final int connectTimeoutMillis, final int reconnectDelayMillis, final int delayMillis,
                final boolean immediateFail, final Layout<? extends Serializable> layout, final int bufferSize,
                final SocketOptions socketOptions) {
            super(host, port, connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout, bufferSize,
                    socketOptions);
            this.sslConfiguration = sslConfiguration;
        }
    }

    /**
     * @deprecated Use {@link SslSocketManager#getSocketManager(SslConfiguration, String, int, int, int, boolean, Layout, int, SocketOptions)}.
     */
    @Deprecated
    public static SslSocketManager getSocketManager(final SslConfiguration sslConfig, final String host, final int port,
            final int connectTimeoutMillis, final int reconnectDelayMillis, final boolean immediateFail,
            final Layout<? extends Serializable> layout, final int bufferSize) {
        return getSocketManager(sslConfig, host, port, connectTimeoutMillis, reconnectDelayMillis, immediateFail, layout, bufferSize, null);
    }

    public static SslSocketManager getSocketManager(final SslConfiguration sslConfig, final String host, int port,
            final int connectTimeoutMillis, int reconnectDelayMillis, final boolean immediateFail,
            final Layout<? extends Serializable> layout, final int bufferSize, final SocketOptions socketOptions) {
        if (Strings.isEmpty(host)) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (reconnectDelayMillis == 0) {
            reconnectDelayMillis = DEFAULT_RECONNECTION_DELAY_MILLIS;
        }
        return (SslSocketManager) getManager("TLS:" + host + ':' + port, new SslFactoryData(sslConfig, host, port,
                connectTimeoutMillis, 0, reconnectDelayMillis, immediateFail, layout, bufferSize, socketOptions), FACTORY);
    }

    private Socket createSocket(final String host, final int port) throws IOException {
        final SSLSocketFactory socketFactory = createSslSocketFactory(sslConfig);
        final InetSocketAddress address = new InetSocketAddress(host, port);
        final Socket newSocket = socketFactory.createSocket();
        newSocket.connect(address, getConnectTimeoutMillis());
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


    private static class SslSocketManagerFactory implements ManagerFactory<SslSocketManager, SslFactoryData> {

        private static class TlsSocketManagerFactoryException extends Exception {

            private static final long serialVersionUID = 1L;
        }

        @SuppressWarnings("resource")
        @Override
        public SslSocketManager createManager(final String name, final SslFactoryData data) {
            InetAddress inetAddress = null;
            OutputStream os = null;
            Socket socket = null;
            try {
                inetAddress = resolveAddress(data.host);
                socket = createSocket(data);
                os = socket.getOutputStream();
                checkDelay(data.reconnectDelayMillis, os);
            } catch (final IOException e) {
                LOGGER.error("SslSocketManager ({})", name, e);
                os = new ByteArrayOutputStream();
            } catch (final TlsSocketManagerFactoryException e) {
                LOGGER.catching(Level.DEBUG, e);
                Closer.closeSilently(socket);
                return null;
            }
            return new SslSocketManager(name, os, socket, data.sslConfiguration, inetAddress, data.host, data.port,
                    data.connectTimeoutMillis, data.reconnectDelayMillis, data.immediateFail, data.layout, data.bufferSize,
                    data.socketOptions);
        }

        private InetAddress resolveAddress(final String hostName) throws TlsSocketManagerFactoryException {
            InetAddress address;

            try {
                address = InetAddress.getByName(hostName);
            } catch (final UnknownHostException ex) {
                LOGGER.error("Could not find address of {}", hostName, ex);
                throw new TlsSocketManagerFactoryException();
            }

            return address;
        }

        private void checkDelay(final int delay, final OutputStream os) throws TlsSocketManagerFactoryException {
            if (delay == 0 && os == null) {
                throw new TlsSocketManagerFactoryException();
            }
        }

        private Socket createSocket(final SslFactoryData data) throws IOException {
            SSLSocketFactory socketFactory;
            SSLSocket socket;

            socketFactory = createSslSocketFactory(data.sslConfiguration);
            socket = (SSLSocket) socketFactory.createSocket();
            final SocketOptions socketOptions = data.socketOptions;
            if (socketOptions != null) {
                // Not sure which options must be applied before or after the connect() call.
                socketOptions.apply(socket);
            }
            socket.connect(new InetSocketAddress(data.host, data.port), data.connectTimeoutMillis);
            if (socketOptions != null) {
                // Not sure which options must be applied before or after the connect() call.
                socketOptions.apply(socket);
            }
            return socket;
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
     * Gets this SslSocketManager's content format. Specified by:
     * <ul>
     * <li>Key: "protocol" Value: "ssl"</li>
     * <li>Key: "direction" Value: "out"</li>
     * </ul>
     * 
     * @return Map of content format keys supporting TcpSocketManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(super.getContentFormat());
        result.put("protocol", "ssl");
        result.put("direction", "out");
        return result;
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
                    } catch (IOException reconnEx) {
                        LOGGER.debug("Cannot reestablish socket connection to {}: {}; starting reconnector thread {}",
                                config, reconnEx.getLocalizedMessage(), reconnector.getName(), reconnEx);
                        reconnector.start();
                        throw new AppenderLoggingException(
                                String.format("Error sending to %s for %s", getName(), config), causeEx);
                    }
                    try {
                        writeAndFlush(bytes, offset, length, immediateFlush);
                    } catch (IOException e) {
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

    private SSLReconnector createReconnector() {
        SSLReconnector recon = new SSLReconnector(this);
        recon.setDaemon(true);
        recon.setPriority(Thread.MIN_PRIORITY);
        return recon;
    }

    /**
     * Handles reconnecting to a Socket on a Thread.
     */
    private class SSLReconnector extends Log4jThread {
        private final CountDownLatch latch = new CountDownLatch(1);
        private boolean shutdown = false;
        private final Object owner;

        public SSLReconnector(final OutputStreamManager owner) {
            super("SslSocketManager-Reconnector");
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
                shutdown();
            }
            LOGGER.debug("Connection to {}:{} reestablished: {}", host, port, socket);
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
