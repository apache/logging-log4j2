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
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;

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

    /**
     * Test FileUtils.updateFileCreationTime() and confirm proper behaviour.
     * 
     * Note: This unit test will only be effective when running on an OS that
     *   supports the creationTime attribute for its filesystems (e.g. Windows).
     * 
     * @throws Exception 
     */
    @Test
    public void testUpdateFileCreationTime() throws Exception {
        final String textFileName = "testFileForCreationTime.txt";
        final String textFilePath = "target/" + textFileName;
        final File file = new File(textFilePath);
        final long currentTimeMS = System.currentTimeMillis();
        try {
            assertEquals(textFileName, file.getName());
            assertFalse("test text file exists ?", file.exists());
            assertTrue("test text file creation failed ?", file.createNewFile());
            final Path filePath = Paths.get(textFilePath);
            final BasicFileAttributeView view = Files.getFileAttributeView(filePath, BasicFileAttributeView.class);
            if (view != null) {
                BasicFileAttributes attributes;
                final long originalAttributeCreationTimeAsMS;
                long attributeCreationTimeAsMS;
                long changeCreationTimeMS;
                try {
                    attributes = view.readAttributes();  // Read attributes after file creation
                    originalAttributeCreationTimeAsMS = attributes.creationTime().toMillis();  // 0L (usually) means unsupported attribute
                    if (originalAttributeCreationTimeAsMS != 0L) {
                        assertTrue("file creationTime not >= test currentTimeMS ?", originalAttributeCreationTimeAsMS >= currentTimeMS);
                        // Attempt to change creationTime with a value under the threshold of 1250ms.
                        changeCreationTimeMS = originalAttributeCreationTimeAsMS - 1000;  // 1000ms is under threshold
                        assertFalse("update reported file creationTime change for parameter under the 1250ms threshold ?",
                                FileUtils.updateFileCreationTime(filePath, changeCreationTimeMS));
                        attributes = view.readAttributes();  // Re-read attributes
                        attributeCreationTimeAsMS = attributes.creationTime().toMillis();
                        assertEquals("current creationTime not equal to original creationTime after false update ?",
                                originalAttributeCreationTimeAsMS, attributeCreationTimeAsMS);
                        // Attempt to change creationTime with a value 120000ms (2 minutes) "in the past".
                        changeCreationTimeMS = currentTimeMS - 120000;  // Set creationTime to two minute in the past
                        assertTrue("update failed to change file creationTime (1st check) ?",
                                FileUtils.updateFileCreationTime(filePath, changeCreationTimeMS));
                        attributes = view.readAttributes();  // Re-read attributes
                        attributeCreationTimeAsMS = attributes.creationTime().toMillis();
                        assertEquals("current creationTime not equal to updated creationTime (1st check) ?",
                                changeCreationTimeMS, attributeCreationTimeAsMS);
                        // Attempt to change creationTime with a value 120000ms (2 minutes) "in the future".
                        changeCreationTimeMS = currentTimeMS + 120000;  // Two minutes in the future
                        assertTrue("update failed to change file creationTime (2nd check) ?",
                                FileUtils.updateFileCreationTime(filePath, changeCreationTimeMS));
                        attributes = view.readAttributes();  // Re-read attributes
                        attributeCreationTimeAsMS = attributes.creationTime().toMillis();
                        assertEquals("current creationTime not equal to updated creationTime (2nd check) ?",
                                changeCreationTimeMS, attributeCreationTimeAsMS);
                    } // Else creationTime unsupported by OS.
                } catch (final UnsupportedOperationException uoe) {
                  // Possible failure on OSes that don't support BasicFileAttributeView.
                }
            }
        } finally {
            file.delete();  // Clean-up
            assertFalse("test text file exists still exists ?", file.exists());
        }
    }

}
