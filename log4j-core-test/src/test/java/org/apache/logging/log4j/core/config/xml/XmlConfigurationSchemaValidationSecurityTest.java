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
package org.apache.logging.log4j.core.config.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Verifies that the strict-mode schema validation performed by {@link XmlConfiguration} does not resolve external
 * entities, matching the entity restrictions already applied to its {@code DocumentBuilderFactory}.
 */
@Tag("functional")
@Tag("security")
class XmlConfigurationSchemaValidationSecurityTest {

    @Test
    @Timeout(15)
    void schemaValidationDoesNotResolveExternalEntities() throws Exception {
        final AtomicBoolean contacted = new AtomicBoolean(false);
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setSoTimeout(2000);
            final int port = serverSocket.getLocalPort();
            final Thread acceptor = new Thread(() -> {
                try (final Socket socket = serverSocket.accept()) {
                    contacted.set(true);
                    final OutputStream out = socket.getOutputStream();
                    out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            .getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch (final IOException ignored) {
                    // timed out waiting for a connection: nothing tried to fetch the DTD
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            // An external parameter entity in the DOCTYPE points at the local socket. The DocumentBuilder does not
            // resolve it (external entities are disabled); the strict-mode schema validation must not either.
            final String dtdUrl = "http://localhost:" + port + "/evil.dtd";
            final String xml = "<?xml version=\"1.0\"?>\n"
                    + "<!DOCTYPE Configuration [ <!ENTITY % dtd SYSTEM \"" + dtdUrl + "\"> %dtd; ]>\n"
                    + "<Configuration status=\"OFF\" name=\"XxeSchema\" strict=\"true\" schema=\"Log4j-events.xsd\">\n"
                    + "  <Appenders>\n"
                    + "    <List name=\"list\"><PatternLayout pattern=\"%m%n\"/></List>\n"
                    + "  </Appenders>\n"
                    + "  <Loggers>\n"
                    + "    <Root level=\"info\"><AppenderRef ref=\"list\"/></Root>\n"
                    + "  </Loggers>\n"
                    + "</Configuration>\n";
            try (final LoggerContext context = new LoggerContext("XxeSchema")) {
                // Building the configuration runs the strict-mode schema validation in the constructor.
                new XmlConfiguration(
                        context,
                        new ConfigurationSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
            }
            acceptor.join(2500);
        }
        assertFalse(contacted.get(), "schema validation must not fetch the external DTD");
    }
}
