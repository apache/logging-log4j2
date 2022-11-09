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
package org.apache.logging.log4j.plugins.util;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.logging.log4j.plugins.model.PluginRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the ResolverUtil class.
 */
public class ResolverUtilTest {

    @Test
    public void testExtractPathFromJarUrl() throws Exception {
        final URL url = new URL("jar:file:/C:/Users/me/.m2/repository/junit/junit/4.11/junit-4.11.jar!/org/junit/Test.class");
        final String expected = "/C:/Users/me/.m2/repository/junit/junit/4.11/junit-4.11.jar";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromJarUrlNotDecodedIfFileExists() throws Exception {
        testExtractPathFromJarUrlNotDecodedIfFileExists("/log4j+config+with+plus+characters.xml");
    }

    private void testExtractPathFromJarUrlNotDecodedIfFileExists(final String existingFile)
            throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
        URL url = ResolverUtilTest.class.getResource(existingFile);
        assertNotNull(url, "No url returned for " + existingFile);
        if (!url.getProtocol().equals("jar")) {
            // create fake jar: URL that resolves to existing file
            url = new URL("jar:" + url.toExternalForm() + "!/some/entry");
        }
        final String actual = new ResolverUtil().extractPath(url);
        assertTrue(actual.endsWith(existingFile), "should not be decoded: " + actual);
    }

    @Test
    public void testFileFromUriWithSpacesAndPlusCharactersInName() throws Exception {
        final String existingFile = "/s p a c e s/log4j+config+with+plus+characters.xml";
        testExtractPathFromJarUrlNotDecodedIfFileExists(existingFile);
    }

    @Test
    public void testExtractPathFromJarUrlDecodedIfFileDoesNotExist() throws Exception {
        final URL url = new URL("jar:file:/path+with+plus/file+does+not+exist.jar!/some/file");
        final String expected = "/path with plus/file does not exist.jar";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFileUrl() throws Exception {
        final URL url = new URL("file:/C:/Users/me/workspace/log4j2/log4j-core/target/test-classes/log4j2-config.xml");
        final String expected = "/C:/Users/me/workspace/log4j2/log4j-core/target/test-classes/log4j2-config.xml";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFileUrlNotDecodedIfFileExists() throws Exception {
        final String existingFile = "/log4j+config+with+plus+characters.xml";
        final URL url = ResolverUtilTest.class.getResource(existingFile);
        assertNotNull(url);
        assertEquals("file", url.getProtocol(), "should be file url but was " + url);

        final String actual = new ResolverUtil().extractPath(url);
        assertTrue(actual.endsWith(existingFile), "should not be decoded: " + actual);
    }

    @Test
    public void testExtractPathFromFileUrlDecodedIfFileDoesNotExist() throws Exception {
        final URL url = new URL("file:///path+with+plus/file+does+not+exist.xml");
        final String expected = "/path with plus/file does not exist.xml";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromHttpUrl() throws Exception {
        final URL url = new URL("http://java.sun.com/index.html#chapter1");
        final String expected = "/index.html";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromHttpUrlWithPlusCharacters() throws Exception {
        final URL url = new URL("http://www.server.com/path+with+plus/file+name+with+plus.jar!/org/junit/Test.class");
        final String expected = "/path with plus/file name with plus.jar";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromHttpsComplexUrl() throws Exception {
        final URL url = new URL("https://issues.apache.org/jira/browse/LOG4J2-445?focusedCommentId=13862479&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13862479");
        final String expected = "/jira/browse/LOG4J2-445";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFtpUrl() throws Exception {
        final URL url = new URL("ftp://user001:secretpassword@private.ftp-servers.example.com/mydirectory/myfile.txt");
        final String expected = "/mydirectory/myfile.txt";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFtpUrlWithPlusCharacters() throws Exception {
        final URL url = new URL("ftp://user001:secretpassword@private.ftp-servers.example.com/my+directory/my+file.txt");
        final String expected = "/my directory/my file.txt";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testFindInPackageFromDirectoryPath(@TempDir final File baseDir) throws Exception {
        try (final URLClassLoader cl = compileAndCreateClassLoader("1", baseDir)) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(cl);
            resolverUtil.findInPackage(new PluginRegistry.PluginTest(), "customplugin1");
            assertEquals(1, resolverUtil.getClasses().size(), "Class not found in packages");
            assertEquals(cl.loadClass("customplugin1.FixedString1"), resolverUtil.getClasses().iterator().next(), "Unexpected class resolved");
        }
    }

    @Test
    public void testFindInPackageFromJarPath(@TempDir final File baseDir) throws Exception {
        try (final URLClassLoader cl = compileJarAndCreateClassLoader("2", baseDir)) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(cl);
            resolverUtil.findInPackage(new PluginRegistry.PluginTest(), "customplugin2");
            assertEquals(1, resolverUtil.getClasses().size(), "Class not found in packages");
            assertEquals(cl.loadClass("customplugin2.FixedString2"), resolverUtil.getClasses().iterator().next(), "Unexpected class resolved");
        }
    }

    static URLClassLoader compileJarAndCreateClassLoader(final String suffix, final File baseDir) throws IOException {
        final File workDir = compile(suffix, baseDir);
        final File jarFile = new File(workDir, "customplugin" + suffix + ".jar");
        final URI jarURI = jarFile.toURI();
        createJar(jarURI, workDir, new File(workDir,
              "customplugin" + suffix + "/FixedString" + suffix + ".class"));
        return URLClassLoader.newInstance(new URL[] {jarURI.toURL()});
    }

    static URLClassLoader compileAndCreateClassLoader(final String suffix, final File baseDir) throws IOException {
        final File workDir = compile(suffix, baseDir);
        return URLClassLoader.newInstance(new URL[] {workDir.toURI().toURL()});
    }

    static File compile(final String suffix, final File baseDir) throws IOException {
        final File orig = new File("target/test-classes/customplugin/FixedString.java.source");
        final File workDir = new File(baseDir, "resolverutil" + suffix);
        final File f = new File(workDir, "customplugin" + suffix + "/FixedString" + suffix + ".java");
        final File parent = f.getParentFile();
        if (!parent.exists()) {
          assertTrue(f.getParentFile().mkdirs(), "Create customplugin" + suffix + " folder KO");
        }

        final String content = new String(Files.readAllBytes(orig.toPath()))
          .replaceAll("FixedString", "FixedString" + suffix)
          .replaceAll("customplugin", "customplugin" + suffix);
        Files.write(f.toPath(), content.getBytes());

        compile(f);
        return workDir;
    }

    static void createJar(final URI jarURI, final File workDir, final File f) throws IOException {
        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        final URI uri = URI.create("jar:file://" + jarURI.getRawPath());
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            final Path path = zipfs.getPath(workDir.toPath().relativize(f.toPath()).toString());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.copy(f.toPath(),
                   path,
                   StandardCopyOption.REPLACE_EXISTING );
        }
    }

    static void compile(final File f) throws IOException {
        // set up compiler
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final List<String> errors = new ArrayList<>();
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(f));

            // compile generated source
            // (switch off annotation processing: no need to create Log4j2Plugins.dat)
            final List<String> options = List.of("-proc:none");
            compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

            // check we don't have any compilation errors
            for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(String.format("Compile error at line %d, column %d: %s%n", diagnostic.getLineNumber(),
                        diagnostic.getColumnNumber(), diagnostic.getMessage(Locale.getDefault())));
                }
            }
        }
        assertThat(errors).isEmpty();
    }

}
