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
package org.apache.log4j.config;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.SyslogAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.WritesSystemProperty;

/**
 * Tests configuring a Syslog appender.
 */
@UsingStatusListener
@WritesSystemProperty
public class SyslogAppenderConfigurationTest {

    private static ServerSocket tcpSocket;

    @BeforeAll
    static void setup() throws IOException {
        // TCP appenders log an error if there is no server socket
        tcpSocket = new ServerSocket(0);
        System.setProperty("syslog.port", Integer.toString(tcpSocket.getLocalPort()));
    }

    @AfterAll
    static void cleanup() throws IOException {
        System.clearProperty("syslog.port");
        tcpSocket.close();
    }

    private void check(final Protocol expected, final Configuration configuration) {
        final Map<String, Appender> appenders = configuration.getAppenders();
        assertNotNull(appenders);
        final String appenderName = "syslog";
        final Appender appender = appenders.get(appenderName);
        assertNotNull(appender, "Missing appender " + appenderName);
        final SyslogAppender syslogAppender = (SyslogAppender) appender;
        @SuppressWarnings("resource")
        final AbstractSocketManager manager = syslogAppender.getManager();
        final String prefix = expected + ":";
        assertTrue(
                manager.getName().startsWith(prefix),
                () -> String.format("'%s' does not start with '%s'", manager.getName(), prefix));
        // Threshold
        final ThresholdFilter filter = (ThresholdFilter) syslogAppender.getFilter();
        assertEquals(Level.DEBUG, filter.getLevel());
        // Host
        assertEquals("localhost", manager.getHost());
        // Port
        assertEquals(tcpSocket.getLocalPort(), manager.getPort());
    }

    private void checkProtocolPropertiesConfig(final Protocol expected, final String xmlPath) throws IOException {
        check(expected, TestConfigurator.configure(xmlPath).getConfiguration());
    }

    private void checkProtocolXmlConfig(final Protocol expected, final String xmlPath) throws IOException {
        check(expected, TestConfigurator.configure(xmlPath).getConfiguration());
    }

    @Test
    public void testPropertiesProtocolDefault() throws Exception {
        checkProtocolPropertiesConfig(Protocol.TCP, "target/test-classes/log4j1-syslog-protocol-default.properties");
    }

    @Test
    public void testPropertiesProtocolTcp() throws Exception {
        checkProtocolPropertiesConfig(Protocol.TCP, "target/test-classes/log4j1-syslog-protocol-tcp.properties");
    }

    @Test
    public void testPropertiesProtocolUdp() throws Exception {
        checkProtocolPropertiesConfig(Protocol.UDP, "target/test-classes/log4j1-syslog-protocol-udp.properties");
    }

    @Test
    public void testXmlProtocolDefault() throws Exception {
        checkProtocolXmlConfig(Protocol.TCP, "target/test-classes/log4j1-syslog.xml");
    }

    @Test
    public void testXmlProtocolTcp() throws Exception {
        checkProtocolXmlConfig(Protocol.TCP, "target/test-classes/log4j1-syslog-protocol-tcp.xml");
    }

    @Test
    public void testXmlProtocolUdp() throws Exception {
        checkProtocolXmlConfig(Protocol.UDP, "target/test-classes/log4j1-syslog-protocol-udp.xml");
    }
}
