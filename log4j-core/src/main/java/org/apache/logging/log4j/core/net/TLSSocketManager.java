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
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.net.ssl.SSLConfiguration;
import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public class TLSSocketManager extends TCPSocketManager {
    public static final int DEFAULT_PORT = 6514;
    private static final TLSSocketManagerFactory FACTORY = new TLSSocketManagerFactory();
    private final SSLConfiguration sslConfig;

    /**
     *
     *
     * @param name          The unique name of this connection.
     * @param os            The OutputStream.
     * @param sock          The Socket.
     * @param addr          The internet address of the host.
     * @param host          The name of the host.
     * @param port          The port number on the host.
     * @param delay         Reconnection interval.
     * @param immediateFail
     * @param layout        The Layout.
     */
    public TLSSocketManager(final String name, final OutputStream os, final Socket sock, final SSLConfiguration sslConfig, final InetAddress addr,
                            final String host, final int port, final int delay, final boolean immediateFail, final Layout<? extends Serializable> layout) {
        super(name, os, sock, addr, host, port, delay, immediateFail, layout);
        this.sslConfig = sslConfig;
    }

    private static class TLSFactoryData {
        protected SSLConfiguration sslConfig;
        private final String host;
        private final int port;
        private final int delay;
        private final boolean immediateFail;
        private final Layout<? extends Serializable> layout;

        public TLSFactoryData(final SSLConfiguration sslConfig, final String host, final int port, final int delay, final boolean immediateFail,
                              final Layout<? extends Serializable> layout) {
            this.host = host;
            this.port = port;
            this.delay = delay;
            this.immediateFail = immediateFail;
            this.layout = layout;
            this.sslConfig = sslConfig;
        }
    }

    public static TLSSocketManager getSocketManager(final SSLConfiguration sslConfig, final String host, int port,
                                                    int delay, final boolean immediateFail, final Layout<? extends Serializable> layout ) {
        if (Strings.isEmpty(host)) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (delay == 0) {
            delay = DEFAULT_RECONNECTION_DELAY;
        }
        return (TLSSocketManager) getManager("TLS:" + host + ':' + port,
                new TLSFactoryData(sslConfig, host, port, delay, immediateFail, layout), FACTORY);
    }

    @Override
    protected Socket createSocket(final String host, final int port) throws IOException {
        final SSLSocketFactory socketFactory = createSSLSocketFactory(sslConfig);
        return socketFactory.createSocket(host, port);
    }

    private static SSLSocketFactory createSSLSocketFactory(final SSLConfiguration sslConf) {
        SSLSocketFactory socketFactory;

        if (sslConf != null) {
            socketFactory = sslConf.getSSLSocketFactory();
        } else {
            socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }

        return socketFactory;
    }


    private static class TLSSocketManagerFactory implements ManagerFactory<TLSSocketManager, TLSFactoryData> {

        private class TLSSocketManagerFactoryException extends Exception {

            private static final long serialVersionUID = 1L;
        }

        @Override
        public TLSSocketManager createManager(final String name, final TLSFactoryData data) {
            InetAddress address = null;
            OutputStream os = null;
            Socket socket = null;

            try {
                address = resolveAddress(data.host);
                socket = createSocket(data);
                os = socket.getOutputStream();
                checkDelay(data.delay, os);
            }
            catch (final IOException e) {
                LOGGER.error("TLSSocketManager ({})", name, e);
                os = new ByteArrayOutputStream();
            }
            catch (final TLSSocketManagerFactoryException e) {
                LOGGER.catching(Level.DEBUG, e);
                return null;
            }
            return createManager(name, os, socket, data.sslConfig, address, data.host, data.port, data.delay, data.immediateFail, data.layout);
        }

        private InetAddress resolveAddress(final String hostName) throws TLSSocketManagerFactoryException {
            InetAddress address;

            try {
                address = InetAddress.getByName(hostName);
            } catch (final UnknownHostException ex) {
                LOGGER.error("Could not find address of {}", hostName, ex);
                throw new TLSSocketManagerFactoryException();
            }

            return address;
        }

        private void checkDelay(final int delay, final OutputStream os) throws TLSSocketManagerFactoryException {
            if (delay == 0 && os == null) {
                throw new TLSSocketManagerFactoryException();
            }
        }

        private Socket createSocket(final TLSFactoryData data) throws IOException {
            SSLSocketFactory socketFactory;
            SSLSocket socket;

            socketFactory = createSSLSocketFactory(data.sslConfig);
            socket = (SSLSocket) socketFactory.createSocket(data.host, data.port);
            return socket;
        }

        private TLSSocketManager createManager(final String name, final OutputStream os, final Socket socket, final SSLConfiguration sslConfig, final InetAddress address, final String host, final int port, final int delay, final boolean immediateFail, final Layout<? extends Serializable> layout) {
            return new TLSSocketManager(name, os, socket, sslConfig, address, host, port, delay, immediateFail, layout);
        }
    }
}
