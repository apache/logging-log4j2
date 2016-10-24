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

package org.apache.logging.log4j.core.config.plugins.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry.PluginTest;
import org.apache.logging.log4j.junit.CleanFolders;
import org.apache.logging.log4j.junit.URLStreamHandlerFactoryRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests the ResolverUtil class for custom protocol like bundleresource, vfs, vfszip.
 */
public class ResolverUtilCustomProtocolTest {

    @Rule
    public URLStreamHandlerFactoryRule rule = new URLStreamHandlerFactoryRule(new NoopURLStreamHandlerFactory());

    @Rule
    public RuleChain chain = RuleChain.outerRule(new CleanFolders(ResolverUtilTest.WORK_DIR));
    
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

    @Test
    public void testExtractPathFromVfsEarJarWindowsUrl() throws Exception {
        final URL url = new URL(
                "vfs:/C:/jboss/jboss-eap-6.4/standalone/deployments/com.xxx.yyy.application-ear.ear/lib/com.xxx.yyy.logging.jar/com/xxx/yyy/logging/config/");
        final String expected = "/C:/jboss/jboss-eap-6.4/standalone/deployments/com.xxx.yyy.application-ear.ear/lib/com.xxx.yyy.logging.jar/com/xxx/yyy/logging/config/";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromVfsWarClassesWindowsUrl() throws Exception {
        final URL url = new URL(
                "vfs:/C:/jboss/jboss-eap-6.4/standalone/deployments/test-log4j2-web-standalone.war/WEB-INF/classes/org/hypik/test/jboss/eap7/logging/config/");
        final String expected = "/C:/jboss/jboss-eap-6.4/standalone/deployments/test-log4j2-web-standalone.war/WEB-INF/classes/org/hypik/test/jboss/eap7/logging/config/";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromVfsWarClassesLinuxUrl() throws Exception {
        final URL url = new URL(
                "vfs:/content/mycustomweb.war/WEB-INF/classes/org/hypik/test/jboss/log4j2/logging/pluginweb/");
        final String expected = "/content/mycustomweb.war/WEB-INF/classes/org/hypik/test/jboss/log4j2/logging/pluginweb/";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromVfszipUrl() throws Exception {
        final URL url = new URL(
                "vfszip:/home2/jboss-5.0.1.CR2/jboss-as/server/ais/ais-deploy/myear.ear/mywar.war/WEB-INF/some.xsd");
        final String expected = "/home2/jboss-5.0.1.CR2/jboss-as/server/ais/ais-deploy/myear.ear/mywar.war/WEB-INF/some.xsd";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromVfsEarJarLinuxUrl() throws Exception {
        final URL url = new URL(
                "vfs:/content/test-log4k2-ear.ear/lib/test-log4j2-jar-plugins.jar/org/hypik/test/jboss/log4j2/pluginjar/");
        final String expected = "/content/test-log4k2-ear.ear/lib/test-log4j2-jar-plugins.jar/org/hypik/test/jboss/log4j2/pluginjar/";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromVfszipUrlWithPlusCharacters() throws Exception {
        final URL url = new URL("vfszip:/path+with+plus/file+name+with+plus.xml");
        final String expected = "/path+with+plus/file+name+with+plus.xml";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromVfsUrlWithPlusCharacters() throws Exception {
        final URL url = new URL("vfs:/path+with+plus/file+name+with+plus.xml");
        final String expected = "/path+with+plus/file+name+with+plus.xml";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromResourceBundleUrl() throws Exception {
        final URL url = new URL("bundleresource:/some/path/some/file.properties");
        final String expected = "/some/path/some/file.properties";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromResourceBundleUrlWithPlusCharacters() throws Exception {
        final URL url = new URL("bundleresource:/some+path/some+file.properties");
        final String expected = "/some+path/some+file.properties";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testFindInPackageFromVfsDirectoryURL() throws Exception {
        try (final URLClassLoader cl = ResolverUtilTest.compileAndCreateClassLoader("3")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil
                    .setClassLoader(new SingleURLClassLoader(new URL("vfs:/" + ResolverUtilTest.WORK_DIR + "/resolverutil3/customplugin3/"), cl));
            resolverUtil.findInPackage(new PluginTest(), "customplugin3");
            assertEquals("Class not found in packages", 1, resolverUtil.getClasses().size());
            assertEquals("Unexpected class resolved", cl.loadClass("customplugin3.FixedString3Layout"),
                    resolverUtil.getClasses().iterator().next());
        }
    }

    @Test
    public void testFindInPackageFromVfsJarURL() throws Exception {
        try (final URLClassLoader cl = ResolverUtilTest.compileJarAndCreateClassLoader("4")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(new SingleURLClassLoader(
                    new URL("vfs:/" + ResolverUtilTest.WORK_DIR + "/resolverutil4/customplugin4.jar/customplugin4/"), cl));
            resolverUtil.findInPackage(new PluginTest(), "customplugin4");
            assertEquals("Class not found in packages", 1, resolverUtil.getClasses().size());
            assertEquals("Unexpected class resolved", cl.loadClass("customplugin4.FixedString4Layout"),
                    resolverUtil.getClasses().iterator().next());
        }
    }

}
