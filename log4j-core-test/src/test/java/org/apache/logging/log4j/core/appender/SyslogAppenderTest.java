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
import java.net.SocketException;

import org.apache.logging.log4j.core.appender.SyslogAppender.Builder;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.test.net.mock.MockSyslogServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class SyslogAppenderTest extends SyslogAppenderTestBase {

    public SyslogAppenderTest() {
        root = ctx.getLogger("SyslogAppenderTest");
    }

    @Before
    public void setUp() {
        sentMessages.clear();
    }

    @After
    public void teardown() {
        removeAppenders();
        if (syslogServer != null) {
            syslogServer.shutdown();
        }
    }

    @Test
    public void testTCPAppender() throws Exception {
        initTCPTestEnvironment(null);

        sendAndCheckLegacyBsdMessage("This is a test message");
        sendAndCheckLegacyBsdMessage("This is a test message 2");
    }

    @Test
    public void testDefaultAppender() throws Exception {
        initTCPTestEnvironment(null);

        sendAndCheckLegacyBsdMessage("This is a test message");
        sendAndCheckLegacyBsdMessage("This is a test message 2");
    }

    @Test
    public void testTCPStructuredAppender() throws Exception {
        initTCPTestEnvironment("RFC5424");

        sendAndCheckStructuredMessage();
    }

    @Test
    public void testUDPAppender() throws Exception {
        initUDPTestEnvironment("bsd");

        sendAndCheckLegacyBsdMessage("This is a test message");
        root.removeAppender(appender);
        appender.stop();
    }

    @Test
    public void testUDPStructuredAppender() throws Exception {
        initUDPTestEnvironment("RFC5424");

        sendAndCheckStructuredMessage();
        root.removeAppender(appender);
        appender.stop();
    }

    protected void initUDPTestEnvironment(final String messageFormat) throws SocketException {
        syslogServer = MockSyslogServerFactory.createUDPSyslogServer();
        syslogServer.start();
        initAppender(Protocol.UDP, messageFormat, syslogServer.getLocalPort());
    }

    protected void initTCPTestEnvironment(final String messageFormat) throws IOException {
        syslogServer = MockSyslogServerFactory.createTCPSyslogServer();
        syslogServer.start();
        initAppender(Protocol.TCP, messageFormat, syslogServer.getLocalPort());
    }

    protected void initAppender(final Protocol protocol, final String messageFormat, final int port) {
        appender = createAppender(protocol, messageFormat, port);
        validate(appender);
        appender.start();
        initRootLogger(appender);
    }

    protected SyslogAppender createAppender(final Protocol protocol, final String format, final int port) {
        return newSyslogAppenderBuilder(protocol, format, includeNewLine, port).build();
    }

    protected Builder<?> newSyslogAppenderBuilder(final Protocol protocol, final String format, final boolean newLine,
            final int port) {
        // @formatter:off
        return SyslogAppender.newSyslogAppenderBuilder()
                .setPort(port)
                .setProtocol(protocol)
                .setReconnectDelayMillis(-1)
                .setName("TestApp")
                .setIgnoreExceptions(false)
                .setId("Audit")
                .setEnterpriseNumber(18060)
                .setMdcId("RequestContext")
                .setNewLine(newLine)
                .setAppName("TestApp")
                .setMsgId("Test")
                .setFormat(format);
        // @formatter:on
    }
}
