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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class SocketAppenderTest {

    private static final int PORT = AvailablePortFinder.getNextAvailable();
    private static final int DYN_PORT = AvailablePortFinder.getNextAvailable();
    private static final int ERROR_PORT = AvailablePortFinder.getNextAvailable();

    private static TcpSocketTestServer tcpServer;
    private static UdpSocketTestServer udpServer;

    private final LoggerContext context = LoggerContext.getContext();
    private final Logger logger = context.getLogger(SocketAppenderTest.class.getName());

    @BeforeClass
    public static void setupClass() throws Exception {
        tcpServer = new TcpSocketTestServer(PORT);
        tcpServer.start();
        udpServer = new UdpSocketTestServer();
        udpServer.start();
        (LoggerContext.getContext()).reconfigure();
        ThreadContext.clearAll();
    }

    @AfterClass
    public static void cleanupClass() {
        tcpServer.shutdown();
        udpServer.shutdown();
        ThreadContext.clearAll();
    }

    @After
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
    @Ignore("WIP Bug when this method runs after testTcpAppender1()")
    public void testTcpAppender2() throws Exception {
        testTcpAppender(tcpServer, logger, Constants.ENCODER_BYTE_BUFFER_SIZE);
    }

    static void testTcpAppender(final TcpSocketTestServer tcpTestServer, final Logger logger, final int bufferSize)
            throws Exception {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .withHost("localhost")
                .withPort(tcpTestServer.getLocalPort())
                .withReconnectDelayMillis(-1)
                .withName("test")
                .withImmediateFail(false)
                .withBufferSize(bufferSize)
                .build();
        // @formatter:on
        appender.start();
        Assert.assertEquals(bufferSize, appender.getManager().getByteBuffer().capacity());

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
        LogEvent event = tcpTestServer.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a test message"));
        assertTrue("Message not delivered via TCP", tcpTestServer.getCount() > 0);
        assertEquals(expectedUuidStr, event.getContextData().getValue(tcKey));
        event = tcpTestServer.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("Throwing an exception"));
        assertTrue("Message not delivered via TCP", tcpTestServer.getCount() > 1);
        assertEquals(expectedUuidStr, event.getContextStack().pop());
        assertNotNull(event.getThrownProxy());
        assertEquals(expectedExMsg, event.getThrownProxy().getMessage());
    }

    @Test
    public void testDefaultProtocol() throws Exception {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .withPort(tcpServer.getLocalPort())
                .withReconnectDelayMillis(-1)
                .withName("test")
                .withImmediateFail(false)
                .build();
        // @formatter:on
        assertNotNull(appender);
        appender.stop();
    }

    @Test
    public void testUdpAppender() throws Exception {
        try {
            udpServer.latch.await();
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }

        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .withProtocol(Protocol.UDP)
                .withPort(tcpServer.getLocalPort())
                .withReconnectDelayMillis(-1)
                .withName("test")
                .withImmediateFail(false)
                .build();
        // @formatter:on
        appender.start();

        // set appender on root and set level to debug
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
        logger.debug("This is a udp message");
        final LogEvent event = udpServer.getQueue().poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a udp message"));
        assertTrue("Message not delivered via UDP", udpServer.getCount() > 0);
    }

    @Test
    public void testTcpAppenderDeadlock() throws Exception {

        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .withHost("localhost")
                .withPort(DYN_PORT)
                .withReconnectDelayMillis(100)
                .withName("test")
                .withImmediateFail(false)
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

            final LogEvent event = tcpSocketServer.getQueue().poll(3, TimeUnit.SECONDS);
            assertNotNull("No event retrieved", event);
        } finally {
            tcpSocketServer.shutdown();
        }
    }

    @Test
    public void testTcpAppenderNoWait() throws Exception {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .withHost("localhost")
                .withPort(ERROR_PORT)
                .withReconnectDelayMillis(100)
                .withName("test")
                .withImmediateFail(false)
                .withIgnoreExceptions(false)
                .build();
        // @formatter:on
        appender.start();
        // set appender on root and set level to debug
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);

        try {
            logger.debug("This message is written because a deadlock never.");
            fail("No Exception was thrown");
        } catch (final Exception ex) {
            // TODO: move exception to @Test(expect = Exception.class)
            // Failure is expected.
            // ex.printStackTrace();
        }
    }

    public static class UdpSocketTestServer extends Thread {

        private final DatagramSocket sock;
        private boolean shutdown = false;
        private Thread thread;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile int count = 0;
        private final BlockingQueue<LogEvent> queue;

        public UdpSocketTestServer() throws IOException {
            this.sock = new DatagramSocket(PORT);
            this.queue = new ArrayBlockingQueue<>(10);
        }

        public void reset() {
            queue.clear();
            count = 0;
        }

        public void shutdown() {
            this.shutdown = true;
            thread.interrupt();
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
                    final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
                    ++count;
                    final Object received = ois.readObject(); // separate lines for debugging
                    final LogEvent event = (LogEvent) received;
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
            return count;
        }

        public BlockingQueue<LogEvent> getQueue() {
            return queue;
        }
    }

    public static class TcpSocketTestServer extends Thread {

        private final ServerSocket serverSocket;
        private volatile boolean shutdown = false;
        private volatile int count = 0;
        private final BlockingQueue<LogEvent> queue;

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
            count = 0;
        }

        public void shutdown() {
            this.shutdown = true;
            interrupt();
        }

        @Override
        public void run() {
            try {
                try (final Socket socket = serverSocket.accept()) {
                    if (socket != null) {
                        final ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        while (!shutdown) {
                            queue.add((LogEvent) ois.readObject());
                            ++count;
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

        public BlockingQueue<LogEvent> getQueue() {
            return queue;
        }

        public int getCount() {
            return count;
        }
    }

}
