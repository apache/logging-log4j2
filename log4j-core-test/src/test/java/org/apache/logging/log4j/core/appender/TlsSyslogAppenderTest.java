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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.ssl.KeyStoreConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.StoreConfigurationException;
import org.apache.logging.log4j.core.net.ssl.TestConstants;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfiguration;
import org.apache.logging.log4j.core.test.net.mock.MockSyslogServerFactory;
import org.apache.logging.log4j.core.test.net.ssl.TlsSyslogMessageFormat;
import org.apache.logging.log4j.core.test.net.ssl.TlsSyslogTestUtil;
import org.junit.Test;

public class TlsSyslogAppenderTest extends SyslogAppenderTest {

    private SSLServerSocketFactory serverSocketFactory;
    private TlsSyslogMessageFormat messageFormat;
    private SslConfiguration sslConfiguration;

    public TlsSyslogAppenderTest() throws StoreConfigurationException {
        initServerSocketFactory();
        root = ctx.getLogger("TLSSyslogAppenderTest");
    }

    @Test
    public void sendLargeLegacyBsdMessageOverTls() throws IOException, InterruptedException {
        final String prefix = "BEGIN";
        initTlsTestEnvironment(1, TlsSyslogMessageFormat.LEGACY_BSD);

        final char[] msg = new char[2 * 1024 * 2014 + prefix.length()];
        Arrays.fill(msg, 'a');
        System.arraycopy(prefix.toCharArray(), 0, msg, 0, prefix.length());
        sendAndCheckLegacyBsdMessage(new String(msg));
    }

    @Test
    public void sendLegacyBsdMessagesOverTls() throws IOException, InterruptedException {
        final int numberOfMessages = 100;
        initTlsTestEnvironment(numberOfMessages, TlsSyslogMessageFormat.LEGACY_BSD);
        final List<String> generatedMessages = TlsSyslogTestUtil.generateMessages(numberOfMessages, TlsSyslogMessageFormat.LEGACY_BSD);
        sendAndCheckLegacyBsdMessages(generatedMessages);
    }

    @Test
    public void sendStructuredMessageOverTls() throws InterruptedException, IOException {
        initTlsTestEnvironment(1, TlsSyslogMessageFormat.SYSLOG);

        sendAndCheckStructuredMessage();
    }

    @Test
    public void sendStructuredMessagesOverTls() throws IOException, InterruptedException {
        final int numberOfMessages = 100;
        initTlsTestEnvironment(numberOfMessages, TlsSyslogMessageFormat.SYSLOG);
        sendAndCheckStructuredMessages(numberOfMessages);
    }

    private void initServerSocketFactory() throws StoreConfigurationException {
        final KeyStoreConfiguration ksc = new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE, TestConstants.KEYSTORE_PWD(), null, null);
        final TrustStoreConfiguration tsc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, TestConstants.TRUSTSTORE_PWD(), null, null);
        sslConfiguration = SslConfiguration.createSSLConfiguration(null, ksc, tsc);
        serverSocketFactory = sslConfiguration.getSslServerSocketFactory();
    }

    private SyslogAppender createAppender(final int port) {
        String format;

        if (messageFormat == TlsSyslogMessageFormat.LEGACY_BSD) {
            format = "LEGACY_BSD";
        } else {
            format = "RFC5424";
        }

        return SyslogAppender.newSyslogAppenderBuilder()
                .setHost("localhost")
                .setPort(port)
                .setProtocol(Protocol.SSL)
                .setSslConfiguration(sslConfiguration)
                .setImmediateFail(true)
                .setName("Test")
                .setImmediateFlush(true)
                .setIgnoreExceptions(false)
                .setFacility(Facility.LOCAL0)
                .setId("Audit")
                .setEnterpriseNumber("18060")
                .setIncludeMdc(true)
                .setMdcId("RequestContext")
                .setNewLine(includeNewLine)
                .setAppName("TestApp")
                .setMsgId("Test")
                .setIncludes("ipAddress,loginId")
                .setFormat(format)
                .setAdvertise(false)
                .build();
    }

    private void initTlsTestEnvironment(final int numberOfMessages, final TlsSyslogMessageFormat messageFormat) throws IOException {
        this.messageFormat = messageFormat;
        final SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(0);

        syslogServer = MockSyslogServerFactory.createTLSSyslogServer(numberOfMessages, messageFormat, sslServerSocket);
        syslogServer.start();
        initAppender(syslogServer.getLocalPort());
    }

    protected void initAppender(final int port) {
        appender = createAppender(port);
        validate(appender);
        appender.start();
        initRootLogger(appender);
    }
}
