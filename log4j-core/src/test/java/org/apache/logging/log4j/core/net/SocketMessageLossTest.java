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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore("Currently needs better port choosing support")
public class SocketMessageLossTest {
    private static final int SOCKET_PORT = AvailablePortFinder.getNextAvailable();

    private static final String CONFIG = "log4j-socket2.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void testSocket() throws Exception {
        TestSocketServer testServer;
        ExecutorService executor = null;
        Future<InputStream> futureIn;

        try {
            executor = Executors.newSingleThreadExecutor();
            System.err.println("Initializing server");
            testServer = new TestSocketServer();
            futureIn = executor.submit(testServer);

            //System.err.println("Initializing logger");
            final Logger logger = context.getLogger();

            String message = "Log #1";
            logger.error(message);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(futureIn.get()));
            assertEquals(message, reader.readLine());

            //System.err.println("Closing server");
            closeQuietly(testServer);
            assertTrue("Server not shutdown", testServer.server.isClosed());

            //System.err.println("Sleeping to ensure no race conditions");
            Thread.sleep(1000);

            message = "Log #2";
            try {
                logger.error(message);
                fail("Expected exception not thrown");
            } catch (final AppenderLoggingException e) {
                // An exception is expected.
            }

            message = "Log #3";
            try {
                logger.error(message);
                fail("Expected exception not thrown");
            } catch (final AppenderLoggingException e) {
                // An exception is expected.
            }
        } finally {
            closeQuietly(executor);
        }
    }


    private static class TestSocketServer implements Callable<InputStream> {
        private final ServerSocket server;
        private Socket client;

        public TestSocketServer() throws Exception {
            server = new ServerSocket(SOCKET_PORT);
        }

        @Override
        public InputStream call() throws Exception {
            client = server.accept();
            return client.getInputStream();
        }

        public void close() {
            closeQuietly(client);
            closeQuietly(server);
        }

        private static void closeQuietly(final ServerSocket socket) {
            if (null != socket) {
                try {
                    socket.close();
                } catch (final IOException ignore) {
                }
            }
        }

        private static void closeQuietly(final Socket socket) {
            if (null != socket) {
                try {
                    socket.setSoLinger(true, 0);
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
