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
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SyslogAppenderTest {

    private static final String HOST = "localhost";
    private static final String PORT = "8199";
    private static final int PORTNUM = Integer.parseInt(PORT);

    private static BlockingQueue<String> list = new ArrayBlockingQueue<String>(10);

    private static TCPSocketServer tcp;
    private static UDPSocketServer udp;

    LoggerContext ctx = (LoggerContext) LogManager.getContext();
    Logger root = ctx.getLogger("SyslogAppenderTest");

    private static int tcpCount = 0;
    private static int udpCount = 0;

    private static final String line1 =
        "TestApp - Audit [Transfer@18060 Amount=\"200.00\" FromAccount=\"123457\" ToAccount=\"123456\"]" +
        "[RequestContext@18060 ipAddress=\"192.168.0.120\" loginId=\"JohnDoe\"] Transfer Complete\n";

    @BeforeClass
    public static void setupClass() throws Exception {
        tcp = new TCPSocketServer();
        tcp.start();
        udp = new UDPSocketServer();
        udp.start();
        ((LoggerContext) LogManager.getContext()).reconfigure();
    }

    @AfterClass
    public static void cleanupClass() {
        tcp.shutdown();
        udp.shutdown();
    }

    @After
    public void teardown() {
        final Map<String,Appender<?>> map = root.getAppenders();
        for (final Map.Entry<String, Appender<?>> entry : map.entrySet()) {
            final Appender<?> app = entry.getValue();
            root.removeAppender(app);
            app.stop();
        }
        tcpCount = 0;
        udpCount = 0;
    }

    @Test
    public void testTCPAppender() throws Exception {
        final SyslogAppender appender = createAppender("tcp", "bsd");
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        String msg = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", msg);
        assertTrue("Incorrect msg: " + msg, msg.endsWith("This is a test message\n"));
        assertTrue("Message not delivered via TCP", tcpCount > 0);
        root.debug("This is test message 2");
        msg = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", msg);
        assertTrue("Incorrect msg: " + msg, msg.endsWith("This is test message 2\n"));
        assertTrue("Message not delivered via TCP", tcpCount > 1);
    }


    @Test
    public void testDefaultAppender() throws Exception {
        final SyslogAppender appender = createAppender("tcp", null);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setAdditive(false);
        root.setLevel(Level.DEBUG);
        root.debug("This is a test message");
        String msg = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", msg);
        assertTrue("Incorrect msg: " + msg, msg.endsWith("This is a test message\n"));
        assertTrue("Message not delivered via TCP", tcpCount > 0);
        root.debug("This is test message 2");
        msg = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", msg);
        assertTrue("Incorrect msg: " + msg, msg.endsWith("This is test message 2\n"));
        assertTrue("Message not delivered via TCP", tcpCount > 1);
    }



    @Test
    public void testTCPStructuredAppender() throws Exception {
        final SyslogAppender appender = createAppender("tcp", "RFC5424");
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);
        root.setAdditive(false);
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Transfer@18060", "Transfer Complete", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        root.info(MarkerManager.getMarker("EVENT"), msg);
        final String str = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", str);
        assertTrue("Incorrect msg: " + str, str.endsWith(line1));
        assertTrue("Message not delivered via TCP", tcpCount > 0);
    }


    @Test
    public void testUDPAppender() throws Exception {

        final SyslogAppender appender = createAppender("udp", "bsd");
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);
        root.setAdditive(false);
        root.debug("This is a test message");
        final String str = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", str);
        assertTrue("Incorrect msg: " + str, str.endsWith("This is a test message\n"));
        assertTrue("Message not delivered via UDP", udpCount > 0);
        root.removeAppender(appender);
        appender.stop();
    }


    @Test
    public void testUDPStructuredAppender() throws Exception {
        final SyslogAppender appender = createAppender("udp", "RFC5424");
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);
        root.setAdditive(false);
        ThreadContext.put("loginId", "JohnDoe");
        ThreadContext.put("ipAddress", "192.168.0.120");
        ThreadContext.put("locale", Locale.US.getDisplayName());
        final StructuredDataMessage msg = new StructuredDataMessage("Transfer@18060", "Transfer Complete", "Audit");
        msg.put("ToAccount", "123456");
        msg.put("FromAccount", "123457");
        msg.put("Amount", "200.00");
        root.info(MarkerManager.getMarker("EVENT"), msg);
        final String str = list.poll(3, TimeUnit.SECONDS);
        assertNotNull("No event retrieved", str);
        assertTrue("Incorrect msg: " + str, str.endsWith(line1));
        assertTrue("Message not delivered via TCP", udpCount > 0);
        root.removeAppender(appender);
        appender.stop();
    }

    private SyslogAppender createAppender(final String protocol, final String format) {
        return SyslogAppender.createAppender("localhost", PORT, protocol, "-1", "Test", "true", "false", "LOCAL0", "Audit",
            "18060", "true", "RequestContext", "true", null, "TestApp", "Test", null, "ipAddress,loginId", null, format, null,
                null, null, null);
    }

    public static class UDPSocketServer extends Thread {
        private final DatagramSocket sock;
        private boolean shutdown = false;
        private Thread thread;

        public UDPSocketServer() throws IOException {
            this.sock = new DatagramSocket(PORTNUM);
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
                    final String str = new String(packet.getData(), 0, packet.getLength());
                    ++udpCount;
                    list.add(str);
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
        private Thread thread;

        public TCPSocketServer() throws IOException {
            this.sock = new ServerSocket(PORTNUM);
        }

        public void shutdown() {
            this.shutdown = true;
            thread.interrupt();
        }

        @Override
        public void run() {
            this.thread = Thread.currentThread();
            while (!shutdown) {
                try {
                    final byte[] buffer = new byte[4096];
                    final Socket socket = sock.accept();
                    socket.setSoLinger(true, 0);
                    final StringBuilder sb = new StringBuilder();
                    if (socket != null) {
                        final InputStream in = socket.getInputStream();
                        int i = in.read(buffer, 0, buffer.length);
                        while (i != -1) {
                            if (i < buffer.length) {
                                final String line = new String(buffer, 0, i);
                                ++tcpCount;
                                list.add(line);
                                i = in.read(buffer, 0, buffer.length);
                            } else if (i == 0) {
                                System.out.println("No data received");
                            } else {
                                System.out.println("Message too long");
                            }
                        }

                        socket.close();
                    }
                } catch (final Exception ex) {
                    if (!shutdown) {
                        System.out.println("Caught exception: " + ex.getMessage());
                    }
                }
            }

        }
    }

}
