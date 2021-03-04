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
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import org.apache.logging.log4j.core.net.ssl.LegacyBsdTlsSyslogInputStreamReader;
import org.apache.logging.log4j.core.net.ssl.TlsSyslogInputStreamReader;
import org.apache.logging.log4j.core.net.ssl.TlsSyslogInputStreamReaderBase;
import org.apache.logging.log4j.core.net.ssl.TlsSyslogMessageFormat;

public class MockTlsSyslogServer extends MockSyslogServer {
    private final SSLServerSocket serverSocket;
    private SSLSocket clientSocket;
    private final List<String> messageList = new ArrayList<>();
    private TlsSyslogInputStreamReaderBase syslogReader;

    private volatile boolean shutdown = false;
    private Thread thread;

    private TlsSyslogMessageFormat messageFormat = TlsSyslogMessageFormat.SYSLOG;
    private final int loopLen;

    public MockTlsSyslogServer(final int loopLen, final TlsSyslogMessageFormat format, final SSLServerSocket serverSocket) {
        super(loopLen, serverSocket.getLocalPort());
        this.messageFormat = format;
        this.loopLen = loopLen;
        this.serverSocket = serverSocket;
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        try {
            if (serverSocket != null) {
                try {
                    this.serverSocket.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            this.interrupt();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if (thread != null) {
            try {
                thread.join(100);
            } catch (final InterruptedException ie) {
                System.out.println("Shutdown of TLS server thread failed.");
            }
        }
    }

    @Override
    public void run() {
        System.out.println("TLS Server Started");
        this.thread = Thread.currentThread();
        try {
            waitForConnection();
            processFrames();
        } catch (final Exception se) {
            se.printStackTrace();
        } finally {
            closeSockets();
        }
        System.out.println("TLS Server stopped");
    }

    private void waitForConnection() throws IOException {
        clientSocket =  (SSLSocket) serverSocket.accept();
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
        if(clientSocket != null) {
            try {
                clientSocket.close();
            }
            catch(final Exception e) {
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
                messageList.add(message);
                count++;
                if (isEndOfMessages(count)) {
                    break;
                }
            }
            this.notify();
        }
        catch(final Exception e) {
            this.notify();
            throw new IOException(e);
        }
    }

    private boolean isEndOfMessages(final int count) {
        return count == loopLen;
    }

    @Override
    public List<String> getMessageList() {
        return messageList;
    }
}