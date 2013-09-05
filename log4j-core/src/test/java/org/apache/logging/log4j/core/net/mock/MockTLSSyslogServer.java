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

import org.apache.logging.log4j.core.net.ssl.LegacyBSDTLSSyslogInputStreamReader;
import org.apache.logging.log4j.core.net.ssl.TLSSyslogInputStreamReader;
import org.apache.logging.log4j.core.net.ssl.TLSSyslogInputStreamReaderBase;
import org.apache.logging.log4j.core.net.ssl.TLSSyslogMessageFormat;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MockTLSSyslogServer extends MockSyslogServer {
    private SSLServerSocket serverSocket;
    private SSLSocket clientSocket;
    private List<String> messageList = new ArrayList<String>();
    private TLSSyslogInputStreamReaderBase syslogReader;

    private TLSSyslogMessageFormat messageFormat = TLSSyslogMessageFormat.SYSLOG;
    private int loopLen;

    public MockTLSSyslogServer(int loopLen, TLSSyslogMessageFormat format, SSLServerSocket serverSocket) {
        super(loopLen, serverSocket.getLocalPort());
        this.messageFormat = format;
        this.loopLen = loopLen;
        this.serverSocket = serverSocket;
    }

    @Override
    public void shutdown() {
        try {
            try {
                this.serverSocket.close();
            }
            catch (Exception e) {

            }
            this.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            waitForConnection();
            processFrames();
        } catch (Exception se) {
            se.printStackTrace();
        } finally {
            closeSockets();
        }
    }

    private void waitForConnection() throws IOException {
        clientSocket =  (SSLSocket) serverSocket.accept();
        InputStream clientSocketInputStream = clientSocket.getInputStream();
        syslogReader = createTLSSyslogReader(clientSocketInputStream);
    }

    private TLSSyslogInputStreamReaderBase createTLSSyslogReader(InputStream inputStream) {
        switch (messageFormat) {
            case SYSLOG:
                return new TLSSyslogInputStreamReader(inputStream);
            case LEGACY_BSD:
                return new LegacyBSDTLSSyslogInputStreamReader(inputStream);
            default:
                return null;
        }
    }

    private void closeSockets() {
        if(clientSocket != null) {
            try {
                clientSocket.close();
            }
            catch(Exception e) {}
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception e) {
            }
        }
    }

    private synchronized void processFrames() throws IOException {
        try {
            int count = 0;
            while (true) {
                String message = "";
                message = syslogReader.read();
                messageList.add(message);
                count++;
                if (isEndOfMessages(count))
                    break;
            }
            this.notify();
        }
        catch(Exception e) {
            this.notify();
            throw new IOException(e);
        }
        return;
    }

    private boolean isEndOfMessages(int count) {
        return count == loopLen;
    }

    public List<String> getMessageList() {
        return messageList;
    }
}