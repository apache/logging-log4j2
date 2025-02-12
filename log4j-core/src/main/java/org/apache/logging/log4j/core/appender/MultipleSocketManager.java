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

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Log4jThread;

public class MultipleSocketManager extends AbstractManager {
    public static final int DEFAULT_RECONNECTION_DELAY_MILLIS = 30000;

    private final Random random = new Random();

    private final Configuration configuration;

    private String serverListString;

    private String[] serverList;

    private String[] serverAddressList;

    private Integer[] serverPortList;

    private SocketOptions socketOptions;

    private Socket[] socketList;

    private final int connectTimeoutMillis;

    private final int reconnectionDelayMillis;

    /**
     * for future versions
     */
    private boolean immediateFlush;

    private boolean immediateFail;

    private ConnectThread connectThread;

    public MultipleSocketManager(
            final LoggerContext loggerContext,
            final String name,
            final Configuration configuration,
            final String serverListString,
            final SocketOptions socketOptions,
            final int connectTimeoutMillis,
            final int reconnectionDelayMillis,
            final boolean immediateFlush,
            final boolean immediateFail) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        this.serverListString = Objects.requireNonNull(serverListString);
        try {
            this.serverList = serverListString.split(",");
            this.serverAddressList = new String[this.serverList.length];
            this.serverPortList = new Integer[this.serverList.length];
            for (int i = 0; i < this.serverList.length; ++i) {
                String[] server = this.serverList[i].split(":");
                this.serverAddressList[i] = server[0];
                this.serverPortList[i] = Integer.valueOf(server[1]);
            }
        } catch (Exception e) {
            LOGGER.error("parse server list failed. {}", serverListString, e);
            throw new RuntimeException(e);
        }
        this.socketOptions = socketOptions;
        this.socketList = new Socket[this.serverList.length];
        this.connectTimeoutMillis = connectTimeoutMillis;
        if (reconnectionDelayMillis > 0) {
            this.reconnectionDelayMillis = reconnectionDelayMillis;
        } else {
            this.reconnectionDelayMillis = DEFAULT_RECONNECTION_DELAY_MILLIS;
        }
        this.immediateFlush = immediateFlush;
        this.immediateFail = immediateFail;
        this.connectThread = new ConnectThread(this);
        this.connectThread.setDaemon(true);
        this.connectThread.setPriority(Thread.MIN_PRIORITY);
        try {
            this.connectThread.connect();
        } catch (Exception e) {
            LOGGER.error("connectThread.connect failed. {}", serverListString, e);
        }
        this.connectThread.start();
    }

    public void send(final Layout layout, final LogEvent event) throws Exception {
        final byte[] byteArray = layout.toByteArray(event);
        for (int i = 0; i < 2; ++i) {
            // select socket randomly
            final int index = random.nextInt(serverList.length);
            synchronized (serverList[index]) {
                if (socketList[index] == null && !immediateFail) {
                    continue;
                }
                try {
                    final OutputStream outputStream = socketList[index].getOutputStream();
                    outputStream.write(byteArray);
                    // TODO
                    // immediateFlush
                    outputStream.flush();
                    break;
                } catch (final Exception e) {
                    LOGGER.error("outputStream.write failed. {} {} {}", index, socketList[index], i, e);
                    if (!immediateFail) {
                        continue;
                    }
                    throw e;
                }
            }
        }
    }

    @Override
    public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        if (connectThread != null) {
            connectThread.shutdown();
            connectThread.interrupt();
            connectThread = null;
        }
        boolean closed = true;
        for (int i = 0; i < serverList.length; ++i) {
            synchronized (serverList[i]) {
                if (socketList[i] != null) {
                    LOGGER.debug("closing socket {} {} {}", i, serverList[i], socketList[i]);
                    try {
                        socketList[i].close();
                    } catch (final Exception e) {
                        LOGGER.error("socket.close failed. {} {}", i, socketList[i], e);
                        closed = false;
                    }
                    socketList[i] = null;
                }
            }
        }
        return closed;
    }

    /**
     * Handles reconnecting to servers on a Thread.
     */
    private class ConnectThread extends Log4jThread {
        private final Random random = new Random();

        private boolean shutdown = false;

        private final Object owner;

        public ConnectThread(final MultipleSocketManager owner) {
            super("MultipleSocketManager-ConnectThread");
            this.owner = owner;
        }

        public void shutdown() {
            shutdown = true;
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    sleep(reconnectionDelayMillis + random.nextInt(reconnectionDelayMillis + 1));
                    connect();
                } catch (final InterruptedException e) {
                    LOGGER.debug("Reconnection interrupted.", e);
                } catch (final Exception e) {
                    LOGGER.debug("Unable to connect to {}", serverListString, e);
                }
            }
        }

        void connect() throws Exception {
            int successCount = 0;
            for (int i = 0; i < serverList.length; ++i) {
                LOGGER.debug("resolving server {} {}", i, serverList[i]);
                List<InetSocketAddress> inetSocketAddressList = null;
                try {
                    inetSocketAddressList = resolveServer(serverAddressList[i], serverPortList[i]);
                } catch (Exception e) {
                    LOGGER.error("could not resolve server. {} {}", i, serverList[i], e);
                    continue;
                }
                for (InetSocketAddress inetSocketAddress : inetSocketAddressList) {
                    LOGGER.debug("creating socket {} {} {}", i, serverList[i], inetSocketAddress);
                    Socket newSocket = null;
                    try {
                        newSocket = createSocket(inetSocketAddress, socketOptions, connectTimeoutMillis, i);
                    } catch (Exception e) {
                        LOGGER.error("createSocket failed. {} {} {}", i, serverList[i], inetSocketAddress, e);
                        Closer.closeSilently(newSocket);
                        continue;
                    }
                    Socket oldSocket = null;
                    synchronized (serverList[i]) {
                        oldSocket = socketList[i];
                        socketList[i] = newSocket;
                    }
                    if (oldSocket != null) {
                        LOGGER.debug("closing socket {} {} {}", i, serverList[i], oldSocket);
                        try {
                            oldSocket.close();
                        } catch (final Exception e) {
                            LOGGER.error("socket.close failed. {} {} {}", i, serverList[i], oldSocket, e);
                        }
                    }
                    ++successCount;
                    break;
                }
            }
            if (successCount < 1) {
                throw new Exception("connect failed." + successCount + "/" + serverList.length);
            }
        }

        @Override
        public String toString() {
            return "ConnectThread [shutdown=" + shutdown + "]";
        }
    }

    public List<InetSocketAddress> resolveServer(final String address, final int port) throws Exception {
        final InetAddress[] inetAddressList = InetAddress.getAllByName(address);
        final List<InetSocketAddress> inetSocketAddressList = new ArrayList<>(inetAddressList.length);
        for (InetAddress inetAddress : inetAddressList) {
            inetSocketAddressList.add(new InetSocketAddress(inetAddress, port));
        }
        return inetSocketAddressList;
    }

    protected static Socket createSocket(
            final InetSocketAddress inetSocketAddress,
            final SocketOptions socketOptions,
            final int connectTimeoutMillis,
            final int index)
            throws Exception {
        final Socket socket = new Socket();
        if (socketOptions != null) {
            // Not sure which options must be applied before or after the connect() call.
            socketOptions.apply(socket);
        }
        LOGGER.debug("connecting socket {} {} {}", index, inetSocketAddress.toString(), connectTimeoutMillis);
        socket.connect(inetSocketAddress, connectTimeoutMillis);
        if (socketOptions != null) {
            // Not sure which options must be applied before or after the connect() call.
            socketOptions.apply(socket);
        }
        return socket;
    }
}
