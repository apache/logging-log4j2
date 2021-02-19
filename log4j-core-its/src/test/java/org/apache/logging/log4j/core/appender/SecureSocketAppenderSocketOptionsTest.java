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
import javax.net.ssl.SSLServerSocketFactory;
import org.apache.logging.log4j.core.appender.SocketAppenderTest.TcpSocketTestServer;
import org.apache.logging.log4j.core.net.Rfc1349TrafficClass;
import org.apache.logging.log4j.core.net.SocketOptions;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.apache.logging.log4j.core.net.ssl.KeyStoreConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;
import org.apache.logging.log4j.core.net.ssl.TestConstants;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

public class SecureSocketAppenderSocketOptionsTest {

    private static final int PORT;
    private static TcpSocketTestServer tcpSocketTestServer;

    private static SSLServerSocketFactory serverSocketFactory;
    private static SslConfiguration sslConfiguration;

    static {
        PORT = AvailablePortFinder.getNextAvailable();
        System.setProperty("SecureSocketAppenderSocketOptionsTest.port", Integer.toString(PORT));
        try {
            initServerSocketFactory();
            tcpSocketTestServer = new TcpSocketTestServer(serverSocketFactory.createServerSocket(PORT));
            tcpSocketTestServer.start();
            loggerContextRule = new LoggerContextRule("log4j-ssl-socket-options.xml");
        } catch (IOException | StoreConfigurationException e) {
            throw new IllegalStateException(e);
        }

    }

    @ClassRule
    public static final LoggerContextRule loggerContextRule;

    @AfterClass
    public static void afterClass() {
        if (tcpSocketTestServer != null) {
            tcpSocketTestServer.shutdown();
        }
    }

    public static void initServerSocketFactory() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = KeyStoreConfiguration.createKeyStoreConfiguration(
                TestConstants.KEYSTORE_FILE, // file
                TestConstants.KEYSTORE_PWD(),  // password
                null, // passwordEnvironmentVariable
                null, // passwordFile
                null, // key store type
                null); // algorithm

        final TrustStoreConfiguration tsc = TrustStoreConfiguration.createKeyStoreConfiguration(
                TestConstants.TRUSTSTORE_FILE, // file
                TestConstants.TRUSTSTORE_PWD(), // password
                null, // passwordEnvironmentVariable
                null, // passwordFile
                null, // key store type
                null); // algorithm
        sslConfiguration = SslConfiguration.createSSLConfiguration(null, ksc, tsc);
        serverSocketFactory = sslConfiguration.getSslServerSocketFactory();
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
        assertThat(socketOptions.isKeepAlive()).isFalse();
        assertThat(socketOptions.isOobInline()).isEqualTo(null);
        assertThat(socketOptions.isReuseAddress()).isFalse();
        assertThat(socketOptions.isTcpNoDelay()).isFalse();
        assertThat(socketOptions.getActualTrafficClass().intValue()).isEqualTo(Rfc1349TrafficClass.IPTOS_LOWCOST.value());
        assertThat(socketOptions.getReceiveBufferSize().intValue()).isEqualTo(10000);
        assertThat(socketOptions.getSendBufferSize().intValue()).isEqualTo(8000);
        assertThat(socketOptions.getSoLinger().intValue()).isEqualTo(12345);
        assertThat(socketOptions.getSoTimeout().intValue()).isEqualTo(54321);
        // Test live socket
        assertThat(socket.getKeepAlive()).isFalse();
        assertThat(socket.getReuseAddress()).isFalse();
        assertThat(socket.getTcpNoDelay()).isFalse();
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
