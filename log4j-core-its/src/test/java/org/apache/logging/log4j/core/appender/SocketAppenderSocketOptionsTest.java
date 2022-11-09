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

import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.core.appender.SocketAppenderTest.TcpSocketTestServer;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Rfc1349TrafficClass;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.test.junit.AllocatePorts;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.plugins.Named;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@AllocatePorts("SocketAppenderSocketOptionsTest.port")
public class SocketAppenderSocketOptionsTest {

    private TcpSocketTestServer server;

    @BeforeEach
    void setUp() throws IOException {
        int port = Integer.getInteger("SocketAppenderSocketOptionsTest.port");
        server = new TcpSocketTestServer(port);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

    @Test
    @LoggerContextSource("log4j-socket-options.xml")
    public void testSocketOptions(@Named("socket") final SocketAppender appender) throws IOException {
        final AbstractSocketManager abstractSocketManager = appender.getManager();
        assertThat(abstractSocketManager)
                .isNotNull()
                .isInstanceOf(TcpSocketManager.class);
        final TcpSocketManager manager = (TcpSocketManager) abstractSocketManager;
        assertThat(manager.getOutputStream())
                .isNotNull()
                .isInstanceOf(NullOutputStream.class);
        final SocketOptions socketOptions = manager.getSocketOptions();
        assertNotNull(socketOptions);
        final Socket socket = manager.getSocket();
        assertNotNull(socket);
        // Test config request
        assertEquals(false, socketOptions.isKeepAlive());
        assertEquals(false, socketOptions.isOobInline());
        assertEquals(false, socketOptions.isReuseAddress());
        assertEquals(false, socketOptions.isTcpNoDelay());
        assertEquals(Rfc1349TrafficClass.IPTOS_LOWCOST.value(), socketOptions.getActualTrafficClass().intValue());
        assertEquals(10000, socketOptions.getReceiveBufferSize().intValue());
        assertEquals(8000, socketOptions.getSendBufferSize().intValue());
        assertEquals(12345, socketOptions.getSoLinger().intValue());
        assertEquals(54321, socketOptions.getSoTimeout().intValue());
        // Test live socket
        assertFalse(socket.getKeepAlive());
        assertFalse(socket.getOOBInline());
        assertFalse(socket.getReuseAddress());
        assertFalse(socket.getTcpNoDelay());
        // Assert.assertEquals(10000, socket.getReceiveBufferSize());
        // This settings changes while we are running, so we cannot assert it.
        // Assert.assertEquals(8000, socket.getSendBufferSize());
        assertEquals(12345, socket.getSoLinger());
        assertEquals(54321, socket.getSoTimeout());
    }
}
