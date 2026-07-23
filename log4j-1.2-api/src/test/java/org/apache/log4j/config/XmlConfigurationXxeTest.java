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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks that the Log4j 1.x XML configuration reader does not resolve external entities.
 */
class XmlConfigurationXxeTest {

    @Test
    void doesNotResolveExternalEntities(@TempDir final Path tempDir) throws Exception {
        // If the external entity is resolved, its replacement text injects this logger into the configuration.
        final Path injected = tempDir.resolve("injected.xml");
        Files.write(injected, "<logger name=\"xxe-injected\"></logger>".getBytes(StandardCharsets.UTF_8));

        final Path configFile = tempDir.resolve("log4j1-xxe.xml");
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\" [\n"
                + "  <!ENTITY xxe SYSTEM \"" + injected.toUri() + "\">\n"
                + "]>\n"
                + "<log4j:configuration xmlns:log4j=\"http://jakarta.apache.org/log4j/\">\n"
                + "  <appender name=\"console\" class=\"org.apache.log4j.ConsoleAppender\">\n"
                + "    <layout class=\"org.apache.log4j.SimpleLayout\"/>\n"
                + "  </appender>\n"
                + "  &xxe;\n"
                + "  <root>\n"
                + "    <priority value=\"debug\"/>\n"
                + "    <appender-ref ref=\"console\"/>\n"
                + "  </root>\n"
                + "</log4j:configuration>\n";
        Files.write(configFile, xml.getBytes(StandardCharsets.UTF_8));

        final ConfigurationSource source = new ConfigurationSource(Files.newInputStream(configFile), configFile);
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        assertNotNull(configuration, "No configuration created");
        configuration.initialize();

        assertFalse(
                configuration.getLoggers().containsKey("xxe-injected"),
                "External entity was resolved; the parser must not process external entities");
    }
}
