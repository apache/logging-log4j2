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

import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.OutputStreamManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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

    private static ManagerFactory factory = new TCPSocketManagerFactory();

    private final int reconnectionDelay;

    private Reconnector connector = null;

    private Socket socket;

    private final boolean retry;

    /**
     * The Constructor.
     * @param name The unique name of this connection.
     * @param os The OutputStream.
     * @param sock The Socket.
     * @param addr The internet address of the host.
     * @param host The name of the host.
     * @param port The port number on the host.
     * @param delay Reconnection interval.
     */
    public TCPSocketManager(String name, OutputStream os, Socket sock, InetAddress addr, String host, int port,
                            int delay) {
        super(name, os, addr, host, port);
        this.reconnectionDelay = delay;
        this.socket = sock;
        retry = delay > 0;
    }

    /**
     * Obtain a TCPSocketManager.
     * @param host The host to connect to.
     * @param port The port on the host.
     * @param delay The interval to pause between retries.
     * @return A TCPSocketManager.
     */
    public static TCPSocketManager getSocketManager(String host, int port, int delay) {
        if (host == null || host.length() == 0) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (delay == 0) {
            delay = DEFAULT_RECONNECTION_DELAY;
        }
        return (TCPSocketManager) getManager("TCP:" + host + ":" + port, new FactoryData(host, port, delay), factory);
    }

    @Override
    protected synchronized void write(byte[] bytes, int offset, int length)  {
        try {
            getOutputStream().write(bytes, offset, length);
            socket.setSendBufferSize(length);
        } catch (IOException ex) {
            if (retry && connector == null) {
                connector = new Reconnector(this);
                connector.setDaemon(true);
                connector.setPriority(Thread.MIN_PRIORITY);
                connector.start();
            }
            String msg = "Error writing to " + getName();
            throw new AppenderRuntimeException(msg, ex);
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
     * Handles recoonecting to a Thread.
     */
    private class Reconnector extends Thread {

        private boolean shutdown = false;

        private final Object owner;

        public Reconnector(OutputStreamManager owner) {
            this.owner = owner;
        }

        public void shutdown() {
            shutdown = true;
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    sleep(reconnectionDelay);
                    Socket sock = new Socket(address, port);
                    OutputStream newOS = sock.getOutputStream();
                    synchronized (owner) {
                        try {
                            getOutputStream().close();
                        } catch (IOException ioe) {
                            // Ignore this.
                        }

                        setOutputStream(newOS);
                        socket = sock;
                        connector = null;
                    }
                    LOGGER.debug("Connection to " + host + ":" + port + " reestablished.");
                } catch (InterruptedException ie) {
                    LOGGER.debug("Reconnection interrupted.");
                } catch (ConnectException ex) {
                    LOGGER.debug(host + ":" + port + " refused connection");
                } catch (IOException ioe) {
                    LOGGER.debug("Unable to reconnect to " + host + ":" + port);
                }
            }
        }
    }

    /**
     * Data for the factory.
     */
    private static class FactoryData {
        private String host;
        private int port;
        private int delay;

        public FactoryData(String host, int port, int delay) {
            this.host = host;
            this.port = port;
            this.delay = delay;
        }
    }

    /**
     * Factory to create a TCPSocketManager.
     */
    private static class TCPSocketManagerFactory implements ManagerFactory<TCPSocketManager, FactoryData> {

        public TCPSocketManager createManager(String name, FactoryData data) {
            try {
                InetAddress address = InetAddress.getByName(data.host);
                Socket socket = new Socket(data.host, data.port);
                OutputStream os = socket.getOutputStream();
                return new TCPSocketManager(name, os, socket, address, data.host, data.port, data.delay);
            } catch (UnknownHostException ex) {
                LOGGER.error("Could not find address of " + data.host, ex);
            } catch (IOException ex) {
                LOGGER.error("TCPSocketManager (" + name + ") " + ex);
            }
            return null;
        }
    }
}
