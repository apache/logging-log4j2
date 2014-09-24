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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.core.util.Closer;
import org.junit.Test;

import static org.junit.Assert.*;

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
        final boolean force = false;
        final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(file.getAbsolutePath(), append,
                force, mapSize, null, null);

        byte[] msg;

        for (int i = 0; i < 1000; i++) {
            msg = ("Message " + i + "\n").getBytes();
            manager.write(msg, 0, msg.length);
        }

        manager.release();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();

            for (int i = 0; i < 1000; i++) {
                assertNotNull("line", line);
                assertTrue("line incorrect", line.contains("Message " + i));
                line = reader.readLine();
            }
        } finally {
            Closer.close(reader);
        }
    }

    @Test
    public void testAppendDoesNotOverwriteExistingFile() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        assertEquals(0, file.length());

        final int initialLength = 4 * 1024;

        // create existing file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(new byte[initialLength], 0, initialLength);
            fos.flush();
        } finally {
            fos.close();
        }
        assertEquals("all flushed to disk", initialLength, file.length());

        final boolean isAppend = true;
        final boolean isForce = false;
        final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(file.getAbsolutePath(),
                isAppend, isForce, MemoryMappedFileManager.DEFAULT_REGION_LENGTH, null, null);

        manager.write(new byte[initialLength], 0, initialLength);
        manager.release();
        final int expected = initialLength * 2;
        assertEquals("appended, not overwritten", expected, file.length());
    }
}