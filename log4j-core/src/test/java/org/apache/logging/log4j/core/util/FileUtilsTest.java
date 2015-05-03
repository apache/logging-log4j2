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

package org.apache.logging.log4j.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Tests the FileUtils class.
 */
public class FileUtilsTest {

    @Test
    public void testFileFromUriWithPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/log4j+config+with+plus+characters.xml";
        final URI uri = new URI(config);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals("log4j+config+with+plus+characters.xml", file.getName());
        assertTrue("file exists", file.exists());
    }

    /**
     * Help figure out why {@link #testFileFromUriWithPlusCharactersInName()} fails in Jenkins but asserting different
     * parts of the implementation of {@link FileUtils#fileFromUri(URI)}.
     */
    @Test
    public void testFileExistsWithPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/log4j+config+with+plus+characters.xml";
        final File file = new File(config);
        assertEquals("log4j+config+with+plus+characters.xml", file.getName());
        assertTrue("file exists", file.exists());
        //
        final URI uri = new URI(config);
        assertNull(uri.getScheme());
    }

    @Test
    public void testFileFromUriWithPlusCharactersConvertedToSpacesIfFileDoesNotExist() throws Exception {
        final String config = "NON-EXISTING-PATH/this+file+does+not+exist.xml";
        final URI uri = new URI(config);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals("this file does not exist.xml", file.getName());
        assertFalse("file does not exist", file.exists());
    }

    @Test
    public void testGetCorrectedFilePathUriWithoutBackslashes() throws URISyntaxException {
        final String config = "file:///path/to/something/on/unix";
        final URI uri = FileUtils.getCorrectedFilePathUri(config);

        assertNotNull("The URI should not be null.", uri);
        assertEquals("The URI is not correct.", "file:///path/to/something/on/unix", uri.toString());
    }

    @Test
    public void testGetCorrectedFilePathUriWithBackslashes() throws URISyntaxException {
        final String config = "file:///D:\\path\\to\\something/on/windows";
        final URI uri = FileUtils.getCorrectedFilePathUri(config);

        assertNotNull("The URI should not be null.", uri);
        assertEquals("The URI is not correct.", "file:///D:/path/to/something/on/windows", uri.toString());
    }
}
