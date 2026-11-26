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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CompositeConfigurationPostConfigureTest {

    @TempDir
    private Path tempDir;

    private static final String MARKER = "programmaticMarker";

    private static final byte[] XML = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<Configuration><Loggers><Logger name=\"from.base\" level=\"INFO\"/><Root level=\"INFO\"/>"
                    + "</Loggers></Configuration>\n")
            .getBytes(StandardCharsets.UTF_8);

    private static final class ContributingConfiguration extends XmlConfiguration {
        ContributingConfiguration(final LoggerContext ctx, final ConfigurationSource source) {
            super(ctx, source);
        }

        @Override
        public void setup() {
            super.setup();
            for (final Node loggers : getRootNode().getChildren()) {
                if ("Loggers".equals(loggers.getName())) {
                    final boolean alreadyAdded = loggers.getChildren().stream().anyMatch(node -> "from.setup"
                            .equals(node.getAttributes().get("name")));
                    if (!alreadyAdded && !loggers.getChildren().isEmpty()) {
                        final Node template = loggers.getChildren().get(0);
                        final Node node = new Node(loggers, "Logger", template.getType());
                        node.getAttributes().put("name", "from.setup");
                        node.getAttributes().put("level", "trace");
                        loggers.getChildren().add(node);
                    }
                }
            }
        }

        @Override
        public void postConfigure(final Configuration target) {
            if (target.getAppender(MARKER) == null) {
                target.addAppender(NullAppender.createAppender(MARKER));
            }
        }

        @Override
        public Configuration reconfigure() {
            try {
                final ConfigurationSource source = getConfigurationSource().resetInputStream();
                return source == null ? null : new ContributingConfiguration(getLoggerContext(), source);
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    private static ConfigurationSource streamSource() throws Exception {
        return new ConfigurationSource(new ByteArrayInputStream(XML));
    }

    private static CompositeConfiguration composite(final LoggerContext ctx) throws Exception {
        final AbstractConfiguration contributing = new ContributingConfiguration(ctx, streamSource());
        final AbstractConfiguration plain = new XmlConfiguration(ctx, streamSource());
        return new CompositeConfiguration(Arrays.asList(contributing, plain));
    }

    private CompositeConfiguration fileComposite(final LoggerContext ctx) throws Exception {
        final Path file = tempDir.resolve("configuration.xml");
        Files.write(file, XML);
        final AbstractConfiguration contributing =
                new ContributingConfiguration(ctx, ConfigurationSource.fromUri(file.toUri()));
        final AbstractConfiguration plain = new XmlConfiguration(ctx, ConfigurationSource.fromUri(file.toUri()));
        return new CompositeConfiguration(Arrays.asList(contributing, plain));
    }

    @Test
    public void postConfigureRunsOnInitialBuild() throws Exception {
        final LoggerContext ctx = new LoggerContext("postConfigureInitial");
        final CompositeConfiguration composite = composite(ctx);
        composite.initialize();
        assertNotNull(composite.getAppender(MARKER), "postConfigure contribution missing from the composite");
    }

    @Test
    public void postConfigureRunsAgainOnReconfigure() throws Exception {
        final LoggerContext ctx = new LoggerContext("postConfigureReconfigure");
        final CompositeConfiguration composite = composite(ctx);
        composite.initialize();
        final Appender first = composite.getAppender(MARKER);
        assertNotNull(first, "postConfigure contribution missing after the initial build");

        final Configuration reconfigured = composite.reconfigure();
        assertNotNull(reconfigured, "reconfigure() returned null");
        reconfigured.initialize();
        final Appender second = reconfigured.getAppender(MARKER);
        assertNotNull(second, "postConfigure contribution dropped on reconfigure");
        assertNotSame(first, second, "reconfigure should rebuild the contribution on the new configuration");
    }

    @Test
    public void childReconfigureIsUsedForFileBackedConfiguration() throws Exception {
        final LoggerContext ctx = new LoggerContext("postConfigureFileReconfigure");
        final CompositeConfiguration composite = fileComposite(ctx);
        composite.initialize();

        final Configuration reconfigured = composite.reconfigure();
        assertNotNull(reconfigured, "reconfigure() returned null");
        reconfigured.initialize();
        assertEquals(
                Level.TRACE,
                reconfigured.getLoggerConfig("from.setup").getLevel(),
                "custom child setup contribution was dropped on reconfigure");
        assertNotNull(
                reconfigured.getAppender(MARKER), "custom child configuration contribution was dropped on reconfigure");
    }
}
