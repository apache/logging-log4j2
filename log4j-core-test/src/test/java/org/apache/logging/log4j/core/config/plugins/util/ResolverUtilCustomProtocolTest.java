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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry.PluginTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the ResolverUtil class for custom protocol like bundleresource, vfs, vfszip.
 */
public class ResolverUtilCustomProtocolTest {

    static class NoopURLStreamHandlerFactory implements URLStreamHandlerFactory {

        @Override
        public URLStreamHandler createURLStreamHandler(final String protocol) {
            return new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(final URL url) {
                    return open(url, null);
                }

                private URLConnection open(final URL url, final Proxy proxy) {
                    return new URLConnection(url) {
                        @Override
                        public void connect() throws IOException {
                            // do nothing
                        }
                    };
                }

                @Override
                protected URLConnection openConnection(final URL url, final Proxy proxy) {
                    return open(url, proxy);
                }

                @Override
                protected int getDefaultPort() {
                    return 1;
                }
            };
        }
    }

    private static final String DIR = "target/vfs";
    private static Field factoryField;
    private static URLStreamHandlerFactory oldFactory;

    @BeforeAll
    static void setup() throws Exception {
        for (final Field field : URL.class.getDeclaredFields()) {
            if (URLStreamHandlerFactory.class.equals(field.getType()) && "factory".equals(field.getName())) {
                factoryField = field;
                factoryField.setAccessible(true);
                oldFactory = (URLStreamHandlerFactory) factoryField.get(null);
            }
        }
        assertThat(factoryField).as("java.net.URL#factory field").isNotNull();
        URL.setURLStreamHandlerFactory(new NoopURLStreamHandlerFactory());
    }

    @AfterAll
    static void cleanup() throws Exception {
        final Field handlersFields = URL.class.getDeclaredField("handlers");
        if (handlersFields != null) {
            if (!handlersFields.isAccessible()) {
                handlersFields.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            final Hashtable<String, URLStreamHandler> handlers =
                    (Hashtable<String, URLStreamHandler>) handlersFields.get(null);
            if (handlers != null) {
                handlers.clear();
            }
        }
        factoryField.set(null, null);
        URL.setURLStreamHandlerFactory(oldFactory);
    }

    static class SingleURLClassLoader extends ClassLoader {
        private final URL url;

        public SingleURLClassLoader(final URL url) {
            this.url = url;
        }

        public SingleURLClassLoader(final URL url, final ClassLoader parent) {
            super(parent);
            this.url = url;
        }

        @Override
        protected URL findResource(final String name) {
            return url;
        }

        @Override
        public URL getResource(final String name) {
            return findResource(name);
        }

        @Override
        public Enumeration<URL> getResources(final String name) throws IOException {
            return findResources(name);
        }

        @Override
        protected Enumeration<URL> findResources(final String name) throws IOException {
            return Collections.enumeration(Arrays.asList(findResource(name)));
        }
    }

    static Stream<Arguments> testExtractedPath() {
        return Stream.of(
                Arguments.of(
                        "vfs:/C:/jboss/jboss-eap-6.4/standalone/deployments/com.xxx.yyy.application-ear.ear/lib/com.xxx.yyy.logging.jar/com/xxx/yyy/logging/config/",
                        "/C:/jboss/jboss-eap-6.4/standalone/deployments/com.xxx.yyy.application-ear.ear/lib/com.xxx.yyy.logging.jar/com/xxx/yyy/logging/config/"),
                Arguments.of(
                        "vfs:/C:/jboss/jboss-eap-6.4/standalone/deployments/test-log4j2-web-standalone.war/WEB-INF/classes/org/hypik/test/jboss/eap7/logging/config/",
                        "/C:/jboss/jboss-eap-6.4/standalone/deployments/test-log4j2-web-standalone.war/WEB-INF/classes/org/hypik/test/jboss/eap7/logging/config/"),
                Arguments.of(
                        "vfs:/content/mycustomweb.war/WEB-INF/classes/org/hypik/test/jboss/log4j2/logging/pluginweb/",
                        "/content/mycustomweb.war/WEB-INF/classes/org/hypik/test/jboss/log4j2/logging/pluginweb/"),
                Arguments.of(
                        "vfszip:/home2/jboss-5.0.1.CR2/jboss-as/server/ais/ais-deploy/myear.ear/mywar.war/WEB-INF/some.xsd",
                        "/home2/jboss-5.0.1.CR2/jboss-as/server/ais/ais-deploy/myear.ear/mywar.war/WEB-INF/some.xsd"),
                Arguments.of(
                        "vfs:/content/test-log4k2-ear.ear/lib/test-log4j2-jar-plugins.jar/org/hypik/test/jboss/log4j2/pluginjar/",
                        "/content/test-log4k2-ear.ear/lib/test-log4j2-jar-plugins.jar/org/hypik/test/jboss/log4j2/pluginjar/"),
                Arguments.of(
                        "vfszip:/path+with+plus/file+name+with+plus.xml", "/path+with+plus/file+name+with+plus.xml"),
                Arguments.of("vfs:/path+with+plus/file+name+with+plus.xml", "/path+with+plus/file+name+with+plus.xml"),
                Arguments.of("bundleresource:/some/path/some/file.properties", "/some/path/some/file.properties"),
                Arguments.of("bundleresource:/some+path/some+file.properties", "/some+path/some+file.properties"));
    }

    @ParameterizedTest
    @MethodSource
    public void testExtractedPath(final String urlAsString, final String expected) throws Exception {
        final URL url = new URL(urlAsString);
        assertThat(new ResolverUtil().extractPath(url)).isEqualTo(expected);
    }

    @Test
    public void testFindInPackageFromVfsDirectoryURL() throws Exception {
        final File tmpDir = new File(DIR, "resolverutil3");
        try (final URLClassLoader cl = ResolverUtilTest.compileAndCreateClassLoader(tmpDir, "3")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(new SingleURLClassLoader(new URL("vfs:/" + tmpDir + "/customplugin3/"), cl));
            resolverUtil.findInPackage(new PluginTest(), "customplugin3");
            assertEquals(
                    "Class not found in packages", 1, resolverUtil.getClasses().size());
            assertEquals(
                    "Unexpected class resolved",
                    cl.loadClass("customplugin3.FixedString3Layout"),
                    resolverUtil.getClasses().iterator().next());
        }
    }

    @Test
    public void testFindInPackageFromVfsJarURL() throws Exception {
        final File tmpDir = new File(DIR, "resolverutil4");
        try (final URLClassLoader cl = ResolverUtilTest.compileJarAndCreateClassLoader(tmpDir, "4")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(
                    new SingleURLClassLoader(new URL("vfs:/" + tmpDir + "/customplugin4.jar/customplugin4/"), cl));
            resolverUtil.findInPackage(new PluginTest(), "customplugin4");
            assertEquals(
                    "Class not found in packages", 1, resolverUtil.getClasses().size());
            assertEquals(
                    "Unexpected class resolved",
                    cl.loadClass("customplugin4.FixedString4Layout"),
                    resolverUtil.getClasses().iterator().next());
        }
    }
}
