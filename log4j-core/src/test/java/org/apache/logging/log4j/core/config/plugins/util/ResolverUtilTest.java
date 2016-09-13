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
import static org.junit.Assert.assertTrue;

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

import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry.PluginTest;
import org.apache.logging.log4j.junit.CleanFolders;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests the ResolverUtil class.
 */
public class ResolverUtilTest {

    static final String WORK_DIR = "target/testpluginsutil";

    @Rule
    public RuleChain chain = RuleChain.outerRule(new CleanFolders(WORK_DIR));
    
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
        if (!url.getProtocol().equals("jar")) {
            // create fake jar: URL that resolves to existing file
            url = new URL("jar:" + url.toExternalForm() + "!/some/entry");
        }
        final String actual = new ResolverUtil().extractPath(url);
        assertTrue("should not be decoded: " + actual, actual.endsWith(existingFile));
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
        assertTrue("should be file url but was " + url, "file".equals(url.getProtocol()));

        final String actual = new ResolverUtil().extractPath(url);
        assertTrue("should not be decoded: " + actual, actual.endsWith(existingFile));
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
    public void testFindInPackageFromDirectoryPath() throws Exception {
        try (final URLClassLoader cl = compileAndCreateClassLoader("1")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(cl);
            resolverUtil.findInPackage(new PluginTest(), "customplugin1");
            assertEquals("Class not found in packages", 1, resolverUtil.getClasses().size());
            assertEquals("Unexpected class resolved", cl.loadClass("customplugin1.FixedString1Layout"),
                    resolverUtil.getClasses().iterator().next());
        }
    }

    @Test
    public void testFindInPackageFromJarPath() throws Exception {
        try (final URLClassLoader cl = compileJarAndCreateClassLoader("2")) {
            final ResolverUtil resolverUtil = new ResolverUtil();
            resolverUtil.setClassLoader(cl);
            resolverUtil.findInPackage(new PluginTest(), "customplugin2");
            assertEquals("Class not found in packages", 1, resolverUtil.getClasses().size());
            assertEquals("Unexpected class resolved", cl.loadClass("customplugin2.FixedString2Layout"),
                    resolverUtil.getClasses().iterator().next());
        }
    }

    static URLClassLoader compileJarAndCreateClassLoader(final String suffix) throws IOException, Exception {
        final File workDir = compile(suffix);
        final File jarFile = new File(workDir, "customplugin" + suffix + ".jar");
        final URI jarURI = jarFile.toURI();
        createJar(jarURI, workDir, new File(workDir,
              "customplugin" + suffix + "/FixedString" + suffix + "Layout.class"));
        return URLClassLoader.newInstance(new URL[] {jarURI.toURL()});
    }

    static URLClassLoader compileAndCreateClassLoader(final String suffix) throws IOException {
        final File workDir = compile(suffix);
        return URLClassLoader.newInstance(new URL[] {workDir.toURI().toURL()});
    }

    static File compile(final String suffix) throws IOException {
        final File orig = new File("target/test-classes/customplugin/FixedStringLayout.java.source");
        final File workDir = new File(WORK_DIR, "resolverutil" + suffix);
        final File f = new File(workDir, "customplugin" + suffix + "/FixedString" + suffix + "Layout.java");
        final File parent = f.getParentFile();
        if (!parent.exists()) {
          assertTrue("Create customplugin" + suffix + " folder KO", f.getParentFile().mkdirs());
        }
  
        final String content = new String(Files.readAllBytes(orig.toPath()))
          .replaceAll("FixedString", "FixedString" + suffix)
          .replaceAll("customplugin", "customplugin" + suffix);
        Files.write(f.toPath(), content.getBytes());
  
        PluginManagerPackagesTest.compile(f);
        return workDir;
    }

    static void createJar(final URI jarURI, final File workDir, final File f) throws Exception {
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

}
