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
import java.net.SocketException;

import org.apache.logging.log4j.core.net.mock.MockSyslogServerFactory;
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

        sendAndCheckLegacyBSDMessage("This is a test message");
        sendAndCheckLegacyBSDMessage("This is a test message 2");
    }

    @Test
    public void testDefaultAppender() throws Exception {
        initTCPTestEnvironment(null);

        sendAndCheckLegacyBSDMessage("This is a test message");
        sendAndCheckLegacyBSDMessage("This is a test message 2");
    }

    @Test
    public void testTCPStructuredAppender() throws Exception {
        initTCPTestEnvironment("RFC5424");

        sendAndCheckStructuredMessage();
    }

    @Test
    public void testUDPAppender() throws Exception {
        initUDPTestEnvironment("bsd");

        sendAndCheckLegacyBSDMessage("This is a test message");
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

    protected void initUDPTestEnvironment(String messageFormat) throws SocketException {
        syslogServer = MockSyslogServerFactory.createUDPSyslogServer(1, PORTNUM);
        syslogServer.start();
        initAppender("udp", messageFormat);
    }

    protected void initTCPTestEnvironment(String messageFormat) throws IOException {
        syslogServer = MockSyslogServerFactory.createTCPSyslogServer(1, PORTNUM);
        syslogServer.start();
        initAppender("tcp", messageFormat);
    }

    protected void initAppender(String transportFormat, String messageFormat) {
        appender = createAppender(transportFormat, messageFormat);
        appender.start();
       initRootLogger(appender);
    }

    private SyslogAppender createAppender(final String protocol, final String format) {
        return SyslogAppender.createAppender("localhost", PORT, protocol, "-1", null, "Test", "true", "false", "LOCAL0", "Audit",
            "18060", "true", "RequestContext", null, null, includeNewLine, null, "TestApp", "Test", null, "ipAddress,loginId",
            null, format, null, null, null, null, null, null);
    }
}
