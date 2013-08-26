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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

public class SocketTest {
    private static final int SOCKET_PORT = 5514;

    private static final String CONFIG = "log4j-socket.xml";

    @BeforeClass
    public static void before() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
    }

    @Test
    public void testConnect() throws Exception {
        System.err.println("Initializing logger");
        Logger logger = null;
        try {
            logger = LogManager.getLogger(SocketTest.class);
        } catch (final NullPointerException e) {
            fail("Unexpected exception; should not occur until first logging statement " + e.getMessage());
        }

        final String message = "Log #1";
        try {
            logger.error(message);
            fail("Expected exception not thrown");
        } catch (final AppenderLoggingException e) {
            //System.err.println("Expected exception here, but already errored out when initializing logger");
        }
    }

    private static class TestSocketServer implements Callable<InputStream> {
        private ServerSocket server;
        private Socket client;

        @Override
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
