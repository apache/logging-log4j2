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

package org.apache.logging.log4j.core.helpers;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;

import org.junit.Test;

/**
 * Tests the FileUtils class.
 */
public class FileUtilsTest {

    @Test
    public void testFileFromUriWithPlusCharactersInName() throws Exception {
        String CONFIG = "target/test-classes/log4j+config+with+plus+characters.xml";
        URI uri = new URI(CONFIG);
        File file = FileUtils.fileFromURI(uri);
        assertEquals("log4j+config+with+plus+characters.xml", file.getName());
        assertTrue("file exists", file.exists());
    }

    @Test
    public void testFileFromUriWithPlusCharactersConvertedToSpacesIfFileDoesNotExist()
            throws Exception {
        String CONFIG = "NON-EXISTING-PATH/this+file+does+not+exist.xml";
        URI uri = new URI(CONFIG);
        File file = FileUtils.fileFromURI(uri);
        assertEquals("this file does not exist.xml", file.getName());
        assertFalse("file does not exist", file.exists());
    }
}
