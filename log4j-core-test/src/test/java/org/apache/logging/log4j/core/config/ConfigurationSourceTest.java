/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.junit.jupiter.api.Test;

import com.sun.management.UnixOperatingSystemMXBean;

public class ConfigurationSourceTest {

    @Test
    public void testJira_LOG4J2_2770_byteArray() throws Exception {
        ConfigurationSource configurationSource = new ConfigurationSource(new ByteArrayInputStream(new byte[] { 'a', 'b' }));
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
        final Path original = Paths.get("target", "classes", "jarfile.jar");
        final Path copy = Paths.get("target", "classes", "jarfile-copy.jar");
        Files.copy(original, copy);
        final URL jarUrl = new URL("jar:" + copy.toUri().toURL() + "!/config/console.xml");
        final long expected = getOpenFileDescriptorCount();
        UrlConnectionFactory.createConnection(jarUrl).getInputStream().close();
        // This can only fail on UNIX
        assertEquals(expected, getOpenFileDescriptorCount());
        // This can only fail on Windows
        try {
            Files.delete(copy);
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
}
