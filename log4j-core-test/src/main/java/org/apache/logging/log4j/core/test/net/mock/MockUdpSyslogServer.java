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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.apache.logging.log4j.core.util.Throwables;

public class MockUdpSyslogServer extends MockSyslogServer {
    private final DatagramSocket socket;
    private volatile boolean shutdown = false;
    private Thread thread;

    public MockUdpSyslogServer(final int numberOfMessagesToReceive, final int port) throws SocketException {
        this(port);
    }

    public MockUdpSyslogServer() throws SocketException {
        this(0);
    }

    private MockUdpSyslogServer(final int port) throws SocketException {
        super(0, port);
        this.socket = new DatagramSocket(port);
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        if (socket != null) {
            socket.close();
        }
        if (thread != null) {
            thread.interrupt();
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
        final byte[] bytes = new byte[4096];
        final DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        try {
            while (!shutdown) {
                socket.receive(packet);
                final String message = new String(packet.getData(), 0, packet.getLength());
                LOGGER.debug("{} received a message: {}", getName(), message);
                messageList.add(message);
            }
        } catch (final Exception e) {
            if (!shutdown) {
                Throwables.rethrow(e);
            }
        }
        LOGGER.info("{} stopped.", getName());
    }
}
