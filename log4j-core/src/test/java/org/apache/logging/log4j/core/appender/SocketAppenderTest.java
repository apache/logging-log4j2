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
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class SocketAppenderTest {

    private static final int PORT = AvailablePortFinder.getNextAvailable();
    private static final int DYN_PORT = AvailablePortFinder.getNextAvailable();
    private static final int ERROR_PORT = AvailablePortFinder.getNextAvailable();

    private static BlockingQueue<LogEvent> list = new ArrayBlockingQueue<>(10);

    private static TCPSocketServer tcpServer;
    private static UDPSocketServer udpServer;

    LoggerContext context = LoggerContext.getContext();
    Logger root = context.getLogger("SocketAppenderTest");

    private static int tcpCount = 0;
    private static int udpCount = 0;

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
        list.clear();
    }

    @After
    public void teardown() {
        final Map<String, Appender> map = root.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
        tcpCount = 0;
        udpCount = 0;
        list.clear();
    }

    @Test
    public void testTcpAppender() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", PORT, Protocol.TCP, null, 0, -1,
                false, "Test", true, true, null, null, false, null);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        final String tcKey = "UUID";
        final String expectedUuidStr = UUID.randomUUID().toString();
        ThreadContext.put(tcKey, expectedUuidStr);
        ThreadContext.push(expectedUuidStr);
        final String expectedExMsg = "This is a test";
        try {
            root.debug("This is a test message");
            final Throwable child = new LoggingException(expectedExMsg);
            root.error("Throwing an exception", child);
            root.debug("This is another test message");
        } finally {
            ThreadContext.remove(tcKey);
            ThreadContext.pop();
        }
        Thread.sleep(250);
        LogEvent event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a test message"));
        assertTrue("Message not delivered via TCP", tcpCount > 0);
        assertEquals(expectedUuidStr, event.getContextMap().get(tcKey));
        event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("Throwing an exception"));
        assertTrue("Message not delivered via TCP", tcpCount > 1);
        assertEquals(expectedUuidStr, event.getContextStack().pop());
        assertNotNull(event.getThrownProxy());
        assertEquals(expectedExMsg, event.getThrownProxy().getMessage());
    }

    @Test
    public void testDefaultProtocol() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", PORT, (Protocol) null, null, 0, -1,
                false, "Test", true, true, null, null, false, null);
        assertNotNull(appender);
    }

    @Test
    public void testUdpAppender() throws Exception {
        try {
            udpServer.latch.await();
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }

        final SocketAppender appender = SocketAppender.createAppender("localhost", PORT, Protocol.UDP, null, 0, -1,
                false, "Test", true, true, null, null, false, null);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a udp message");
        final LogEvent event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a udp message"));
        assertTrue("Message not delivered via UDP", udpCount > 0);
    }

    @Test
    public void testTcpAppenderDeadlock() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", DYN_PORT, Protocol.TCP, null, 0,
                100, false, "Test", true, true, null, null, false, null);
        appender.start();
        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);

        new TCPSocketServer(DYN_PORT).start();

        root.debug("This message is written because a deadlock never.");

        final LogEvent event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
    }

    @Test
    public void testTcpAppenderNoWait() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", ERROR_PORT, Protocol.TCP, null, 0,
                100, true, "Test", true, false, null, null, false, null);
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
        }
    }

    public static class UDPSocketServer extends Thread {
        private final DatagramSocket sock;
        private boolean shutdown = false;
        private Thread thread;
        private final CountDownLatch latch = new CountDownLatch(1);

        public UDPSocketServer() throws IOException {
            this.sock = new DatagramSocket(PORT);
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
                    ++udpCount;
                    final Object received = ois.readObject(); // separate lines for debugging
                    final LogEvent event = (LogEvent) received;
                    list.add(event);
                }
            } catch (final Throwable ex) {
                ex.printStackTrace();
                if (!shutdown) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public static class TCPSocketServer extends Thread {

        private final ServerSocket sock;
        private boolean shutdown = false;

        public TCPSocketServer(final int port) throws IOException {
            this.sock = new ServerSocket(port);
        }

        public void shutdown() {
            this.shutdown = true;
            interrupt();
        }

        @Override
        public void run() {
            try {
                final Socket socket = sock.accept();
                if (socket != null) {
                    final ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    while (!shutdown) {
                        list.add((LogEvent) ois.readObject());
                        ++tcpCount;
                    }
                }
            } catch (final EOFException eof) {
                // Socket is closed.
            } catch (final Exception ex) {
                if (!shutdown) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

}
