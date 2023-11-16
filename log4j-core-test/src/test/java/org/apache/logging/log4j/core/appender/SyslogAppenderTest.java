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

import static org.apache.logging.log4j.core.test.net.ssl.TlsSyslogMessageFormat.LEGACY_BSD;
import static org.apache.logging.log4j.core.test.net.ssl.TlsSyslogMessageFormat.SYSLOG;

import java.io.IOException;
import java.net.SocketException;
import org.apache.logging.log4j.core.appender.SyslogAppender.Builder;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.test.net.mock.MockSyslogServerFactory;
import org.apache.logging.log4j.core.test.net.ssl.TlsSyslogMessageFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class SyslogAppenderTest extends SyslogAppenderTestBase {

    public SyslogAppenderTest() {
        root = ctx.getLogger("SyslogAppenderTest");
    }

    @BeforeEach
    public void setUp() {
        sentMessages.clear();
    }

    @AfterEach
    public void teardown() {
        removeAppenders();
        if (syslogServer != null) {
            syslogServer.shutdown();
        }
    }

    @Test
    public void testTCPAppender() throws Exception {
        initTCPTestEnvironment(LEGACY_BSD);

        sendAndCheckLegacyBsdMessage("This is a test message");
        sendAndCheckLegacyBsdMessage("This is a test message 2");
    }

    @Test
    public void testDefaultAppender() throws Exception {
        initTCPTestEnvironment(LEGACY_BSD);

        sendAndCheckLegacyBsdMessage("This is a test message");
        sendAndCheckLegacyBsdMessage("This is a test message 2");
    }

    @Test
    public void testTCPStructuredAppender() throws Exception {
        initTCPTestEnvironment(SYSLOG);

        sendAndCheckStructuredMessage();
    }

    @Test
    public void testUDPAppender() throws Exception {
        initUDPTestEnvironment(LEGACY_BSD);

        sendAndCheckLegacyBsdMessage("This is a test message");
        root.removeAppender(appender);
        appender.stop();
    }

    @Test
    public void testUDPStructuredAppender() throws Exception {
        initUDPTestEnvironment(SYSLOG);

        sendAndCheckStructuredMessage();
        root.removeAppender(appender);
        appender.stop();
    }

    protected void initUDPTestEnvironment(final TlsSyslogMessageFormat messageFormat) throws SocketException {
        syslogServer = MockSyslogServerFactory.createUDPSyslogServer();
        syslogServer.start();
        initAppender(Protocol.UDP, messageFormat, syslogServer.getLocalPort());
    }

    protected void initTCPTestEnvironment(final TlsSyslogMessageFormat messageFormat) throws IOException {
        syslogServer = MockSyslogServerFactory.createTCPSyslogServer();
        syslogServer.start();
        initAppender(Protocol.TCP, messageFormat, syslogServer.getLocalPort());
    }

    protected void initAppender(final Protocol protocol, final TlsSyslogMessageFormat messageFormat, final int port) {
        appender = createAppender(protocol, messageFormat, port);
        validate(appender);
        appender.start();
        initRootLogger(appender);
    }

    protected SyslogAppender createAppender(
            final Protocol protocol, final TlsSyslogMessageFormat format, final int port) {
        return newSyslogAppenderBuilder(protocol, format, includeNewLine, port).build();
    }

    protected Builder<?> newSyslogAppenderBuilder(
            final Protocol protocol, final TlsSyslogMessageFormat format, final boolean newLine, final int port) {
        // @formatter:off
        return SyslogAppender.newSyslogAppenderBuilder()
                .setHost("localhost")
                .setPort(port)
                .setProtocol(protocol)
                .setReconnectDelayMillis(-1)
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
                .setFormat(format == SYSLOG ? SyslogAppender.RFC5424 : null)
                .setAdvertise(false);
        // @formatter:on
    }
}
