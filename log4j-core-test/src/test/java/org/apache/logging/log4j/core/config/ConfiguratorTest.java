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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("functional")
public class ConfiguratorTest {

    @Test
    public void testInitializeFromAbsoluteFilePath() {
        final String path = new File("src/test/resources/log4j-list.xml").getAbsolutePath();
        testInitializeFromFilePath(path);
    }

    @Test
    public void testInitializeFromRelativeFilePath() {
        final String path = new File("src/test/resources/log4j-list.xml").toString();
        testInitializeFromFilePath(path);
    }

    @Test
    public void testReconfigure() {
        final String path = new File("src/test/resources/log4j-list.xml").getAbsolutePath();
        try (final LoggerContext loggerContext =
                Configurator.initialize(getClass().getName(), null, path)) {
            assertNotNull(loggerContext.getConfiguration().getAppender("List"));
            final URI uri = loggerContext.getConfigLocation();
            assertNotNull(uri, "No configuration location returned");
            Configurator.reconfigure();
            assertEquals(uri, loggerContext.getConfigLocation(), "Unexpected configuration location returned");
        }
    }

    @Test
    public void testReconfigureFromPath() {
        final String path = new File("src/test/resources/log4j-list.xml").getAbsolutePath();
        try (final LoggerContext loggerContext =
                Configurator.initialize(getClass().getName(), null, path)) {
            assertNotNull(loggerContext.getConfiguration().getAppender("List"));
            final URI uri = loggerContext.getConfigLocation();
            assertNotNull(uri, "No configuration location returned");
            final URI location = new File("src/test/resources/log4j2-config.xml").toURI();
            Configurator.reconfigure(location);
            assertEquals(location, loggerContext.getConfigLocation(), "Unexpected configuration location returned");
        }
    }

    private void testInitializeFromFilePath(final String path) {
        try (final LoggerContext loggerContext =
                Configurator.initialize(getClass().getName(), null, path)) {
            assertNotNull(loggerContext.getConfiguration().getAppender("List"));
        }
    }

    /**
     * LOG4J2-3631: Configurator uses getName() instead of getCanonicalName().
     */
    @Test
    public void testSetLevelUsesCanonicalName() {
        final String path = new File("src/test/resources/log4j-list.xml").getAbsolutePath();
        try (final LoggerContext loggerContext =
                Configurator.initialize(getClass().getName(), null, path)) {
            Configurator.setLevel(Internal.class, Level.DEBUG);
            final Configuration config = loggerContext.getConfiguration();
            assertNotNull(config);
            final String canonicalName = Internal.class.getCanonicalName();
            assertThat(config.getLoggerConfig(canonicalName))
                    .extracting(LoggerConfig::getName, LoggerConfig::getExplicitLevel)
                    .containsExactly(canonicalName, Level.DEBUG);
        }
    }

    private static class Internal {}
}
