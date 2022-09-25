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
package org.apache.logging.log4j.core.net.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class MockTcpSyslogServer extends MockSyslogServer {
    private final ServerSocket socketServer;
    private volatile boolean shutdown = false;
    private Thread thread;

    public MockTcpSyslogServer(final int numberOfMessagesToReceive, final int port) throws IOException {
        super(numberOfMessagesToReceive, port);
        socketServer = new ServerSocket(port);
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        if (socketServer != null) {
            try {
                socketServer.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (final InterruptedException ie) {
                System.out.println("Shutdown of TCP server thread failed.");
            }
        }
    }

    @Override
    public void run() {
        System.out.println("TCP Server started");
        this.thread = Thread.currentThread();
        while (!shutdown) {
            try {
                final byte[] buffer = new byte[4096];
                try (Socket socket = socketServer.accept()) {
                    socket.setSoLinger(true, 0);
                    final InputStream in = socket.getInputStream();
                    int i = in.read(buffer, 0, buffer.length);
                    while (i != -1) {
                        if (i < buffer.length) {
                            final String line = new String(buffer, 0, i);
                            messageList.add(line);
                            i = in.read(buffer, 0, buffer.length);
                        } else if (i == 0) {
                            System.out.println("No data received");
                        } else {
                            System.out.println("Message too long");
                        }
                    }
                } catch (final BindException be) {
                    be.printStackTrace();
                }
            } catch (final Exception ex) {
                if (!shutdown) {
                    System.out.println("Caught exception: " + ex.getMessage());
                }
            }
        }
        System.out.println("TCP Server stopped");
    }
}
