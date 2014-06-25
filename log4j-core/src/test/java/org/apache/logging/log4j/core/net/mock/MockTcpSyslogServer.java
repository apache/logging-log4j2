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
import java.net.ServerSocket;
import java.net.Socket;

public class MockTcpSyslogServer extends MockSyslogServer {
    private final ServerSocket sock;
    private boolean shutdown = false;
    private Thread thread;

    public MockTcpSyslogServer(final int numberOfMessagesToReceive, final int port) throws IOException {
        super(numberOfMessagesToReceive, port);
        sock = new ServerSocket(port);
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        try {
            sock.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        thread.interrupt();
    }

    @Override
    public void run() {
        this.thread = Thread.currentThread();
        while (!shutdown) {
            try {
                final byte[] buffer = new byte[4096];
                final Socket socket = sock.accept();
                socket.setSoLinger(true, 0);
                if (socket != null) {
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

                    socket.close();
                }
            } catch (final Exception ex) {
                if (!shutdown) {
                    System.out.println("Caught exception: " + ex.getMessage());
                }
            }
        }
    }
}
