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

    static TCPSocketServer tcpServer;
    static UDPSocketServer udpServer;

    private LoggerContext context = LoggerContext.getContext();
    private Logger root = context.getLogger(SocketAppenderTest.class.getName());

    @BeforeClass
    public static void setupClass() throws Exception {
        tcpServer = new TCPSocketServer(PORT);
        tcpServer.start();
        udpServer = new UDPSocketServer();
        udpServer.start();
        (LoggerContext.getContext()).reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        tcpServer.shutdown();
        udpServer.shutdown();
    }

    @After
    public void teardown() {
        removeAndStopAppenders();
        reset();
    }

    void removeAndStopAppenders() {
        final Map<String, Appender> map = root.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender appender = entry.getValue();
            root.removeAppender(appender);
            appender.stop();
        }
    }

    static void reset() {
        tcpServer.reset();
        udpServer.reset();
    }

    @Test
    public void testTcpAppender1() throws Exception {
        testTcpAppender(root, Constants.ENCODER_BYTE_BUFFER_SIZE, tcpServer.getQueue());
    }

    @Test
    @Ignore("WIP Bug when this method runs after testTcpAppender1()")
    public void testTcpAppender2() throws Exception {
        testTcpAppender(root, Constants.ENCODER_BYTE_BUFFER_SIZE, tcpServer.getQueue());
    }

    static void testTcpAppender(final Logger rootLogger, final int bufferSize, final BlockingQueue<LogEvent> blockingQ)
            throws Exception {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .withHost("localhost")
                .withPort(PORT)
                .withReconnectDelayMillis(-1)
                .withName("test")
                .withImmediateFail(false)
                .withBufferSize(bufferSize)
                .build();
        // @formatter:on
        appender.start();
        Assert.assertEquals(bufferSize, appender.getManager().getByteBuffer().capacity());

        // set appender on root and set level to debug
        rootLogger.addAppender(appender);
        rootLogger.setAdditive(false);
        rootLogger.setLevel(Level.DEBUG);
        final String tcKey = "UUID";
        final String expectedUuidStr = UUID.randomUUID().toString();
        ThreadContext.put(tcKey, expectedUuidStr);
        ThreadContext.push(expectedUuidStr);
        final String expectedExMsg = "This is a test";
        try {
            rootLogger.debug("This is a test message");
            final Throwable child = new LoggingException(expectedExMsg);
            rootLogger.error("Throwing an exception", child);
            rootLogger.debug("This is another test message");
        } finally {
            ThreadContext.remove(tcKey);
            ThreadContext.pop();
        }
        Thread.sleep(250);
        LogEvent event = blockingQ.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a test message"));
        assertTrue("Message not delivered via TCP", tcpServer.getCount() > 0);
        assertEquals(expectedUuidStr, event.getContextMap().get(tcKey));
        event = blockingQ.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("Throwing an exception"));
        assertTrue("Message not delivered via TCP", tcpServer.getCount() > 1);
        assertEquals(expectedUuidStr, event.getContextStack().pop());
        assertNotNull(event.getThrownProxy());
        assertEquals(expectedExMsg, event.getThrownProxy().getMessage());
    }

    @Test
    public void testDefaultProtocol() throws Exception {
        // @formatter:off
        final SocketAppender appender = SocketAppender.newBuilder()
                .withHost("localhost")
                .withPort(PORT)
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
                .withHost("localhost")
                .withPort(PORT)
                .withReconnectDelayMillis(-1)
                .withName("test")
                .withImmediateFail(false)
                .build();
        // @formatter:on
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a udp message");
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
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);

        final TCPSocketServer tcpSocketServer = new TCPSocketServer(DYN_PORT);
        try {
            tcpSocketServer.start();

            root.debug("This message is written because a deadlock never.");

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
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);

        try {
            root.debug("This message is written because a deadlock never.");
            fail("No Exception was thrown");
        } catch (final Exception ex) {
            // TODO: move exception to @Test(expect = Exception.class)
            // Failure is expected.
            // ex.printStackTrace();
        }
    }

    public static class UDPSocketServer extends Thread {
        private final DatagramSocket sock;
        private boolean shutdown = false;
        private Thread thread;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile int count = 0;
        private final BlockingQueue<LogEvent> queue;

        public UDPSocketServer() throws IOException {
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

    public static class TCPSocketServer extends Thread {

        private final ServerSocket sock;
        private volatile boolean shutdown = false;
        private volatile int count = 0;
        private final BlockingQueue<LogEvent> queue;

        public TCPSocketServer(final int port) throws IOException {
            this.sock = new ServerSocket(port);
            this.queue = new ArrayBlockingQueue<>(10);
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
                try (final Socket socket = sock.accept()) {
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
