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
        Assert.assertNotNull(loggerContextRule);
        Assert.assertNotNull(loggerContextRule.getConfiguration());
        final SocketAppender appender = loggerContextRule.getAppender("socket", SocketAppender.class);
        Assert.assertNotNull(appender);
        final TcpSocketManager manager = (TcpSocketManager) appender.getManager();
        Assert.assertNotNull(manager);
        final OutputStream outputStream = manager.getOutputStream();
        Assert.assertFalse(outputStream instanceof NullOutputStream);
        final SocketOptions socketOptions = manager.getSocketOptions();
        Assert.assertNotNull(socketOptions);
        final Socket socket = manager.getSocket();
        Assert.assertNotNull(socket);
        // Test config request
        Assert.assertEquals(false, socketOptions.isKeepAlive());
        Assert.assertEquals(false, socketOptions.isOobInline());
        Assert.assertEquals(false, socketOptions.isReuseAddress());
        Assert.assertEquals(false, socketOptions.isTcpNoDelay());
        Assert.assertEquals(Rfc1349TrafficClass.IPTOS_LOWCOST.value(),
                socketOptions.getActualTrafficClass().intValue());
        Assert.assertEquals(10000, socketOptions.getReceiveBufferSize().intValue());
        Assert.assertEquals(8000, socketOptions.getSendBufferSize().intValue());
        Assert.assertEquals(12345, socketOptions.getSoLinger().intValue());
        Assert.assertEquals(54321, socketOptions.getSoTimeout().intValue());
        // Test live socket
        Assert.assertEquals(false, socket.getKeepAlive());
        Assert.assertEquals(false, socket.getOOBInline());
        Assert.assertEquals(false, socket.getReuseAddress());
        Assert.assertEquals(false, socket.getTcpNoDelay());
        Assert.assertEquals(Rfc1349TrafficClass.IPTOS_LOWCOST.value(), socket.getTrafficClass());
        // Assert.assertEquals(10000, socket.getReceiveBufferSize());
        // This settings changes while we are running, so we cannot assert it.
        // Assert.assertEquals(8000, socket.getSendBufferSize());
        Assert.assertEquals(12345, socket.getSoLinger());
        Assert.assertEquals(54321, socket.getSoTimeout());
    }
}
