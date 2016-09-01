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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.logging.log4j.core.util.Throwables;

public class MockUdpSyslogServer extends MockSyslogServer {
    private final DatagramSocket socket;
    private boolean shutdown = false;
    private Thread thread;

    public MockUdpSyslogServer(final int numberOfMessagesToReceive, final int port) throws SocketException {
        super(numberOfMessagesToReceive, port);
        this.socket = new DatagramSocket(port);
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        thread.interrupt();
        socket.close();
    }

    @Override
    public void run() {
        this.thread = Thread.currentThread();
        final byte[] bytes = new byte[4096];
        final DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        try {
            while (!shutdown) {
                socket.receive(packet);
                final String str = new String(packet.getData(), 0, packet.getLength());
                messageList.add(str);
            }
        } catch (final Exception e) {
            if (!shutdown) {
                Throwables.rethrow(e);
            }
        }
    }
}