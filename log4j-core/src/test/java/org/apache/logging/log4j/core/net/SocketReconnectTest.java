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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.apache.logging.log4j.util.Strings;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore("Currently needs better port choosing support")
public class SocketReconnectTest {
    private static final int SOCKET_PORT = AvailablePortFinder.getNextAvailable();

    private static final String CONFIG = "log4j-socket.xml";

    private static final String SHUTDOWN = "Shutdown" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR +
        "................................................................" + Strings.LINE_SEPARATOR;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void testReconnect() throws Exception {

        final List<String> list = new ArrayList<>();
        TestSocketServer server = new TestSocketServer(list);
        server.start();
        Thread.sleep(300);

        //System.err.println("Initializing logger");
        final Logger logger = context.getLogger();

        String message = "Log #1";
        logger.error(message);
        final String expectedHeader = "Header";

        String msg = null;
        String header = null;
        for (int i = 0; i < 5; ++i) {
            Thread.sleep(100);
            if (list.size() > 1) {
                header = list.get(0);
                msg = list.get(1);
                break;
            }
        }
        assertNotNull("No header", header);
        assertEquals(expectedHeader, header);
        assertNotNull("No message", msg);
        assertEquals(message, msg);

        logger.error(SHUTDOWN);
        server.join();

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
        assertTrue("No Exception thrown", exceptionCaught);
        message = "Log #3";


        server = new TestSocketServer(list);
        server.start();
        Thread.sleep(300);

        msg = null;
        header = null;
        logger.error(message);
        for (int i = 0; i < 5; ++i) {
            Thread.sleep(100);
            if (list.size() > 1) {
                header = list.get(0);
                msg = list.get(1);
                break;
            }
        }
        assertNotNull("No header", header);
        assertEquals(expectedHeader, header);
        assertNotNull("No message", msg);
        assertEquals(message, msg);
        logger.error(SHUTDOWN);
        server.join();
    }


    private static class TestSocketServer extends Thread {
        private volatile boolean shutdown = false;
        private final List<String> list;
        private Socket client;

        public TestSocketServer(final List<String> list) {
            this.list = list;
        }

        @Override
        public void run() {
            ServerSocket server = null;
            client = null;
            try {
                server = new ServerSocket(SOCKET_PORT);
                client = server.accept();
                while (!shutdown) {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    final String line = reader.readLine();
                    if (line.equals("Shutdown")) {
                        shutdown = true;
                    } else {
                        list.add(line);
                    }
                }
            } catch (final Exception ex) {
                ex.printStackTrace();
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (final Exception ex) {
                        System.out.println("Unable to close socket " + ex.getMessage());
                    }
                }
                if (server != null) {
                    try {
                        server.close();
                    } catch (final Exception ex) {
                        System.out.println("Unable to close server socket " + ex.getMessage());
                    }
                }
            }
        }
    }
}
