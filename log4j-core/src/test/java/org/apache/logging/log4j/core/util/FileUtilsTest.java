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

import org.junit.Test;

/**
 * Tests the FileUtils class.
 */
public class FileUtilsTest {

    private static final String LOG4J_CONFIG_WITH_PLUS = "log4j+config+with+plus+characters.xml";

    @Test
    public void testFileFromUriWithPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/log4j+config+with+plus+characters.xml";
        final URI uri = new URI(config);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue("file exists", file.exists());
    }

    @Test
    public void testFileFromUriWithSpacesAndPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/s%20p%20a%20c%20e%20s/log4j%2Bconfig%2Bwith%2Bplus%2Bcharacters.xml";
        final URI uri = new URI(config);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue("file exists", file.exists());
    }

    /**
     * Helps figure out why {@link #testFileFromUriWithPlusCharactersInName()} fails in Jenkins but asserting different
     * parts of the implementation of {@link FileUtils#fileFromUri(URI)}.
     */
    @Test
    public void testFileExistsWithPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/log4j+config+with+plus+characters.xml";
        final File file = new File(config);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue("file exists", file.exists());
        //
        final URI uri1 = new URI(config);
        assertNull(uri1.getScheme());
        //
        final URI uri2 = new File(uri1.getPath()).toURI();
        assertNotNull(uri2);
        assertTrue("URI \"" + uri2 + "\" does not end with \"" + LOG4J_CONFIG_WITH_PLUS + "\"", uri2.toString()
                .endsWith(LOG4J_CONFIG_WITH_PLUS));
        //
        final String fileName = uri2.toURL().getFile();
        assertTrue("File name \"" + fileName + "\" does not end with \"" + LOG4J_CONFIG_WITH_PLUS + "\"",
                fileName.endsWith(LOG4J_CONFIG_WITH_PLUS));
    }

    @Test
    public void testFileFromUriWithPlusCharactersConvertedToSpacesIfFileDoesNotExist() throws Exception {
        final String config = "NON-EXISTING-PATH/this+file+does+not+exist.xml";
        final URI uri = new URI(config);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals("this file does not exist.xml", file.getName());
        assertFalse("file does not exist", file.exists());
    }

}
