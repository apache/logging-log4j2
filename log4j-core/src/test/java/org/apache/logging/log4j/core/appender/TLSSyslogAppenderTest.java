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

import org.apache.logging.log4j.core.net.mock.MockSyslogServerFactory;
import org.apache.logging.log4j.core.net.ssl.*;
import org.junit.Test;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.util.List;

public class TLSSyslogAppenderTest extends SyslogAppenderTest{

    private SSLServerSocketFactory serverSocketFactory;
    private TLSSyslogMessageFormat messageFormat;
    private SSLConfiguration sslConfig;

    public TLSSyslogAppenderTest() throws SSLConfigurationException {
        initServerSocketFactory();
        root = ctx.getLogger("TLSSyslogAppenderTest");
    }

    @Test
    public void sendLargeLegacyBSDMessageOverTLS() throws IOException, InterruptedException {
        String prefix = "BEGIN";
        initTLSTestEnvironment(1, TLSSyslogMessageFormat.LEGACY_BSD);

        StringBuilder builder = new StringBuilder(prefix);
        for (int i = 0; i < 2 * 1024 * 2014; i++) {
            builder.append('a');
        }
        String message = builder.toString();
        sendAndCheckLegacyBSDMessage(message);
    }

    @Test
    public void sendLegacyBSDMessagesOverTLS() throws IOException, InterruptedException {
        int numberOfMessages = 100;
        initTLSTestEnvironment(100, TLSSyslogMessageFormat.LEGACY_BSD);

        List<String> sentMessages = TLSSyslogTestUtil.generateMessages(numberOfMessages, TLSSyslogMessageFormat.LEGACY_BSD);
        sendAndCheckLegacyBSDMessages(sentMessages);
    }

    @Test
    public void sendStructuredMessageOverTLS() throws InterruptedException, IOException {
        initTLSTestEnvironment(1, TLSSyslogMessageFormat.SYSLOG);

        sendAndCheckStructuredMessage();
    }

    @Test
    public void sendStructuredMessagesOverTLS() throws IOException, InterruptedException {
        int numberOfMessages = 100;
        initTLSTestEnvironment(100, TLSSyslogMessageFormat.SYSLOG);

        sendAndCheckStructuredMessages(numberOfMessages);
    }

    private void initServerSocketFactory() {
        KeyStoreConfiguration ksc = new KeyStoreConfiguration(TestConstants.KEYSTORE_FILE, TestConstants.KEYSTORE_PWD);
        TrustStoreConfiguration tsc = new TrustStoreConfiguration(TestConstants.TRUSTSTORE_FILE, TestConstants.TRUSTSTORE_PWD);
        sslConfig = SSLConfiguration.createSSLConfiguration(ksc, tsc);
        serverSocketFactory = sslConfig.getSSLServerSocketFactory();
    }

    private TLSSyslogAppender createAppender() {
        String format;

        if (messageFormat == TLSSyslogMessageFormat.LEGACY_BSD)
            format = "LEGACY_BSD";
        else
            format = "RFC5424";

        return TLSSyslogAppender.createAppender("localhost", PORT, sslConfig, "-1", null, "Test", "true", "false", "LOCAL0", "Audit",
                "18060", "true", "RequestContext", null, null, includeNewLine, null, "TestApp", "Test", null, "ipAddress,loginId",
                null, format, null, null, null, null, null, null);
    }

    private void initTLSTestEnvironment(int numberOfMessages, TLSSyslogMessageFormat messageFormat) throws IOException {
        this.messageFormat = messageFormat;
        SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(PORTNUM);

        syslogServer = MockSyslogServerFactory.createTLSSyslogServer(numberOfMessages, messageFormat, sslServerSocket);
        syslogServer.start();
        initAppender();
    }

    protected void initAppender() {
        appender = createAppender();
        appender.start();
        initRootLogger(appender);
    }
}
