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
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SocketReconnectTest {
    private static final int SOCKET_PORT = 5514;

    private static final String CONFIG = "log4j-socket.xml";

    @BeforeClass
    public static void before() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
    }

    @Test
    public void testReconnect() throws Exception {
        TestSocketServer testServer = null;
        ExecutorService executor = null;
        Future<InputStream> futureIn;
        final InputStream in;

        try {
            executor = Executors.newSingleThreadExecutor();
            System.err.println("Initializing server");
            testServer = new TestSocketServer();
            futureIn = executor.submit(testServer);
            Thread.sleep(300);

            //System.err.println("Initializing logger");
            final Logger logger = LogManager.getLogger(SocketReconnectTest.class);

            String message = "Log #1";
            logger.error(message);

            BufferedReader reader = new BufferedReader(new InputStreamReader(futureIn.get()));
            assertEquals(message, reader.readLine());

            closeQuietly(testServer);

            message = "Log #2";
            logger.error(message);

            message = "Log #3";
            try {
                logger.error(message);
            } catch (final AppenderRuntimeException e) {
                // System.err.println("Caught expected exception");
            }

            //System.err.println("Re-initializing server");
            testServer = new TestSocketServer();
            futureIn = executor.submit(testServer);
            Thread.sleep(500);

            try {
                logger.error(message);
                reader = new BufferedReader(new InputStreamReader(futureIn.get()));
                assertEquals(message, reader.readLine());
            } catch (final AppenderRuntimeException e) {
                e.printStackTrace();
                fail("Unexpected Exception");
            }
            //System.err.println("Sleeping to demonstrate repeated re-connections");
            //Thread.sleep(5000);
        } finally {
            closeQuietly(testServer);
            closeQuietly(executor);
        }
    }


    private static class TestSocketServer implements Callable<InputStream> {
        private ServerSocket server;
        private Socket client;

        public InputStream call() throws Exception {
            server = new ServerSocket(SOCKET_PORT);
            client = server.accept();
            return client.getInputStream();
        }

        public void close() {
            closeQuietly(client);
            closeQuietly(server);
        }

        private void closeQuietly(final ServerSocket socket) {
            if (null != socket) {
                try {
                    socket.close();
                } catch (final IOException ignore) {
                }
            }
        }

        private void closeQuietly(final Socket socket) {
            if (null != socket) {
                try {
                    socket.close();
                } catch (final IOException ignore) {
                }
            }
        }
    }

    private static void closeQuietly(final ExecutorService executor) {
        if (null != executor) {
            executor.shutdownNow();
        }
    }

    private static void closeQuietly(final TestSocketServer testServer) {
        if (null != testServer) {
            testServer.close();
        }
    }
}
