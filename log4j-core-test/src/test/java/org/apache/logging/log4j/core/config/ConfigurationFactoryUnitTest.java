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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.filter.ThreadContextMapFilter;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that the same logical configuration, expressed as XML, JSON, YAML, Java properties, an XInclude-composed
 * XML file, or a merged configuration produces the same components when loaded through {@link ConfigurationFactory}.
 *
 * <p>Unlike {@code ConfigurationFactoryTest}, this exercises the {@link Configuration} directly.</p>
 */
class ConfigurationFactoryUnitTest {

    // Holds the log files created by the loaded configurations, deleted after the test.
    @TempLoggingDir
    private static Path loggingDir;

    private static Configuration load(final String resource) throws Exception {
        final URI uri =
                ConfigurationFactoryUnitTest.class.getResource("/" + resource).toURI();
        final Configuration configuration =
                ConfigurationFactory.getInstance().getConfiguration(new LoggerContext("test"), resource, uri);
        return startConfiguration(configuration);
    }

    private static Configuration loadComposite(final String... resources) throws Exception {
        final List<URI> uris = new ArrayList<>();
        for (final String resource : resources) {
            uris.add(ConfigurationFactoryUnitTest.class
                    .getResource("/" + resource)
                    .toURI());
        }
        final Configuration configuration =
                ConfigurationFactory.getInstance().getConfiguration(new LoggerContext("test"), "composite", uris);
        return startConfiguration(configuration);
    }

    /**
     * Initializes <em>and</em> starts the configuration.
     *
     * <p>A {@link FileAppender} opens its file when it is constructed, during {@link Configuration#initialize()}, but
     * {@link Configuration#stop()} only closes appenders that have been started. The configuration must therefore be
     * started so that the matching {@code stop()} in each test releases the file handles; otherwise the open files
     * would, on Windows, prevent deletion of the temporary logging directory.</p>
     */
    private static Configuration startConfiguration(final Configuration configuration) {
        configuration.start();
        return configuration;
    }

    @ParameterizedTest
    @SetTestProperty(key = "log4j2.configurationEnableXInclude", value = "true")
    @ValueSource(
            strings = {
                "log4j-test1.xml",
                "log4j-test1.json",
                "log4j-test1.yaml",
                "log4j-test1.properties",
                "log4j-xinclude.xml"
            })
    void producesEquivalentComponents(final String resource) throws Exception {
        final Configuration configuration = load(resource);
        try {
            assertAppenders(configuration);
            assertLoggers(configuration);
            // Every representation declares a `filename` substitution property resolving to a per-format log file.
            assertThat(configuration.getStrSubstitutor().replace("${filename}")).endsWith(".log");
        } finally {
            configuration.stop();
        }
    }

    /**
     * Tests behavior with XInclude disabled.
     *
     * <p>With XInclude disabled (the default), the {@code xi:include} elements are not expanded,
     * so Log4j ignores them and falls back to the default setup:
     * an {@code ERROR}-level root logger with a default {@code Console} appender.</p>
     *
     * <p>The {@code Properties} declared directly in the file are still honored.</p>
     */
    @Test
    void xIncludeDisabledIgnoresIncludesAndUsesDefaults() throws Exception {
        final Configuration configuration = load("log4j-xinclude.xml");
        try {
            // None of the included appenders or loggers are present.
            final Map<String, Appender> appenders = configuration.getAppenders();
            assertThat(appenders).doesNotContainKeys("STDOUT", "File", "List");
            assertThat(configuration.getLoggers())
                    .doesNotContainKeys("org.apache.logging.log4j.test1", "org.apache.logging.log4j.test2");

            // The default setup is used instead: a single default Console appender and an ERROR-level root logger.
            assertThat(appenders.values()).singleElement().satisfies(appender -> {
                assertThat(appender).isInstanceOf(ConsoleAppender.class);
                assertThat(appender.getName()).startsWith("DefaultConsole-");
            });
            assertThat(configuration.getRootLogger().getLevel()).isEqualTo(Level.ERROR);

            // The `Properties` declared directly in the file (outside the includes) are still resolved.
            assertThat(configuration.getStrSubstitutor().replace("${filename}")).endsWith(".log");
        } finally {
            configuration.stop();
        }
    }

    /**
     * The same component graph can be assembled from several sources, possibly in different formats, merged into a
     * {@link CompositeConfiguration}: here an XML base (one appender and the root logger) and a YAML remainder.
     */
    @Test
    void mergesMultipleSources() throws Exception {
        final Configuration configuration = loadComposite("log4j-test1-basic.xml", "log4j-test1-extended.yaml");
        try {
            assertThat(configuration).isInstanceOf(CompositeConfiguration.class);
            assertAppenders(configuration);
            assertLoggers(configuration);
            assertThat(configuration.getStrSubstitutor().replace("${filename}")).endsWith(".log");
        } finally {
            configuration.stop();
        }
    }

    private static void assertAppenders(final Configuration configuration) {
        final Map<String, Appender> appenders = configuration.getAppenders();
        assertThat(appenders).containsOnlyKeys("STDOUT", "File", "List");
        assertThat(appenders.get("STDOUT")).isInstanceOf(ConsoleAppender.class);
        assertThat(appenders.get("File")).isInstanceOf(FileAppender.class);
        assertThat(appenders.get("List")).isInstanceOf(ListAppender.class);
    }

    private static void assertLoggers(final Configuration configuration) {
        final LoggerConfig test1 = configuration.getLoggerConfig("org.apache.logging.log4j.test1");
        assertThat(test1.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(test1.isAdditive()).isFalse();
        assertThat(test1.getFilter()).isInstanceOf(ThreadContextMapFilter.class);
        assertThat(test1.getAppenderRefs()).extracting(AppenderRef::getRef).containsExactly("STDOUT");

        final LoggerConfig test2 = configuration.getLoggerConfig("org.apache.logging.log4j.test2");
        assertThat(test2.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(test2.isAdditive()).isFalse();
        assertThat(test2.getAppenderRefs()).extracting(AppenderRef::getRef).containsExactly("File");

        final LoggerConfig root = configuration.getRootLogger();
        assertThat(root.getLevel()).isEqualTo(Level.ERROR);
        assertThat(root.getAppenderRefs()).extracting(AppenderRef::getRef).containsExactly("STDOUT");
    }
}
