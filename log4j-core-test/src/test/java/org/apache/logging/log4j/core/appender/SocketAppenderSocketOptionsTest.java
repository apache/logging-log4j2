/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.SocketAppenderTest.TcpSocketTestServer;
import org.apache.logging.log4j.core.net.Rfc1349TrafficClass;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.test.AvailablePortFinder;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class SocketAppenderSocketOptionsTest {

    private static final int PORT;
    private static TcpSocketTestServer tcpSocketTestServer;

    static {
        PORT = AvailablePortFinder.getNextAvailable();
        System.setProperty("SocketAppenderSocketOptionsTest.port", Integer.toString(PORT));
        try {
            tcpSocketTestServer = new TcpSocketTestServer(PORT);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        tcpSocketTestServer.start();
    }

    @AfterAll
    public static void afterAll() {
        if (tcpSocketTestServer != null) {
            tcpSocketTestServer.shutdown();
        }
    }

    @Test
    @LoggerContextSource("log4j-socket-options.xml")
    public void testSocketOptions(final LoggerContext loggerContext) throws IOException {
        assertNotNull(loggerContext);
        assertNotNull(loggerContext.getConfiguration());
        final SocketAppender appender = loggerContext.getConfiguration().getAppender("socket");

        assertNotNull(appender);
        final TcpSocketManager manager = (TcpSocketManager) appender.getManager();
        assertNotNull(manager);
        final OutputStream outputStream = manager.getOutputStream();
        assertFalse(outputStream instanceof NullOutputStream);
        final SocketOptions socketOptions = manager.getSocketOptions();
        assertNotNull(socketOptions);
        final Socket socket = manager.getSocket();
        assertNotNull(socket);
        // Test config request
        assertFalse(socketOptions.isKeepAlive());
        assertFalse(socketOptions.isOobInline());
        assertFalse(socketOptions.isReuseAddress());
        assertFalse(socketOptions.isTcpNoDelay());
        assertEquals(
                Rfc1349TrafficClass.IPTOS_LOWCOST.value(),
                socketOptions.getActualTrafficClass().intValue());
        assertEquals(10000, socketOptions.getReceiveBufferSize().intValue());
        assertEquals(8000, socketOptions.getSendBufferSize().intValue());
        assertEquals(12345, socketOptions.getSoLinger().intValue());
        assertEquals(54321, socketOptions.getSoTimeout().intValue());
        // Test live socket
        assertFalse(socket.getKeepAlive());
        assertFalse(socket.getOOBInline());
        assertFalse(socket.getReuseAddress());
        assertFalse(socket.getTcpNoDelay());
        // assertEquals(10000, socket.getReceiveBufferSize());
        // This settings changes while we are running, so we cannot assert it.
        // assertEquals(8000, socket.getSendBufferSize());
        assertEquals(12345, socket.getSoLinger());
        assertEquals(54321, socket.getSoTimeout());
    }

    @Test
    @LoggerContextSource("log4j-socket-options.xml")
    public void testSocketTrafficClass(final LoggerContext loggerContext) throws IOException {
        assumeTrue(System.getProperty("java.specification.version").equals("1.7"), "Run only on Java 7");
        assumeFalse("true".equals(System.getenv("TRAVIS")), "Do not run on Travis CI");
        final SocketAppender appender = loggerContext.getConfiguration().getAppender("socket");
        final TcpSocketManager manager = (TcpSocketManager) appender.getManager();
        final Socket socket = manager.getSocket();
        assertEquals(Rfc1349TrafficClass.IPTOS_LOWCOST.value(), socket.getTrafficClass());
    }
}
