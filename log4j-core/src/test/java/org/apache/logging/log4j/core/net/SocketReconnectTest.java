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
package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@Disabled("Currently needs better port choosing support")
public class SocketReconnectTest {
    private static final int SOCKET_PORT1 = AvailablePortFinder.getNextAvailable();
    private static final int SOCKET_PORT2 = AvailablePortFinder.getNextAvailable();

    private static final String CONFIG = "log4j-socket.xml";

    private static final String SHUTDOWN = "Shutdown" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR;

    public static LocalHostResolver resolver = new LocalHostResolver();

    private static LoggerContext loggerContext;

    private static final List<String> list = new ArrayList<>();
    private static int[] ports;
    private static TestSocketServer server1;
    private static TestSocketServer server2;
    private static Logger logger;


    @BeforeAll
    public static void beforeClass() throws IOException, InterruptedException {
        server1 = new TestSocketServer(0);
        server2 = new TestSocketServer(0);
        server1.start();
        server2.start();
        Thread.sleep(100);
        ports = new int[] { server1.getPort(), server2.getPort()};
        resolver.ports = ports;
        TcpSocketManager.setHostResolver(resolver);
        loggerContext = Configurator.initialize("SocketReconnectTest", SocketReconnectTest.class.getClassLoader(),
                CONFIG);
        logger = LogManager.getLogger(SocketReconnectTest.class);
        server1.shutdown();
        server1.join();
        server2.shutdown();
        server2.join();
        server1 = null;
        server2 = null;
        Thread.sleep(100);
        list.clear();
    }

    @AfterAll
    public static void afterClass() {
        Configurator.shutdown(loggerContext);
    }

    @AfterEach
    public void after() throws InterruptedException {
        if (server1 != null) {
            server1.shutdown();
            server1.join();
        }
        if (server2 != null) {
            server2.shutdown();
            server2.join();
        }
        server1 = null;
        server2 = null;
        Thread.sleep(300);
    }

    @Test
    public void testReconnect() throws Exception {
        list.clear();
        resolver.ports = new int[] {ports[0]};
        server1 = new TestSocketServer(ports[0]);
        server1.start();
        Thread.sleep(200);
        String message = "Log #1";
        String msg = null;
        for (int i = 0; i < 5; ++i) {
            logger.error(message);
            Thread.sleep(100);
            if (list.size() > 0) {
                msg = list.get(0);
                if (msg != null) {
                    break;
                }
            }
        }
        assertNotNull(msg, "No message");
        assertEquals(message, msg);

        logger.error(SHUTDOWN);
        server1.join();

        list.clear();

        message = "Log #2";
        boolean exceptionCaught = false;

        for (int i = 0; i < 100; ++i) {
            try {
                logger.error(message);
            } catch (final AppenderLoggingException e) {
                exceptionCaught = true;
                break;
                // System.err.println("Caught expected exception");
            }
        }
        assertTrue(exceptionCaught, "No Exception thrown");
        message = "Log #3";


        server1 = new TestSocketServer(ports[0]);
        server1.start();
        Thread.sleep(300);

        msg = null;
        for (int i = 0; i < 5; ++i) {
            logger.error(message);
            Thread.sleep(100);
            if (list.size() > 0) {
                msg = list.get(0);
                if (msg != null) {
                    break;
                }
            }
        }
        assertNotNull(msg, "No message");
        assertEquals(message, msg);
        logger.error(SHUTDOWN);
        server1.join();
    }

    @Test
    public void testFailover() throws Exception {
        list.clear();
        server1 = new TestSocketServer(ports[0]);
        server2 = new TestSocketServer(ports[1]);
        resolver.ports = ports;
        server1.start();
        server2.start();
        Thread.sleep(100);

        String message = "Log #1";

        String msg = null;
        for (int i = 0; i < 5; ++i) {
            logger.error(message);
            Thread.sleep(100);
            if (list.size() > 0) {
                msg = list.get(0);
                if (msg != null) {
                    break;
                }
            }
        }
        assertNotNull(msg, "No message");
        assertEquals(message, msg);

        server1.shutdown();
        server1.join();

        list.clear();

        message = "Log #2";
        for (int i = 0; i < 5; ++i) {
            logger.error(message);
            Thread.sleep(100);
            if (list.size() > 0) {
                msg = list.get(0);
                if (msg != null) {
                    break;
                }
            }
        }
        assertNotNull(msg, "No message");
        assertEquals(message, msg);

        server2.shutdown();
        server2.join();
    }


    private static class TestSocketServer extends Thread {
        private volatile boolean shutdown = false;
        private volatile boolean started = false;
        private volatile Socket client;
        private final int port;
        private ServerSocket server;

        public TestSocketServer(int port) throws IOException {
            this.port = port;
            server = new ServerSocket(port);
        }

        public int getPort() {
            return port == 0 ? server.getLocalPort() : port;
        }

        public void shutdown() {
            if (!shutdown) {
                shutdown = true;
                if (server != null && server.isBound()) {
                    try {
                        if (client != null) {
                            Socket serverClient = client;
                            client = null;
                            serverClient.shutdownInput();
                            serverClient.shutdownOutput();
                            serverClient.setSoLinger(true, 0);
                            serverClient.close();
                        }
                        ServerSocket serverSocket = server;
                        server = null;
                        serverSocket.close();
                    } catch (Exception ex) {
                        System.out.println("Unable to send shutdown message");
                        ex.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void run() {
            client = null;
            try {
                client = server.accept();
                started = true;
                while (!shutdown) {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    final String line = reader.readLine();
                    if (line == null || line.equals("Shutdown")) {
                        shutdown = true;
                    } else {
                        list.add(line);
                    }
                }
            } catch (final SocketException ex) {
                if (!shutdown) {
                    ex.printStackTrace();
                }
            } catch (final Exception ex) {
                ex.printStackTrace();
            } finally {
                if (client != null && !client.isClosed()) {
                    try {
                        client.setSoLinger(true, 0);
                        client.shutdownOutput();
                        client.close();
                    } catch (final Exception ex) {
                        System.out.println("Unable to close socket: " + ex.getMessage());
                    }
                }
                if (server != null && !server.isClosed()) {
                    try {
                        server.close();
                    } catch (final Exception ex) {
                        System.out.println("Unable to close server socket: " + ex.getMessage());
                    }
                }
            }
        }
    }

    private static class LocalHostResolver extends TcpSocketManager.HostResolver {
        public volatile int[] ports;

        @Override
        public List<InetSocketAddress> resolveHost(String host, int port) throws UnknownHostException {
            int[] socketPorts = ports;
            List<InetSocketAddress> socketAddresses = new ArrayList<>(ports.length);
            InetAddress addr = InetAddress.getLocalHost();
            for (int i = 0; i < socketPorts.length; ++i){
                socketAddresses.add(new InetSocketAddress(addr, socketPorts[i]));
            }
            return socketAddresses;
        }
    }
}
