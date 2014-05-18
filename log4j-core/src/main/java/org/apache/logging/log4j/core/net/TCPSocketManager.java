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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.util.Strings;

/**
 * Manager of TCP Socket connections.
 */
public class TCPSocketManager extends AbstractSocketManager {
    /**
      The default reconnection delay (30000 milliseconds or 30 seconds).
     */
    public static final int DEFAULT_RECONNECTION_DELAY   = 30000;
    /**
      The default port number of remote logging server (4560).
     */
    private static final int DEFAULT_PORT = 4560;

    private static final TCPSocketManagerFactory FACTORY = new TCPSocketManagerFactory();

    private final int reconnectionDelay;

    private Reconnector connector = null;

    private Socket socket;

    private final boolean retry;

    private final boolean immediateFail;

    /**
     * The Constructor.
     * @param name The unique name of this connection.
     * @param os The OutputStream.
     * @param sock The Socket.
     * @param addr The internet address of the host.
     * @param host The name of the host.
     * @param port The port number on the host.
     * @param delay Reconnection interval.
     * @param immediateFail
     * @param layout The Layout.
     */
    public TCPSocketManager(final String name, final OutputStream os, final Socket sock, final InetAddress addr,
                            final String host, final int port, final int delay, final boolean immediateFail,
                            final Layout<? extends Serializable> layout) {
        super(name, os, addr, host, port, layout);
        this.reconnectionDelay = delay;
        this.socket = sock;
        this.immediateFail = immediateFail;
        retry = delay > 0;
        if (sock == null) {
            connector = new Reconnector(this);
            connector.setDaemon(true);
            connector.setPriority(Thread.MIN_PRIORITY);
            connector.start();
        }
    }

    /**
     * Obtain a TCPSocketManager.
     * @param host The host to connect to.
     * @param port The port on the host.
     * @param delay The interval to pause between retries.
     * @return A TCPSocketManager.
     */
    public static TCPSocketManager getSocketManager(final String host, int port, int delay,
                                                    final boolean immediateFail, final Layout<? extends Serializable> layout ) {
        if (Strings.isEmpty(host)) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (delay == 0) {
            delay = DEFAULT_RECONNECTION_DELAY;
        }
        return (TCPSocketManager) getManager("TCP:" + host + ':' + port,
            new FactoryData(host, port, delay, immediateFail, layout), FACTORY);
    }

    @Override
    protected void write(final byte[] bytes, final int offset, final int length)  {
        if (socket == null) {
            if (connector != null && !immediateFail) {
                connector.latch();
            }
            if (socket == null) {
                final String msg = "Error writing to " + getName() + " socket not available";
                throw new AppenderLoggingException(msg);
            }
        }
        synchronized (this) {
            try {
                getOutputStream().write(bytes, offset, length);
            } catch (final IOException ex) {
                if (retry && connector == null) {
                    connector = new Reconnector(this);
                    connector.setDaemon(true);
                    connector.setPriority(Thread.MIN_PRIORITY);
                    connector.start();
                }
                final String msg = "Error writing to " + getName();
                throw new AppenderLoggingException(msg, ex);
            }
        }
    }

    @Override
    protected synchronized void close() {
        super.close();
        if (connector != null) {
            connector.shutdown();
            connector.interrupt();
            connector = null;
        }
    }

    /**
     * TCPSocketManager's content format is specified by:<p/>
     * Key: "protocol" Value: "tcp"<p/>
     * Key: "direction" Value: "out"
     * @return Map of content format keys supporting TCPSocketManager
     */
    @Override
    public Map<String, String> getContentFormat()
    {
        final Map<String, String> result = new HashMap<String, String>(super.getContentFormat());
        result.put("protocol", "tcp");
        result.put("direction", "out");
        return result;
    }

    /**
     * Handles reconnecting to a Thread.
     */
    private class Reconnector extends Thread {

        private final CountDownLatch latch = new CountDownLatch(1);

        private boolean shutdown = false;

        private final Object owner;

        public Reconnector(final OutputStreamManager owner) {
            this.owner = owner;
        }

        public void latch()  {
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
                    final Socket sock = createSocket(address, port);
                    final OutputStream newOS = sock.getOutputStream();
                    synchronized (owner) {
                        try {
                            getOutputStream().close();
                        } catch (final IOException ioe) {
                            // Ignore this.
                        }

                        setOutputStream(newOS);
                        socket = sock;
                        connector = null;
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

    protected Socket createSocket(final InetAddress host, final int port) throws IOException {
        return createSocket(host.getHostName(), port);
    }

    protected Socket createSocket(final String host, final int port) throws IOException {
        return new Socket(host, port);
    }

    /**
     * Data for the factory.
     */
    private static class FactoryData {
        private final String host;
        private final int port;
        private final int delay;
        private final boolean immediateFail;
        private final Layout<? extends Serializable> layout;

        public FactoryData(final String host, final int port, final int delay, final boolean immediateFail,
                           final Layout<? extends Serializable> layout) {
            this.host = host;
            this.port = port;
            this.delay = delay;
            this.immediateFail = immediateFail;
            this.layout = layout;
        }
    }

    /**
     * Factory to create a TCPSocketManager.
     */
    protected static class TCPSocketManagerFactory implements ManagerFactory<TCPSocketManager, FactoryData> {
        @Override
        public TCPSocketManager createManager(final String name, final FactoryData data) {

            InetAddress address;
            OutputStream os;
            try {
                address = InetAddress.getByName(data.host);
            } catch (final UnknownHostException ex) {
                LOGGER.error("Could not find address of " + data.host, ex);
                return null;
            }
            try {
                final Socket socket = new Socket(data.host, data.port);
                os = socket.getOutputStream();
                return new TCPSocketManager(name, os, socket, address, data.host, data.port, data.delay,
                    data.immediateFail, data.layout);
            } catch (final IOException ex) {
                LOGGER.error("TCPSocketManager (" + name + ") " + ex);
                os = new ByteArrayOutputStream();
            }
            if (data.delay == 0) {
                return null;
            }
            return new TCPSocketManager(name, os, null, address, data.host, data.port, data.delay, data.immediateFail,
                data.layout);
        }
    }
}
