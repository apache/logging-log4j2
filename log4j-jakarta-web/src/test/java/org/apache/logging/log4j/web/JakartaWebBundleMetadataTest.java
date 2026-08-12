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
package org.apache.logging.log4j.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class JakartaWebBundleMetadataTest {

    private static final String FRAGMENT_HOST = "Fragment-Host: org.apache.logging.log4j.core";
    private static final String JPMS_MODULE_NAME = "org.apache.logging.log4j.web";
    private static final String JAKARTA_SERVLET_RANGE = "jakarta.servlet;version=\"[5.0,7)\"";

    @Test
    void manifestContainsExpectedOsgiMetadata() throws Exception {
        final String manifest = readModuleResource("META-INF/MANIFEST.MF");
        assertTrue(manifest.contains(FRAGMENT_HOST), "Manifest should declare " + FRAGMENT_HOST);
        assertTrue(
                manifest.contains(JAKARTA_SERVLET_RANGE),
                "Manifest should declare jakarta.servlet import range [5.0,7)");
    }

    @Test
    void moduleDescriptorUsesSharedWebModuleName() throws Exception {
        final String moduleDescriptor = readModuleResource("module-info.class");
        assertTrue(
                moduleDescriptor.contains(JPMS_MODULE_NAME),
                "JPMS module name should be " + JPMS_MODULE_NAME);
    }

    @Test
    void servletContainerInitializerServiceFileRegistersLog4jInitializer() throws Exception {
        final String contents =
                readModuleResource("META-INF/services/jakarta.servlet.ServletContainerInitializer");
        assertTrue(
                contents.contains(Log4jServletContainerInitializer.class.getName()),
                "Service file must register " + Log4jServletContainerInitializer.class.getName());
    }

    private static String readModuleResource(final String resourceName) throws Exception {
        try (InputStream in = openModuleResource(resourceName)) {
            assertNotNull(in, resourceName + " should exist in the module artifact");
            return readIso8859(in);
        }
    }

    private static InputStream openModuleResource(final String resourceName) throws Exception {
        final URL codeSource =
                Log4jServletContainerInitializer.class.getProtectionDomain().getCodeSource().getLocation();
        final String normalizedResource = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        final Path codeSourcePath = Paths.get(codeSource.toURI());
        if (Files.isDirectory(codeSourcePath)) {
            final Path resourcePath = codeSourcePath.resolve(normalizedResource);
            return Files.newInputStream(resourcePath);
        }
        final JarFile jarFile = new JarFile(codeSourcePath.toFile());
        final JarEntry entry = jarFile.getJarEntry(normalizedResource);
        if (entry == null) {
            jarFile.close();
            return null;
        }
        return new JarFileInputStream(jarFile, entry);
    }

    private static String readIso8859(final InputStream in) throws IOException {
        return new String(readAllBytes(in), StandardCharsets.ISO_8859_1);
    }

    private static byte[] readAllBytes(final InputStream in) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[256];
        int read;
        while ((read = in.read(chunk)) >= 0) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static final class JarFileInputStream extends InputStream {

        private final JarFile jarFile;
        private final InputStream delegate;

        private JarFileInputStream(final JarFile jarFile, final JarEntry entry) throws IOException {
            this.jarFile = jarFile;
            this.delegate = jarFile.getInputStream(entry);
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return delegate.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            delegate.close();
            jarFile.close();
        }
    }
}
