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
package org.apache.logging.log4j.layout.template.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.test.junit.UsingTestProperties;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@UsingStatusListener
@UsingTestProperties
class JsonTemplateLayoutNullEventDelimiterTest {

    private static final int TIMEOUT_MS = 10_000;

    private static Logger LOGGER = StatusLogger.getLogger();

    private static TcpServer server;

    private static TestProperties props;

    @BeforeAll
    static void setup() throws IOException {
        // Start the TCP server.
        server = new TcpServer(0);
        // Set the configuration.
        props.setProperty("serverPort", server.getPort());
    }

    @AfterAll
    static void cleanup() throws IOException {
        // Stop the TCP server.
        server.close();
    }

    @Test
    @LoggerContextSource
    void test(final @Named("Socket") SocketAppender appender, final LoggerContext ctx) throws Exception {
        assertThat(appender).isNotNull();
        appender.addFilter(new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                LOGGER.info("Sending message {}", event.getMessage());
                return super.filter(event);
            }
        });
        // Produce log events.
        final Logger logger = ctx.getLogger(JsonTemplateLayoutNullEventDelimiterTest.class);
        logger.log(Level.INFO, "foo");
        logger.log(Level.INFO, "bar");

        // Set the expected bytes.
        final byte[] expectedBytes = {
            '"', 'f', 'o', 'o', '"', '\0',
            '"', 'b', 'a', 'r', '"', '\0'
        };

        // Wait for the log events.
        try {
            Awaitility.await()
                    .atMost(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .until(() -> server.getTotalReadByteCount() >= expectedBytes.length);
        } catch (final ConditionTimeoutException e) {
            LOGGER.info("Timeout reached while waiting for {} bytes.", expectedBytes.length);
        }

        // Verify the received log events.
        final byte[] actualBytes = server.getReceivedBytes();
        assertThat(actualBytes).containsExactly(expectedBytes);
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
            serverSocket.setSoTimeout(TIMEOUT_MS);
            setDaemon(true);
            start();
            LOGGER.info("TcpServer started on port {}.", serverSocket.getLocalPort());
        }

        @Override
        public void run() {
            try {
                try (final Socket socket = serverSocket.accept()) {
                    final InputStream inputStream = socket.getInputStream();
                    final byte[] buffer = new byte[1024];
                    while (true) {
                        final int readByteCount = inputStream.read(buffer);
                        if (readByteCount != -1) {
                            LOGGER.info("Received bytes {}.", () -> Hex.encodeHex(buffer, 0, readByteCount, false));
                            synchronized (this) {
                                totalReadByteCount += readByteCount;
                                outputStream.write(buffer, 0, readByteCount);
                            }
                        } else {
                            break;
                        }
                    }
                }
            } catch (final EOFException ignored) {
                // Socket is closed.
            } catch (final Exception error) {
                if (!closed) {
                    LOGGER.error("TcpServer received an error.", error);
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
