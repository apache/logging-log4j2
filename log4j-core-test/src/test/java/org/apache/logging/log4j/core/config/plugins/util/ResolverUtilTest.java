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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry.PluginTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the ResolverUtil class.
 */
public class ResolverUtilTest {

    static Stream<Arguments> testExtractedPath() {
        return Stream.of(
                Arguments.of(
                        "jar:file:/C:/Users/me/.m2/repository/junit/junit/4.11/junit-4.11.jar!/org/junit/Test.class",
                        "/C:/Users/me/.m2/repository/junit/junit/4.11/junit-4.11.jar"),
                Arguments.of(
                        "jar:file:/path+with+plus/file+does+not+exist.jar!/some/file",
                        "/path with plus/file does not exist.jar"),
                Arguments.of(
                        "file:/C:/Users/me/workspace/log4j2/log4j-core/target/test-classes/log4j2-config.xml",
                        "/C:/Users/me/workspace/log4j2/log4j-core/target/test-classes/log4j2-config.xml"),
                Arguments.of(
                        "file:///path+with+plus/file+does+not+exist.xml", "/path with plus/file does not exist.xml"),
                Arguments.of("http://java.sun.com/index.html#chapter1", "/index.html"),
                Arguments.of(
                        "http://www.server.com/path+with+plus/file+name+with+plus.jar!/org/junit/Test.class",
                        "/path with plus/file name with plus.jar"),
                Arguments.of(
                        "https://issues.apache.org/jira/browse/LOG4J2-445?focusedCommentId=13862479&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13862479",
                        "/jira/browse/LOG4J2-445"),
                Arguments.of(
                        "ftp://user001:secretpassword@private.ftp-servers.example.com/mydirectory/myfile.txt",
                        "/mydirectory/myfile.txt"),
                Arguments.of(
                        "ftp://user001:secretpassword@private.ftp-servers.example.com/my+directory/my+file.txt",
                        "/my directory/my file.txt"));
    }

    @ParameterizedTest
    @MethodSource
    public void testExtractedPath(final String urlAsString, final String expected) throws Exception {
        final URL url = new URL(urlAsString);
        assertThat(new ResolverUtil().extractPath(url)).isEqualTo(expected);
    }

    @Test
    public void testExtractPathFromJarUrlNotDecodedIfFileExists() throws Exception {
        testExtractPathFromJarUrlNotDecodedIfFileExists("/log4j+config+with+plus+characters.xml");
    }

    private void testExtractPathFromJarUrlNotDecodedIfFileExists(final String existingFile)
            throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
        URL url = ResolverUtilTest.class.getResource(existingFile);
        if (!url.getProtocol().equals("jar")) {
            // create fake jar: URL that resolves to existing file
            url = new URL("jar:" + url.toExternalForm() + "!/some/entry");
        }
        final String actual = new ResolverUtil().extractPath(url);
        assertThat(actual).as("Not decoded path").endsWith(existingFile);
    }

    @Test
    public void testFileFromUriWithSpacesAndPlusCharactersInName() throws Exception {
        final String existingFile = "/s p a c e s/log4j+config+with+plus+characters.xml";
        testExtractPathFromJarUrlNotDecodedIfFileExists(existingFile);
    }

    @Test
    public void testExtractPathFromFileUrlNotDecodedIfFileExists() throws Exception {
        final String existingFile = "/log4j+config+with+plus+characters.xml";
        final URL url = ResolverUtilTest.class.getResource(existingFile);
        assertThat(url).hasProtocol("file");
        final String actual = new ResolverUtil().extractPath(url);
        assertThat(actual).endsWith(existingFile);
    }

    @Test
    public void testFindInPackageFromDirectoryPath(final @TempDir File tmpDir) throws Exception {
        try (final URLClassLoader cl = compileAndCreateClassLoader(tmpDir, "1")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(cl);
            resolverUtil.findInPackage(new PluginTest(), "customplugin1");
            assertThat(resolverUtil.getClasses())
                    .as("Number of plugin classes")
                    .hasSize(1)
                    .as("Plugin class")
                    .contains(cl.loadClass("customplugin1.FixedString1Layout"));
        }
    }

    @Test
    public void testFindInPackageFromJarPath(final @TempDir File tmpDir) throws Exception {
        try (final URLClassLoader cl = compileJarAndCreateClassLoader(tmpDir, "2")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(cl);
            resolverUtil.findInPackage(new PluginTest(), "customplugin2");
            assertThat(resolverUtil.getClasses())
                    .as("Number of plugin classes")
                    .hasSize(1)
                    .as("Plugin class")
                    .contains(cl.loadClass("customplugin2.FixedString2Layout"));
        }
    }

    static URLClassLoader compileJarAndCreateClassLoader(final File tmpDir, final String suffix)
            throws IOException, Exception {
        compile(tmpDir, suffix);
        final File jarFile = new File(tmpDir, "customplugin" + suffix + ".jar");
        final URI jarURI = jarFile.toURI();
        createJar(jarURI, tmpDir, new File(tmpDir, "customplugin" + suffix + "/FixedString" + suffix + "Layout.class"));
        return URLClassLoader.newInstance(new URL[] {jarURI.toURL()});
    }

    static URLClassLoader compileAndCreateClassLoader(final File tmpDir, final String suffix) throws Exception {
        compile(tmpDir, suffix);
        return URLClassLoader.newInstance(new URL[] {tmpDir.toURI().toURL()});
    }

    static void compile(final File tmpDir, final String suffix) throws Exception {
        final URL resource = ResolverUtilTest.class.getResource("/customplugin/FixedStringLayout.java.source");
        assertThat(resource).isNotNull();
        final File orig = new File(resource.toURI());
        final File f = new File(tmpDir, "customplugin" + suffix + "/FixedString" + suffix + "Layout.java");
        final File parent = f.getParentFile();
        if (!parent.exists()) {
            assertTrue(f.getParentFile().mkdirs(), "Create customplugin" + suffix + " folder KO");
        }

        final String content = new String(Files.readAllBytes(orig.toPath()))
                .replaceAll("FixedString", "FixedString" + suffix)
                .replaceAll("customplugin", "customplugin" + suffix);
        Files.write(f.toPath(), content.getBytes());

        PluginManagerPackagesTest.compile(f);
    }

    static void createJar(final URI jarURI, final File workDir, final File f) throws Exception {
        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        final URI uri = URI.create("jar:file://" + jarURI.getRawPath());
        try (final FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            final Path path =
                    zipfs.getPath(workDir.toPath().relativize(f.toPath()).toString());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.copy(f.toPath(), path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
