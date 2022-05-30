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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;

class JsonTemplateLayoutNullEventDelimiterTest {

    @Test
    void test() throws Exception {

        // Start the TCP server.
        try (final TcpServer server = new TcpServer(0)) {

            // Set the configuration.
            System.setProperty(
                    "serverPort",
                    String.valueOf(server.getPort()));
            System.setProperty(
                    "log4j.configurationFile",
                    "nullEventDelimitedJsonTemplateLayoutLogging.xml");

            // Produce log events.
            final Logger logger = LogManager.getLogger(JsonTemplateLayoutNullEventDelimiterTest.class);
            logger.log(Level.INFO, "foo");
            logger.log(Level.INFO, "bar");

            // Set the expected bytes.
            final byte[] expectedBytes = {
                    '"', 'f', 'o', 'o', '"', '\0',
                    '"', 'b', 'a', 'r', '"', '\0'
            };

            // Wait for the log events.
            Awaitility
                    .await()
                    .atMost(Duration.ofSeconds(10))
                    .pollDelay(Duration.ofSeconds(1))
                    .until(() -> server.getTotalReadByteCount() >= expectedBytes.length);

            // Verify the received log events.
            final byte[] actualBytes = server.getReceivedBytes();
            Assertions.assertThat(actualBytes).startsWith(expectedBytes);

        }

    }

    private static final class TcpServer extends Thread implements AutoCloseable {

        private final ServerSocket serverSocket;

        private final ByteArrayOutputStream outputStream;

        private volatile int totalReadByteCount = 0;

        private volatile boolean closed = false;

        private TcpServer(final int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
            this.outputStream = new ByteArrayOutputStream();
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(5_000);
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            try {
                try (final Socket socket = serverSocket.accept()) {
                    final InputStream inputStream = socket.getInputStream();
                    final byte[] buffer = new byte[1024];
                    // noinspection InfiniteLoopStatement
                    while (true) {
                        final int readByteCount = inputStream.read(buffer);
                        if (readByteCount > 0) {
                            synchronized (this) {
                                totalReadByteCount += readByteCount;
                                outputStream.write(buffer, 0, readByteCount);
                            }
                        }
                    }
                }
            } catch (final EOFException ignored) {
                // Socket is closed.
            } catch (final Exception error) {
                if (!closed) {
                    throw new RuntimeException(error);
                }
            }
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public synchronized byte[] getReceivedBytes() {
            return outputStream.toByteArray();
        }

        public synchronized int getTotalReadByteCount() {
            return totalReadByteCount;
        }

        @Override
        public synchronized void close() {
            if (closed) {
                throw new IllegalStateException("shutdown has already been invoked");
            }
            closed = true;
            interrupt();
            try {
                join(3_000L);
            } catch (InterruptedException ignored) {
                // Due to JDK-7027157, we shouldn't throw an InterruptedException
                // from an AutoCloseable#close() method. Hence we catch it and
                // then restore the interrupted flag.
                Thread.currentThread().interrupt();
            }
        }

    }

}
