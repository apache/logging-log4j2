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
import java.util.Map;
import org.apache.log4j.layout.Log4j1XmlLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.TcpSocketManager;
import org.junit.Test;

/**
 * Tests configuring a Syslog appender.
 */
public class SocketAppenderConfigurationTest {

    private SocketAppender check(final Protocol expected, final Configuration configuration) {
        final Map<String, Appender> appenders = configuration.getAppenders();
        assertNotNull(appenders);
        final String appenderName = "socket";
        final Appender appender = appenders.get(appenderName);
        assertNotNull(appender, "Missing appender " + appenderName);
        final SocketAppender socketAppender = (SocketAppender) appender;
        @SuppressWarnings("resource")
        final TcpSocketManager manager = (TcpSocketManager) socketAppender.getManager();
        final String prefix = expected + ":";
        assertTrue(
                manager.getName().startsWith(prefix),
                () -> String.format("'%s' does not start with '%s'", manager.getName(), prefix));
        // Threshold
        final ThresholdFilter filter = (ThresholdFilter) socketAppender.getFilter();
        assertEquals(Level.DEBUG, filter.getLevel());
        // Host
        assertEquals("localhost", manager.getHost());
        // Port
        assertEquals(9999, manager.getPort());
        // Port
        assertEquals(100, manager.getReconnectionDelayMillis());
        return socketAppender;
    }

    private void checkProtocolPropertiesConfig(final Protocol expected, final String xmlPath) throws IOException {
        check(expected, TestConfigurator.configure(xmlPath).getConfiguration());
    }

    private SocketAppender checkProtocolXmlConfig(final Protocol expected, final String xmlPath) throws IOException {
        return check(expected, TestConfigurator.configure(xmlPath).getConfiguration());
    }

    @Test
    public void testProperties() throws Exception {
        checkProtocolXmlConfig(Protocol.TCP, "target/test-classes/log4j1-socket.properties");
    }

    @Test
    public void testPropertiesXmlLayout() throws Exception {
        final SocketAppender socketAppender =
                checkProtocolXmlConfig(Protocol.TCP, "target/test-classes/log4j1-socket-xml-layout.properties");
        assertTrue(socketAppender.getLayout() instanceof Log4j1XmlLayout);
    }

    @Test
    public void testXml() throws Exception {
        checkProtocolXmlConfig(Protocol.TCP, "target/test-classes/log4j1-socket.xml");
    }
}
