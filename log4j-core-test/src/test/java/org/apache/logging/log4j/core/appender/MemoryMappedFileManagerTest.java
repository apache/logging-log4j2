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
package org.apache.logging.log4j.core.appender;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the MemoryMappedFileManager class.
 *
 * @since 2.1
 */
class MemoryMappedFileManagerTest {

    @TempDir
    File tempDir;

    @Test
    void testRemapAfterInitialMapSizeExceeded() throws IOException {
        final int mapSize = 64; // very small, on purpose
        final File file = new File(tempDir, "memory-mapped-file.bin");

        final boolean append = false;
        final boolean immediateFlush = false;
        try (final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(
                file.getAbsolutePath(), append, immediateFlush, mapSize, null, null)) {
            byte[] msg;
            for (int i = 0; i < 1000; i++) {
                msg = ("Message " + i + "\n").getBytes();
                manager.write(msg, 0, msg.length, false);
            }
        }

        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            for (int i = 0; i < 1000; i++) {
                assertNotNull(line, "line");
                assertTrue(line.contains("Message " + i), "line incorrect");
                line = reader.readLine();
            }
        }
    }

    @Test
    void testAppendDoesNotOverwriteExistingFile() throws IOException {
        final File file = new File(tempDir, "memory-mapped-file.bin");

        final int initialLength = 4 * 1024;

        // create existing file
        try (final FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[initialLength], 0, initialLength);
            fos.flush();
        }
        assertEquals(initialLength, file.length(), "all flushed to disk");

        final boolean isAppend = true;
        final boolean immediateFlush = false;
        try (final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(
                file.getAbsolutePath(),
                isAppend,
                immediateFlush,
                MemoryMappedFileManager.DEFAULT_REGION_LENGTH,
                null,
                null)) {
            manager.writeBytes(new byte[initialLength], 0, initialLength);
        }
        final int expected = initialLength * 2;
        assertEquals(expected, file.length(), "appended, not overwritten");
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void testWriteHeader(final boolean append, final boolean fileExists) throws IOException {
        final File file = new File(tempDir, "memory-mapped-file.log");
        if (fileExists) {
            Files.write(file.toPath(), "EXISTING\n".getBytes(UTF_8));
        }
        final PatternLayout layout =
                PatternLayout.newBuilder().setHeader("HEADER\n").build();

        try (final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(
                file.getAbsolutePath(), append, false, MemoryMappedFileManager.DEFAULT_REGION_LENGTH, null, layout)) {
            manager.write("DATA\n".getBytes(UTF_8));
        }

        final String expected = (append && fileExists ? "EXISTING\n" : "HEADER\n") + "DATA\n";
        assertEquals(expected, new String(Files.readAllBytes(file.toPath()), UTF_8));
    }
}
