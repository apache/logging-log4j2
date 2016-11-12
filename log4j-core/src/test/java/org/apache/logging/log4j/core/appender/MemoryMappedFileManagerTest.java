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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

/**
 * Tests the MemoryMappedFileManager class.
 *
 * @since 2.1
 */
public class MemoryMappedFileManagerTest {

    @Test
    public void testRemapAfterInitialMapSizeExceeded() throws IOException {
        final int mapSize = 64; // very small, on purpose
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        assertEquals(0, file.length());

        final boolean append = false;
        final boolean immediateFlush = false;
        try (final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(file.getAbsolutePath(),
                append, immediateFlush, mapSize, null, null)) {
            byte[] msg;
            for (int i = 0; i < 1000; i++) {
                msg = ("Message " + i + "\n").getBytes();
                manager.write(msg, 0, msg.length, false);
            }

        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            for (int i = 0; i < 1000; i++) {
                assertNotNull("line", line);
                assertTrue("line incorrect", line.contains("Message " + i));
                line = reader.readLine();
            }
        }
    }

    @Test
    public void testAppendDoesNotOverwriteExistingFile() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        assertEquals(0, file.length());

        final int initialLength = 4 * 1024;

        // create existing file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[initialLength], 0, initialLength);
            fos.flush();
        }
        assertEquals("all flushed to disk", initialLength, file.length());

        final boolean isAppend = true;
        final boolean immediateFlush = false;
        try (final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(file.getAbsolutePath(),
                isAppend, immediateFlush, MemoryMappedFileManager.DEFAULT_REGION_LENGTH, null, null)) {
            manager.write(new byte[initialLength], 0, initialLength);
        }
        final int expected = initialLength * 2;
        assertEquals("appended, not overwritten", expected, file.length());
    }
}