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
package org.apache.logging.log4j.core.test.net.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MockTcpSyslogServer extends MockSyslogServer {
    private final ServerSocket serverSocket;
    private volatile boolean shutdown = false;
    private Thread thread;

    public MockTcpSyslogServer(final int numberOfMessagesToReceive, final int port) throws IOException {
        this(port);
    }

    public MockTcpSyslogServer() throws IOException {
        this(0);
    }

    private MockTcpSyslogServer(final int port) throws IOException {
        super(0, port);
        serverSocket = new ServerSocket(port);
    }

    @Override
    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        try {
            if (serverSocket != null) {
                try {
                    this.serverSocket.close();
                } catch (final Exception e) {
                    LOGGER.error("The {} failed to close its socket.", getName(), e);
                }
            }
            this.interrupt();
        } catch (final SecurityException e) {
            LOGGER.error("Shutdown of {} failed", getName(), e);
        }
        if (thread != null) {
            try {
                thread.join(100);
            } catch (final InterruptedException e) {
                LOGGER.error("Shutdown of {} thread failed.", getName(), e);
            }
        }
    }

    @Override
    public void run() {
        LOGGER.info("{} started on port {}.", getName(), getLocalPort());
        this.thread = Thread.currentThread();
        while (!shutdown) {
            try {
                final byte[] buffer = new byte[4096];
                try (final Socket socket = serverSocket.accept()) {
                    socket.setSoLinger(true, 0);
                    final InputStream in = socket.getInputStream();
                    int i = in.read(buffer, 0, buffer.length);
                    while (i != -1) {
                        if (i < buffer.length) {
                            final String line = new String(buffer, 0, i);
                            messageList.add(line);
                            i = in.read(buffer, 0, buffer.length);
                        } else if (i == 0) {
                            LOGGER.warn("{} received no data.", getName());
                        } else {
                            LOGGER.warn("{} received a message longer than {}.", getName(), buffer.length);
                        }
                    }
                }
            } catch (final Exception e) {
                if (!shutdown) {
                    LOGGER.error("{} caught an exception.", getName(), e);
                }
            }
        }
        LOGGER.info("{} stopped.", getName());
    }
}
