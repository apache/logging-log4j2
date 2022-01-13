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
package org.apache.log4j.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Protocol;
import org.junit.Test;

/**
 * Class Description goes here.
 */
public class SyslogAppenderConfigurationTest {

    private void checkProtocol(final Protocol expected, final Configuration configuration) {
        final Map<String, Appender> appenders = configuration.getAppenders();
        assertNotNull(appenders);
        final String appenderName = "syslog";
        final Appender appender = appenders.get(appenderName);
        assertNotNull(appender, "Missing appender " + appenderName);
        final SocketAppender socketAppender = (SocketAppender) appender;
        @SuppressWarnings("resource")
        final AbstractSocketManager manager = socketAppender.getManager();
        assertTrue(manager.getName().startsWith(expected + ":"));
    }

    private void checkProtocolPropertiesConfig(final Protocol expected, final String xmlPath) throws IOException {
        checkProtocol(expected, TestConfigurator.configure(xmlPath).getConfiguration());
    }

    private void checkProtocolXmlConfig(final Protocol expected, final String xmlPath) throws IOException {
        checkProtocol(expected, TestConfigurator.configure(xmlPath).getConfiguration());
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
