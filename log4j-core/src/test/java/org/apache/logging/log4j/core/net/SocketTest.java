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

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore("Currently needs better port choosing support")
public class SocketTest {
    private static final int SOCKET_PORT = AvailablePortFinder.getNextAvailable();

    private static final String CONFIG = "log4j-socket.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void testConnect() throws Exception {
        // TODO: there's a JUnit rule that simplifies this (matt)
        System.err.println("Initializing logger");
        Logger logger = null;
        try {
            logger = context.getLogger();
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
                    socket.close();
                } catch (final IOException ignore) {
                }
            }
        }
    }

}
