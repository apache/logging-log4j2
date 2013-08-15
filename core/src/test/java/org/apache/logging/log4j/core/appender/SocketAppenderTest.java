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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 */
public class SocketAppenderTest {

    private static final String PORT = "8199";
    private static final String DYN_PORT = "8300";
    private static final String ERROR_PORT = "8301";
    private static final int PORTNUM1 = Integer.parseInt(PORT);
    private static final int PORTNUM2 = Integer.parseInt(DYN_PORT);

    private static BlockingQueue<LogEvent> list = new ArrayBlockingQueue<LogEvent>(10);

    private static TCPSocketServer tcp;
    private static UDPSocketServer udp;

    LoggerContext context = (LoggerContext) LogManager.getContext();
    Logger root = context.getLogger("SocketAppenderTest");

    private static int tcpCount = 0;
    private static int udpCount = 0;

    @BeforeClass
    public static void setupClass() throws Exception {
        tcp = new TCPSocketServer(PORTNUM1);
        tcp.start();
        udp = new UDPSocketServer();
        udp.start();
        ((LoggerContext) LogManager.getContext()).reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        tcp.shutdown();
        udp.shutdown();
        list.clear();
    }

    @After
    public void teardown() {
        final Map<String,Appender> map = root.getAppenders();
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
    public void testTCPAppender() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", PORT, "tcp", "-1",
            "false", "Test", null, null, null, null, null, null);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        final Throwable child = new LoggingException("This is a test");
        root.error("Throwing an exception", child);
        root.debug("This is another test message");
        Thread.sleep(250);
        LogEvent event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("This is a test message"));
        assertTrue("Message not delivered via TCP", tcpCount > 0);
        event = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", event);
        assertTrue("Incorrect event", event.getMessage().getFormattedMessage().equals("Throwing an exception"));
        assertTrue("Message not delivered via TCP", tcpCount > 1);
    }

    @Test
    public void testDefaultProtocol() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", PORT, null, "-1",
            "false", "Test", null, null, null, null, null, null);
        assertNotNull(appender);
    }



    @Test
    public void testUDPAppender() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", PORT, "udp", "-1",
            "false", "Test", null, null, null, null, null, null);
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

        final SocketAppender appender = SocketAppender.createAppender("localhost", DYN_PORT, "tcp", "10000",
                "false", "Test", null, null, null, null, null, null);
            appender.start();
            // set appender on root and set level to debug
            root.addAppender(appender);
            root.setAdditive(false);
            root.setLevel(Level.DEBUG);

            new TCPSocketServer(PORTNUM2).start();

            root.debug("This message is written because a deadlock never.");

            final LogEvent event = list.poll(3, TimeUnit.SECONDS);
            assertNotNull("No event retrieved", event);
    }

    @Test
    public void testTcpAppenderNoWait() throws Exception {

        final SocketAppender appender = SocketAppender.createAppender("localhost", ERROR_PORT, "tcp", "10000",
            "true", "Test", null, "false", null, null, null, null);
        appender.start();
        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);

        try {
            root.debug("This message is written because a deadlock never.");
            fail("No Exception was thrown");
        } catch (final Exception ex) {
            // Failure is expected.
        }
    }


    public static class UDPSocketServer extends Thread {
        private final DatagramSocket sock;
        private boolean shutdown = false;
        private Thread thread;

        public UDPSocketServer() throws IOException {
            this.sock = new DatagramSocket(PORTNUM1);
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
                    sock.receive(packet);
                    final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
                    ++udpCount;
                    list.add((LogEvent) ois.readObject());
                }
            } catch (final Exception ex) {
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
