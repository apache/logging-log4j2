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
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import org.apache.logging.log4j.core.test.net.ssl.LegacyBsdTlsSyslogInputStreamReader;
import org.apache.logging.log4j.core.test.net.ssl.TlsSyslogInputStreamReader;
import org.apache.logging.log4j.core.test.net.ssl.TlsSyslogInputStreamReaderBase;
import org.apache.logging.log4j.core.test.net.ssl.TlsSyslogMessageFormat;

public class MockTlsSyslogServer extends MockSyslogServer {
    private final SSLServerSocket serverSocket;
    private SSLSocket clientSocket;
    private final List<String> messageList = new ArrayList<>();
    private TlsSyslogInputStreamReaderBase syslogReader;

    private volatile boolean shutdown = false;
    private Thread thread;

    private TlsSyslogMessageFormat messageFormat = TlsSyslogMessageFormat.SYSLOG;
    private final int numberOfMessageToReceive;

    public MockTlsSyslogServer(
            final int numberOfMessagesToReceive,
            final TlsSyslogMessageFormat format,
            final SSLServerSocket serverSocket) {
        this.messageFormat = format;
        this.numberOfMessageToReceive = numberOfMessagesToReceive;
        this.serverSocket = serverSocket;
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
        try {
            waitForConnection();
            processFrames();
        } catch (final Exception se) {
            se.printStackTrace();
        } finally {
            closeSockets();
        }
        LOGGER.info("{} stopped.", getName());
    }

    private void waitForConnection() throws IOException {
        clientSocket = (SSLSocket) serverSocket.accept();
        final InputStream clientSocketInputStream = clientSocket.getInputStream();
        syslogReader = createTLSSyslogReader(clientSocketInputStream);
    }

    private TlsSyslogInputStreamReaderBase createTLSSyslogReader(final InputStream inputStream) {
        switch (messageFormat) {
            case SYSLOG:
                return new TlsSyslogInputStreamReader(inputStream);
            case LEGACY_BSD:
                return new LegacyBsdTlsSyslogInputStreamReader(inputStream);
            default:
                return null;
        }
    }

    private void closeSockets() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void processFrames() throws IOException {
        try {
            int count = 0;
            while (!shutdown) {
                String message;
                message = syslogReader.read();
                LOGGER.debug("{} received a message: {}", getName(), message);
                messageList.add(message);
                count++;
                if (isEndOfMessages(count)) {
                    break;
                }
            }
            this.notify();
        } catch (final Exception e) {
            this.notify();
            throw new IOException(e);
        }
    }

    private boolean isEndOfMessages(final int count) {
        return count == numberOfMessageToReceive;
    }

    @Override
    public List<String> getMessageList() {
        return messageList;
    }
}
