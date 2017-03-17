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
package org.apache.logging.log4j.core.net.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.ssl.KeyStoreConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;
import org.apache.logging.log4j.core.net.ssl.TestConstants;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class SslXmlSocketServerTest extends AbstractSocketServerTest {

    private static TcpSocketServer<InputStream> server;

    private static SslConfiguration sslConfiguration;

    private static void initServerSocketFactory() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE,
                TestConstants.KEYSTORE_PWD, TestConstants.KEYSTORE_TYPE, null);
        final TrustStoreConfiguration tsc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE,
                TestConstants.TRUSTSTORE_PWD, null, null);
        sslConfiguration = SslConfiguration.createSSLConfiguration(null, ksc, tsc);
    }

    @Override
    protected SocketAppender createSocketAppender(final Filter socketFilter,
            final Layout<? extends Serializable> socketLayout) {
        // @formatter:off
        return SocketAppender.newBuilder()
                .withProtocol(this.protocol)
                .withHost("localhost")
                .withPort(this.port)
                .withReconnectDelayMillis(-1)
                .withName("test")
                .withImmediateFlush(true)
                .withImmediateFail(false)
                .withIgnoreExceptions(false)
                .withLayout(socketLayout)
                .withFilter(socketFilter)
                .withSslConfiguration(sslConfiguration)
                .build();
        // @formatter:on        
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        (LoggerContext.getContext(false)).reconfigure();
        initServerSocketFactory();
        // Use a large buffer just to test the code, the UDP test uses a tiny buffer
        server = new SecureTcpSocketServer<>(PORT_NUM, new XmlInputStreamLogEventBridge(1024 * 100,
                Charset.defaultCharset()), sslConfiguration);
        thread = server.startNewThread();
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            server.shutdown();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        try {
            thread.join();
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    public SslXmlSocketServerTest() {
        super(Protocol.SSL, PORT, false);
    }

    @Override
    protected Layout<String> createLayout() {
        return super.createXmlLayout();
    }

}
