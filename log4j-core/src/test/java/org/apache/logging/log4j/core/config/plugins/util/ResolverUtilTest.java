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

import java.net.URL;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the ResolverUtil class.
 */
public class ResolverUtilTest {

    @Test
    public void testExtractPathFromJarUrl() throws Exception {
        URL url = new URL("jar:file:/C:/Users/me/.m2/repository/junit/junit/4.11/junit-4.11.jar!/org/junit/Test.class");
        String expected = "/C:/Users/me/.m2/repository/junit/junit/4.11/junit-4.11.jar";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromJarUrlNotDecodedIfFileExists() throws Exception {
        final String existingFile = "/log4j+config+with+plus+characters.xml";
        URL url = ResolverUtilTest.class.getResource(existingFile);
        if (!url.getProtocol().equals("jar")) {
            // create fake jar: URL that resolves to existing file
            url = new URL("jar:" + url.toExternalForm() + "!/some/entry");
        }
        final String actual = new ResolverUtil().extractPath(url);
        assertTrue("should not be decoded: " + actual, actual.endsWith(existingFile));
    }

    @Test
    public void testExtractPathFromJarUrlDecodedIfFileDoesNotExist() throws Exception {
        URL url = new URL("jar:file:/path+with+plus/file+does+not+exist.jar!/some/file");
        String expected = "/path with plus/file does not exist.jar";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFileUrl() throws Exception {
        URL url = new URL("file:/C:/Users/me/workspace/log4j2/log4j-core/target/test-classes/log4j2-config.xml");
        String expected = "/C:/Users/me/workspace/log4j2/log4j-core/target/test-classes/log4j2-config.xml";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFileUrlNotDecodedIfFileExists() throws Exception {
        final String existingFile = "/log4j+config+with+plus+characters.xml";
        URL url = ResolverUtilTest.class.getResource(existingFile);
        assertTrue("should be file url but was " + url, "file".equals(url.getProtocol()));

        final String actual = new ResolverUtil().extractPath(url);
        assertTrue("should not be decoded: " + actual, actual.endsWith(existingFile));
    }

    @Test
    public void testExtractPathFromFileUrlDecodedIfFileDoesNotExist() throws Exception {
        URL url = new URL("file:///path+with+plus/file+does+not+exist.xml");
        String expected = "/path with plus/file does not exist.xml";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Ignore
    @Test
    public void testExtractPathFromVfszipUrl() throws Exception {
        // need to install URLStreamHandlerFactory to prevent "unknown protocol" MalformedURLException
        URL url = new URL(
                "vfszip:/home2/jboss-5.0.1.CR2/jboss-as/server/ais/ais-deploy/myear.ear/mywar.war/WEB-INF/some.xsd");
        String expected = "/home2/jboss-5.0.1.CR2/jboss-as/server/ais/ais-deploy/myear.ear/mywar.war/WEB-INF/some.xsd";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Ignore
    @Test
    public void testExtractPathFromVfszipUrlWithPlusCharacters()
            throws Exception {
        // need to install URLStreamHandlerFactory to prevent "unknown protocol" MalformedURLException
        URL url = new URL("vfszip:/path+with+plus/file+name+with+plus.xml");
        String expected = "/path+with+plus/file+name+with+plus.xml";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Ignore
    @Test
    public void testExtractPathFromResourceBundleUrl() throws Exception {
        // need to install URLStreamHandlerFactory to prevent "unknown protocol" MalformedURLException
        URL url = new URL("resourcebundle:/some/path/some/file.properties");
        String expected = "/some/path/some/file.properties";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Ignore
    @Test
    public void testExtractPathFromResourceBundleUrlWithPlusCharacters() throws Exception {
        // need to install URLStreamHandlerFactory to prevent "unknown protocol" MalformedURLException
        URL url = new URL("resourcebundle:/some+path/some+file.properties");
        String expected = "/some+path/some+file.properties";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromHttpUrl() throws Exception {
        URL url = new URL("http://java.sun.com/index.html#chapter1");
        String expected = "/index.html";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromHttpUrlWithPlusCharacters() throws Exception {
        URL url = new URL("http://www.server.com/path+with+plus/file+name+with+plus.jar!/org/junit/Test.class");
        String expected = "/path with plus/file name with plus.jar";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromHttpsComplexUrl() throws Exception {
        URL url = new URL("https://issues.apache.org/jira/browse/LOG4J2-445?focusedCommentId=13862479&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13862479");
        String expected = "/jira/browse/LOG4J2-445";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFtpUrl() throws Exception {
        URL url = new URL("ftp://user001:secretpassword@private.ftp-servers.example.com/mydirectory/myfile.txt");
        String expected = "/mydirectory/myfile.txt";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

    @Test
    public void testExtractPathFromFtpUrlWithPlusCharacters() throws Exception {
        URL url = new URL("ftp://user001:secretpassword@private.ftp-servers.example.com/my+directory/my+file.txt");
        String expected = "/my directory/my file.txt";
        assertEquals(expected, new ResolverUtil().extractPath(url));
    }

}
