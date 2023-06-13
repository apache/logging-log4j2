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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.management.UnixOperatingSystemMXBean;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ConfigurationSourceTest {
    private static final Path JAR_FILE = Paths.get("target", "classes", "jarfile.jar");
    private static final Path TEST_JARFILE = Paths.get("target", "classes", "jarfile-copy.jar");

    @Test
    public void testJira_LOG4J2_2770_byteArray() throws Exception {
        final ConfigurationSource configurationSource = new ConfigurationSource(new ByteArrayInputStream(new byte[]{'a', 'b'}));
        assertNotNull(configurationSource.resetInputStream());
    }

    /**
     * Checks if the usage of 'jar:' URLs does not increase the file descriptor
     * count and the jar file can be deleted.
     *
     * @throws Exception
     */
    @Test
    public void testNoJarFileLeak() throws Exception {
        final URL jarConfigURL = prepareJarConfigURL();
        final long expected = getOpenFileDescriptorCount();
        UrlConnectionFactory.createConnection(jarConfigURL).getInputStream().close();
        // This can only fail on UNIX
        assertEquals(expected, getOpenFileDescriptorCount());
        // This can only fail on Windows
        try {
            Files.delete(TEST_JARFILE);
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    public void testLoadConfigurationSourceFromJarFile() throws Exception {
        final URL jarConfigURL = prepareJarConfigURL();
        final long expectedFdCount = getOpenFileDescriptorCount();
        ConfigurationSource configSource = ConfigurationSource.fromUri(jarConfigURL.toURI());
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
            Files.delete(TEST_JARFILE);
        } catch (IOException e) {
            fail(e);
        }
    }

    private long getOpenFileDescriptorCount() {
        final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        }
        return 0L;
    }

    private static URL prepareJarConfigURL() throws IOException {
      if(!Files.exists(TEST_JARFILE))
        Files.copy(JAR_FILE, TEST_JARFILE);
      return new URL("jar:" + TEST_JARFILE.toUri().toURL() + "!/config/console.xml");
    }

}
