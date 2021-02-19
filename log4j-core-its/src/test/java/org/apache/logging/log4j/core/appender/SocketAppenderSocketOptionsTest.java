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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.logging.log4j.core.appender.SocketAppenderTest.TcpSocketTestServer;
import org.apache.logging.log4j.core.net.Rfc1349TrafficClass;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

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
        loggerContextRule = new LoggerContextRule("log4j-socket-options.xml");
    }

    @ClassRule
    public static final LoggerContextRule loggerContextRule;

    @AfterClass
    public static void afterClass() {
        if (tcpSocketTestServer != null) {
            tcpSocketTestServer.shutdown();
        }
    }

    @Test
    public void testSocketOptions() throws IOException {
        assertThat(loggerContextRule).isNotNull();
        assertThat(loggerContextRule.getConfiguration()).isNotNull();
        final SocketAppender appender = loggerContextRule.getAppender("socket", SocketAppender.class);
        assertThat(appender).isNotNull();
        final TcpSocketManager manager = (TcpSocketManager) appender.getManager();
        assertThat(manager).isNotNull();
        final OutputStream outputStream = manager.getOutputStream();
        assertThat(outputStream instanceof NullOutputStream).isFalse();
        final SocketOptions socketOptions = manager.getSocketOptions();
        assertThat(socketOptions).isNotNull();
        final Socket socket = manager.getSocket();
        assertThat(socket).isNotNull();
        // Test config request
        assertThat(socketOptions.isKeepAlive()).isEqualTo(false);
        assertThat(socketOptions.isOobInline()).isEqualTo(false);
        assertThat(socketOptions.isReuseAddress()).isEqualTo(false);
        assertThat(socketOptions.isTcpNoDelay()).isEqualTo(false);
        assertThat(socketOptions.getActualTrafficClass().intValue()).isEqualTo(Rfc1349TrafficClass.IPTOS_LOWCOST.value());
        assertThat(socketOptions.getReceiveBufferSize().intValue()).isEqualTo(10000);
        assertThat(socketOptions.getSendBufferSize().intValue()).isEqualTo(8000);
        assertThat(socketOptions.getSoLinger().intValue()).isEqualTo(12345);
        assertThat(socketOptions.getSoTimeout().intValue()).isEqualTo(54321);
        // Test live socket
        assertThat(socket.getKeepAlive()).isEqualTo(false);
        assertThat(socket.getOOBInline()).isEqualTo(false);
        assertThat(socket.getReuseAddress()).isEqualTo(false);
        assertThat(socket.getTcpNoDelay()).isEqualTo(false);
        // Assert.assertEquals(10000, socket.getReceiveBufferSize());
        // This settings changes while we are running, so we cannot assert it.
        // Assert.assertEquals(8000, socket.getSendBufferSize());
        assertThat(socket.getSoLinger()).isEqualTo(12345);
        assertThat(socket.getSoTimeout()).isEqualTo(54321);
    }

    @Test
    public void testSocketTrafficClass() throws IOException {
        Assume.assumeTrue("Run only on Java 7", System.getProperty("java.specification.version").equals("1.7"));
        Assume.assumeFalse("Do not run on Travis CI", "true".equals(System.getenv("TRAVIS")));
        final SocketAppender appender = loggerContextRule.getAppender("socket", SocketAppender.class);
        final TcpSocketManager manager = (TcpSocketManager) appender.getManager();
        final Socket socket = manager.getSocket();
        assertThat(socket.getTrafficClass()).isEqualTo(Rfc1349TrafficClass.IPTOS_LOWCOST.value());
    }
}
