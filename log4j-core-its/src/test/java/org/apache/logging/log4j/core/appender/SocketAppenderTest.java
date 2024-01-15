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
package org.apache.logging.log4j.core.appender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.test.AvailablePortFinder;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 *
 */
@Tag("sleepy")
public class SocketAppenderTest {

    private static final int PORT = AvailablePortFinder.getNextAvailable();
    private static final int DYN_PORT = AvailablePortFinder.getNextAvailable();
    private static final int ERROR_PORT = AvailablePortFinder.getNextAvailable();

    private static TcpSocketTestServer tcpServer;
    private static UdpSocketTestServer udpServer;

    private static final Layout LAYOUT = JsonTemplateLayout.newBuilder()
            .setConfiguration(new DefaultConfiguration())
            .setEventTemplate(LogEventDto.JSON_TEMPLATE)
            .build();

    public static final class LogEventDto {

        private static final String JSON_TEMPLATE = ("{" + "'message': {'$resolver': 'message', 'stringified': true},"
                        + "'contextData': {'$resolver': 'mdc'},"
                        + "'contextStack': {'$resolver': 'ndc'},"
                        + "'error': {'$resolver': 'exception', 'field': 'message'}"
                        + "}")
                .replaceAll("'", "\"");

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private final String message;

        private final Map<String, String> contextData;

        private final List<String> contextStack;

        private final String error;

        @JsonCreator
        public LogEventDto(
                @JsonProperty("message") final String message,
                @JsonProperty("contextData") final Map<String, String> contextData,
                @JsonProperty("contextStack") final List<String> contextStack,
                @JsonProperty("error") final String error) {
            this.message = message;
            this.contextData = contextData;
            this.contextStack = contextStack;
            this.error = error;
        }

        private static LogEventDto fromJsonBytes(final byte[] jsonBytes) throws IOException {
            return MAPPER.readValue(jsonBytes, LogEventDto.class);
        }

        private static Iterator<LogEventDto> fromJsonByteStream(final InputStream stream) throws IOException {
            return MAPPER.readerFor(LogEventDto.class).readValues(stream);
        }
    }

    private final LoggerContext context = LoggerContext.getContext();
    private final Logger logger = context.getLogger(SocketAppenderTest.class.getName());

    @BeforeAll
    public static void setupClass() throws Exception {
        tcpServer = new TcpSocketTestServer(PORT);
        tcpServer.start();
        udpServer = new UdpSocketTestServer();
        udpServer.start();
        LoggerContext.getContext().reconfigure();
        ThreadContext.clearAll();
    }

    @AfterAll
    public static void cleanupClass() {
        tcpServer.shutdown();
        udpServer.shutdown();
        ThreadContext.clearAll();
    }

    @AfterEach
    public void teardown() {
        ThreadContext.clearAll();
        removeAndStopAppenders();
        reset();
    }

