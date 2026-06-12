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

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.management.UnixOperatingSystemMXBean;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConfigurationSourceTest {
    /**
     * The path inside the jar created by {@link #prepareJarConfigURL} containing the configuration.
     */
    public static final String PATH_IN_JAR = "/config/console.xml";

    private static final String CONFIG_FILE = "/config/ConfigurationSourceTest.xml";

    @TempDir
    private Path tempDir;

    @Test
    void testJira_LOG4J2_2770_byteArray() throws Exception {
        final ConfigurationSource configurationSource =
                new ConfigurationSource(new ByteArrayInputStream(new byte[] {'a', 'b'}));
        assertNotNull(configurationSource.resetInputStream());
    }

    /**
     * Checks if the usage of 'jar:' URLs does not increase the file descriptor
     * count, and the jar file can be deleted.
     */
    @Test
    void testNoJarFileLeak() throws Exception {
        final Path jarFile = prepareJarConfigURL(tempDir);
        final URL jarConfigURL = new URL("jar:" + jarFile.toUri().toURL() + "!" + PATH_IN_JAR);
        final long expected = getOpenFileDescriptorCount();
        UrlConnectionFactory.createConnection(jarConfigURL).getInputStream().close();
        // This can only fail on UNIX
        assertEquals(expected, getOpenFileDescriptorCount());
        // This can only fail on Windows
        try {
            Files.delete(jarFile);
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void testLoadConfigurationSourceFromJarFile() throws Exception {
        final Path jarFile = prepareJarConfigURL(tempDir);
        final URL jarConfigURL = new URL("jar:" + jarFile.toUri().toURL() + "!" + PATH_IN_JAR);
        final long expectedFdCount = getOpenFileDescriptorCount();
        ConfigurationSource configSource = ConfigurationSource.fromUri(jarConfigURL.toURI());
        assertNotNull(configSource);
        assertEquals(jarConfigURL.toString(), configSource.getLocation());
        assertNull(configSource.getFile());
        assertTrue(configSource.getLastModified() > 0);
        assertEquals(jarConfigURL, configSource.getURL());
        assertNotNull(configSource.getInputStream());
        configSource.getInputStream().close();

        // This can only fail on UNIX
        assertEquals(expectedFdCount, getOpenFileDescriptorCount());
        // This can only fail on Windows
        try {
            Files.delete(jarFile);
        } catch (IOException e) {
            fail(e);
        }
    }

    /**
     * Verifies that a failed JAR configuration load (missing entry) does not leak file descriptors
     * or hold a lock on the JAR file.
     */
    @Test
    void testJarFileLeakOnFailure() throws Exception {
        final Path jarFile = prepareJarConfigURL(tempDir);
        final String jarUriStr = "jar:" + jarFile.toUri().toASCIIString() + "!/" + java.util.UUID.randomUUID();
        final URI jarUri = URI.create(jarUriStr);

        final long expectedFdCount = getOpenFileDescriptorCount();

        assertNull(ConfigurationSource.fromUri(jarUri));

        assertEquals(expectedFdCount, getOpenFileDescriptorCount());
        Files.delete(jarFile);
    }

    /**
     * Verifies that the error-path cleanup in {@code getConfigurationSource}
     * properly disconnects an {@link HttpURLConnection} on failure without
     * calling {@link URLConnection#getInputStream()} a second time.
     */
    @Test
    void getConfigurationSource_disconnectsHttpConnection_onError() throws Exception {
        final AtomicInteger getInputStreamCalls = new AtomicInteger();
        final AtomicBoolean disconnected = new AtomicBoolean();
        final AtomicInteger openConnectionCalls = new AtomicInteger();

        final URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL u) {
                openConnectionCalls.incrementAndGet();
                return new HttpURLConnection(u) {
                    @Override
                    public InputStream getInputStream() throws IOException {
                        getInputStreamCalls.incrementAndGet();
                        throw new FileNotFoundException("Mocked 404");
                    }

                    @Override
                    public void disconnect() {
                        disconnected.set(true);
                    }

                    @Override
                    public boolean usingProxy() {
                        return false;
                    }

                    @Override
                    public void connect() {}
                };
            }
        };

        final URL url = new URL("http", "example.com", 80, "/missing.xml", handler);

        // Allow the "http" protocol for this test.
        final String propKey = "log4j2.Configuration.allowedProtocols";
        final String previous = System.getProperty(propKey);
        System.setProperty(propKey, "file, https, jar, http");
        try {
            openConnectionCalls.set(0);
            getInputStreamCalls.set(0);

            final Method method =
                    ConfigurationSource.class.getDeclaredMethod("getConfigurationSource", URL.class);
            method.setAccessible(true);
            final Object result = method.invoke(null, url);

            assertTrue(
                    openConnectionCalls.get() > 0,
                    "Custom URLStreamHandler was not used by UrlConnectionFactory");

            assertNull(result, "Expected null return for a 404 response");

            assertTrue(disconnected.get(), "disconnect() must be called on the error path");

            assertEquals(
                    1,
                    getInputStreamCalls.get(),
                    "getInputStream() should be called exactly once");
        } finally {
            if (previous != null) {
                System.setProperty(propKey, previous);
            } else {
                System.clearProperty(propKey);
            }
        }
    }

    private long getOpenFileDescriptorCount() {
        final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        }
        return 0L;
    }

    public static Path prepareJarConfigURL(Path dir) throws IOException {
        Path jarFile = dir.resolve("jarFile.jar");
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (final OutputStream os = Files.newOutputStream(jarFile);
                final JarOutputStream jar = new JarOutputStream(os, manifest);
                final InputStream config =
                        requireNonNull(ConfigurationSourceTest.class.getResourceAsStream(CONFIG_FILE))) {
            final JarEntry jarEntry = new JarEntry("config/console.xml");
            jar.putNextEntry(jarEntry);
            IOUtils.copy(config, os);
        }
        return jarFile;
    }
}
