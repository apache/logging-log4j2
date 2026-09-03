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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks that the Log4j 1.x XML configuration reader resolves external entities through
 * {@link ConfigurationSource}, so they are subject to the {@code log4j2.Configuration.allowedProtocols} restrictions.
 */
class XmlConfigurationExternalEntityTest {

    private static final String INJECTED_LOGGER = "external-entity-injected";

    /**
     * A {@code file} entity is allowed by default and is still resolved.
     */
    @Test
    void resolvesAllowedExternalEntities(@TempDir final Path tempDir) throws Exception {
        final Path injected = tempDir.resolve("injected.xml");
        Files.write(injected, ("<logger name=\"" + INJECTED_LOGGER + "\"></logger>").getBytes(StandardCharsets.UTF_8));

        final Configuration configuration = configure(tempDir, injected.toUri().toString());

        assertTrue(
                configuration.getLoggers().containsKey(INJECTED_LOGGER),
                "External entity was not resolved; allowed protocols must still be resolved");
    }

    /**
     * An entity fetched over a protocol that is not allowed is replaced by an empty source. The {@code @Timeout}
     * guards against the resolver reaching the network instead of rejecting the protocol.
     */
    @Test
    @Timeout(5)
    @SetTestProperty(key = "log4j2.configurationAllowedProtocols", value = "file")
    void ignoresDisallowedExternalEntities(@TempDir final Path tempDir) throws Exception {
        final Configuration configuration = configure(tempDir, "http://localhost:1/injected.xml");

        assertFalse(
                configuration.getLoggers().containsKey(INJECTED_LOGGER),
                "External entity was resolved; disallowed protocols must not be resolved");
    }

    private static Configuration configure(final Path tempDir, final String entitySystemId) throws Exception {
        // If the external entity is resolved, its replacement text injects a logger into the configuration.
        final Path configFile = tempDir.resolve("log4j1-external-entity.xml");
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\" [\n"
                + "  <!ENTITY injected SYSTEM \"" + entitySystemId + "\">\n"
                + "]>\n"
                + "<log4j:configuration xmlns:log4j=\"http://jakarta.apache.org/log4j/\">\n"
                + "  <appender name=\"console\" class=\"org.apache.log4j.ConsoleAppender\">\n"
                + "    <layout class=\"org.apache.log4j.SimpleLayout\"/>\n"
                + "  </appender>\n"
                + "  &injected;\n"
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
        return configuration;
    }
}