    void removeAndStopAppenders() {
        final Map<String, Appender> map = logger.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender appender = entry.getValue();
            logger.removeAppender(appender);
            appender.stop();
        }
    }

    static void reset() {
        tcpServer.reset();
        udpServer.reset();
    }

    @Test
    public void testTcpAppender1() throws Exception {
        testTcpAppender(tcpServer, logger, Constants.ENCODER_BYTE_BUFFER_SIZE);
    }

    @Test
    @Disabled("WIP Bug when this method runs after testTcpAppender1()")
    public void testTcpAppender2() throws Exception {
        testTcpAppender(tcpServer, logger, Constants.ENCODER_BYTE_BUFFER_SIZE);
    }

    static void testTcpAppender(final TcpSocketTestServer tcpTestServer, final Logger logger, final int bufferSize)
            throws Exception {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .setHost("localhost")
                .setPort(tcpTestServer.getLocalPort())
                .setReconnectDelayMillis(-1)
                .setName("test")
                .setImmediateFail(false)
                .setBufferSize(bufferSize)
                .setLayout(LAYOUT)
                .build();
        // @formatter:on
        appender.start();
        assertEquals(bufferSize, appender.getManager().getByteBuffer().capacity());

        // set appender on root and set level to debug
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
        final String tcKey = "UUID";
        final String expectedUuidStr = UUID.randomUUID().toString();
        ThreadContext.put(tcKey, expectedUuidStr);
        ThreadContext.push(expectedUuidStr);
        final String expectedExMsg = "This is a test";
        try {
            logger.debug("This is a test message");
            final Throwable child = new LoggingException(expectedExMsg);
            logger.error("Throwing an exception", child);
            logger.debug("This is another test message");
        } finally {
            ThreadContext.remove(tcKey);
            ThreadContext.pop();
        }
        Thread.sleep(250);
        LogEventDto event = tcpTestServer.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull(event, "No event retrieved");
        assertEquals("This is a test message", event.message, "Incorrect event");
        assertTrue(tcpTestServer.getCount() > 0, "Message not delivered via TCP");
        assertEquals(expectedUuidStr, event.contextData.get(tcKey));
        event = tcpTestServer.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull(event, "No event retrieved");
        assertEquals("Throwing an exception", event.message, "Incorrect event");
        assertTrue(tcpTestServer.getCount() > 1, "Message not delivered via TCP");
        assertThat(event.contextStack).containsOnly(expectedUuidStr);
        assertEquals(expectedExMsg, event.error);
    }

    @Test
    public void testDefaultProtocol() {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .setPort(tcpServer.getLocalPort())
                .setReconnectDelayMillis(-1)
                .setName("test")
                .setImmediateFail(false)
                .setLayout(LAYOUT)
                .build();
        // @formatter:on
        assertNotNull(appender);
        appender.stop();
    }

    @Test
    public void testUdpAppender() throws Exception {
        final boolean released = udpServer.latch.await(3, TimeUnit.SECONDS);
        assertTrue(released);

        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .setProtocol(Protocol.UDP)
                .setPort(tcpServer.getLocalPort())
                .setReconnectDelayMillis(-1)
                .setName("test")
                .setImmediateFail(false)
                .setLayout(LAYOUT)
                .build();
        // @formatter:on
        appender.start();

        // set appender on root and set level to debug
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
        logger.debug("This is a udp message");
        final LogEventDto event = udpServer.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull(event, "No event retrieved");
        assertEquals("This is a udp message", event.message, "Incorrect event");
        assertTrue(udpServer.getCount() > 0, "Message not delivered via UDP");
    }

    @Test
    public void testTcpAppenderDeadlock() throws Exception {

        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .setHost("localhost")
                .setPort(DYN_PORT)
                .setReconnectDelayMillis(100)
                .setName("test")
                .setImmediateFail(false)
                .setLayout(LAYOUT)
                .build();
        // @formatter:on
        appender.start();
        // set appender on root and set level to debug
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);

        final TcpSocketTestServer tcpSocketServer = new TcpSocketTestServer(DYN_PORT);
        try {
            tcpSocketServer.start();

            logger.debug("This message is written because a deadlock never.");

            final LogEventDto event = tcpSocketServer.getQueue().poll(3, TimeUnit.SECONDS);
            assertNotNull(event, "No event retrieved");
        } finally {
            tcpSocketServer.shutdown();
        }
    }

    @Test
    public void testTcpAppenderNoWait() {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .setHost("localhost")
                .setPort(ERROR_PORT)
                .setReconnectDelayMillis(100)
                .setName("test")
                .setImmediateFail(false)
                .setIgnoreExceptions(false)
                .setLayout(LAYOUT)
                .build();
        // @formatter:on
        appender.start();
        // set appender on root and set level to debug
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);

        assertThrows(Exception.class, () -> logger.debug("This message is written because a deadlock never."));
    }

    public static class UdpSocketTestServer extends Thread {

        private final DatagramSocket sock;
        private boolean shutdown = false;
        private Thread thread;
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicInteger count = new AtomicInteger();
        private final BlockingQueue<LogEventDto> queue;

        public UdpSocketTestServer() throws IOException {
            this.sock = new DatagramSocket(PORT);
            this.queue = new ArrayBlockingQueue<>(10);
        }

        public void reset() {
            queue.clear();
            count.set(0);
        }

        public void shutdown() {
            this.shutdown = true;
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException ie) {
                System.out.println("Unable to stop server");
            }
        }

        @Override
        public void run() {
            this.thread = Thread.currentThread();
            final byte[] bytes = new byte[4096];
            final DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            try {
                while (!shutdown) {
                    latch.countDown();
                    sock.receive(packet);
                    count.incrementAndGet();
                    final LogEventDto event = LogEventDto.fromJsonBytes(packet.getData());
                    queue.add(event);
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                if (!shutdown) {
                    Throwables.rethrow(e);
                }
            }
        }

        public int getCount() {
            return count.get();
        }

        public BlockingQueue<LogEventDto> getQueue() {
            return queue;
        }
    }

    public static class TcpSocketTestServer extends Thread {

        private final ServerSocket serverSocket;
        private volatile boolean shutdown = false;
        private final AtomicInteger count = new AtomicInteger();
        private final BlockingQueue<LogEventDto> queue;

        @SuppressWarnings("resource")
        public TcpSocketTestServer(final int port) throws IOException {
            this(new ServerSocket(port));
        }

        public TcpSocketTestServer(final ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            this.queue = new ArrayBlockingQueue<>(10);
        }

        public int getLocalPort() {
            return serverSocket.getLocalPort();
        }

        public void reset() {
            queue.clear();
            count.set(0);
        }

        public void shutdown() {
            this.shutdown = true;
            interrupt();
            try {
                this.join(100);
            } catch (InterruptedException ie) {
                System.out.println("Unable to stop server");
            }
        }

        @Override
        public void run() {
            try {
                try (final Socket socket = serverSocket.accept()) {
                    if (socket != null) {
                        final InputStream is = socket.getInputStream();
                        while (!shutdown) {
                            final Iterator<LogEventDto> it = LogEventDto.fromJsonByteStream(is);
                            while (it.hasNext()) {
                                final LogEventDto logEvent = it.next();
                                queue.add(logEvent);
                                count.incrementAndGet();
                            }
                        }
                    }
                }
            } catch (final EOFException eof) {
                // Socket is closed.
            } catch (final Exception e) {
                if (!shutdown) {
                    Throwables.rethrow(e);
                }
            }
        }

        public BlockingQueue<LogEventDto> getQueue() {
            return queue;
        }

        public int getCount() {
            return count.get();
        }
    }
}
