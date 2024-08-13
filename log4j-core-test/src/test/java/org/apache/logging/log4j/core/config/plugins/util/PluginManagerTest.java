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
package org.apache.logging.log4j.core.config.plugins.util;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.processor.PluginCache;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.test.Compiler;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.util.LoaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginManagerTest {

    @BeforeEach
    void setUp() {
        PluginRegistry.getInstance().clear();
    }

    @Test
    @UsingStatusListener
    @SuppressWarnings("deprecation")
    void calling_addPackage_issues_warning(final ListStatusListener listener) {
        try {
            PluginManager.addPackage("com.example");
            assertThat(listener.findStatusData(Level.WARN))
                    .anySatisfy(PluginManagerTest::assertContainsPackageScanningLink);
            listener.clear();

            PluginManager.addPackages(singletonList("com.example"));
            assertThat(listener.findStatusData(Level.WARN))
                    .anySatisfy(PluginManagerTest::assertContainsPackageScanningLink);
            listener.clear();
        } finally {
            PluginManager.clearPackages();
        }
    }

    @Test
    @UsingStatusListener
    void using_packages_issues_warning(final ListStatusListener listener) {
        final ConfigurationSource source = ConfigurationSource.fromResource(
                "customplugin/log4j2-741.xml", getClass().getClassLoader());
        assertThat(source).as("Configuration source").isNotNull();
        final Configuration configuration = new XmlConfigurationFactory().getConfiguration(null, source);
        assertThat(configuration).as("Configuration").isNotNull();
        configuration.initialize();
        assertThat(configuration.getPluginPackages()).as("Plugin packages").contains("customplugin");
        assertThat(listener.findStatusData(Level.WARN))
                .anySatisfy(PluginManagerTest::assertContainsPackageScanningLink);
    }

    @Test
    @UsingStatusListener
    void using_packages_finds_custom_plugin(final ListStatusListener listener) throws Exception {
        // To ensure our custom plugin is NOT included in the log4j plugin metadata file,
        // we make sure the class does not exist until after the build is finished.
        // So we don't create the custom plugin class until this test is run.
        final URL resource = PluginManagerTest.class.getResource("/customplugin/FixedStringLayout.java.source");
        assertThat(resource).isNotNull();
        final File orig = new File(resource.toURI());
        final File f = new File(orig.getParentFile(), "FixedStringLayout.java");
        assertDoesNotThrow(() -> FileUtils.copyFile(orig, f));
        // compile generated source
        // (switch off annotation processing: no need to create Log4j2Plugins.dat)
        Compiler.compile(f, "-proc:none");
        assertDoesNotThrow(() -> FileUtils.delete(f));
        // load the compiled class
        Class.forName("customplugin.FixedStringLayout");

        final PluginManager manager = new PluginManager(Node.CATEGORY);
        manager.collectPlugins(singletonList("customplugin"));
        assertThat(manager.getPluginType("FixedString"))
                .as("Custom unregistered plugin")
                .isNotNull();
        assertThat(listener.findStatusData(Level.WARN)).anySatisfy(message -> {
            assertThat(message.getLevel()).isEqualTo(Level.WARN);
            assertThat(message.getMessage().getFormattedMessage())
                    .as("Status logger message")
                    // The message specifies which plugin is not registered
                    .contains("customplugin.FixedStringLayout")
                    // The message provides a link to the registration instructions
                    .contains("https://logging.apache.org/log4j/2.x/manual/plugins.html#plugin-registry");
        });
    }

    @Test
    @UsingStatusListener
    void missing_plugin_descriptor_issues_warning(final ListStatusListener listener) {
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(new FilteringClassLoader(getClass().getClassLoader(), null));
            assertThat(LoaderUtil.getClassLoader().getResource(PluginProcessor.PLUGIN_CACHE_FILE))
                    .isNull();
            final PluginManager manager = new PluginManager(Node.CATEGORY);
            manager.collectPlugins(null);
            assertThat(listener.findStatusData(Level.WARN)).anySatisfy(message -> {
                assertThat(message.getLevel()).isEqualTo(Level.WARN);
                assertThat(message.getMessage().getFormattedMessage())
                        .as("Status logger message")
                        // The message provides a link to plugin descriptor troubleshooting.
                        .contains("https://logging.apache.org/log4j/2.x/faq.html#plugin-descriptors");
            });
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    /**
     * Tests if removing descriptors of standard Log4j artifacts and replacing them with a custom one issues warnings.
     * <p>
     *     This often happens in shading scenarios.
     * </p>
     */
    @Test
    @UsingStatusListener
    void corrupted_plugin_descriptor_issues_warning(final ListStatusListener listener) throws IOException {
        // Create empty descriptor
        final Path customDescriptor = Files.createTempFile("Log4j2Plugins", ".dat");
        final URL descriptorUrl = customDescriptor.toUri().toURL();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            // Generate a plugin entry to have a non-empty plugin descriptor
            // For this test case it doesn't really matter if the class exists or not.
            // This test will not instantiate the plugin.
            final PluginEntry entry = new PluginEntry();
            entry.setKey("CustomPlugin");
            entry.setClassName("com.example.NotExistingClass");
            entry.setName("CustomPlugin");
            entry.setCategory(Node.CATEGORY);
            // Generate descriptor with single plugin
            final PluginCache customCache = new PluginCache();
            customCache.getCategory(Node.CATEGORY).put("CustomPlugin", entry);
            customCache.writeCache(Files.newOutputStream(customDescriptor));

            Thread.currentThread()
                    .setContextClassLoader(new FilteringClassLoader(getClass().getClassLoader(), descriptorUrl));

            assertThat(LoaderUtil.getClassLoader().getResource(PluginProcessor.PLUGIN_CACHE_FILE))
                    .isEqualTo(descriptorUrl);
            final PluginManager manager = new PluginManager(Node.CATEGORY);
            // Try loading some standard Log4j plugins
            manager.collectPlugins(singletonList("org.apache.logging.log4j"));
            assertThat(manager.getPluginType("Console")).isNotNull();
            assertThat(listener.findStatusData(Level.WARN)).anySatisfy(message -> {
                assertThat(message.getLevel()).isEqualTo(Level.WARN);
                assertThat(message.getMessage().getFormattedMessage())
                        .as("Status logger message")
                        // The message specifies which standard plugin is not registered
                        .contains("org.apache.logging.log4j.core.appender.ConsoleAppender")
                        // The message provides a link to plugin descriptor troubleshooting.
                        .contains("https://logging.apache.org/log4j/2.x/faq.html#plugin-descriptors");
            });
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
            Files.delete(customDescriptor);
        }
    }

    private static void assertContainsPackageScanningLink(final StatusData message) {
        assertThat(message.getLevel()).isEqualTo(Level.WARN);
        assertThat(message.getMessage().getFormattedMessage())
                // The message specifies
                .contains("https://logging.apache.org/log4j/2.x/faq.html#package-scanning");
    }

    private static final class FilteringClassLoader extends ClassLoader {

        private final URL descriptorUrl;

        private FilteringClassLoader(final ClassLoader parent, final URL descriptorUrl) {
            super(parent);
            this.descriptorUrl = descriptorUrl;
        }

        @Override
        public URL getResource(final String name) {
            return PluginProcessor.PLUGIN_CACHE_FILE.equals(name) ? descriptorUrl : super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(final String name) throws IOException {
            return PluginProcessor.PLUGIN_CACHE_FILE.equals(name)
                    ? descriptorUrl != null ? enumeration(singletonList(descriptorUrl)) : emptyEnumeration()
                    : super.getResources(name);
        }
    }
}
