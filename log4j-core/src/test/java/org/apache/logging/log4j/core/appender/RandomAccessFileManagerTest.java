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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the RandomAccessFileManager class.
 */
public class RandomAccessFileManagerTest {

    @TempDir
    File tempDir;

    /**
     * Test method for
     * {@link org.apache.logging.log4j.core.appender.RandomAccessFileManager#writeBytes(byte[], int, int)}.
     */
    @Test
    public void testWrite_multiplesOfBufferSize() throws IOException {
        final File file = new File(tempDir, "testWrite_multiplesOfBufferSize.bin");
        try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final OutputStream os = NullOutputStream.getInstance();
            try (final RandomAccessFileManager manager = new RandomAccessFileManager(
                    null, raf, file.getName(), os,
                    RandomAccessFileManager.DEFAULT_BUFFER_SIZE, null, null, true)) {
                final int size = RandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3;
                final byte[] data = new byte[size];
                manager.write(data); // no buffer overflow exception
                // all data is written if exceeds buffer size
                assertThat(raf.length()).isEqualTo(RandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3);
            }
        }
    }

    /**
     * Test method for
     * {@link org.apache.logging.log4j.core.appender.RandomAccessFileManager#writeBytes(byte[], int, int)}
     * .
     */
    @Test
    public void testWrite_dataExceedingBufferSize() throws IOException {
        final File file = new File(tempDir, "testWrite_dataExceedingBufferSize.bin");
        try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final OutputStream os = NullOutputStream.getInstance();
            final int size = RandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3 + 1;
            try (final RandomAccessFileManager manager = new RandomAccessFileManager(
                    null, raf, file.getName(), os,
                    RandomAccessFileManager.DEFAULT_BUFFER_SIZE, null, null, true)) {
                final byte[] data = new byte[size];
                manager.write(data); // no exception
                // all data is written if exceeds buffer size
                assertThat(raf.length()).isEqualTo(RandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3 + 1);
                assertThat(raf.length()).isEqualTo(size); // all data written to file now
            }
        }
    }

    @Test
    public void testConfigurableBufferSize() throws IOException {
        final File file = new File(tempDir, "testConfigurableBufferSize.bin");
        try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final OutputStream os = NullOutputStream.getInstance();
            final int bufferSize = 4 * 1024;
            assertThat(RandomAccessFileManager.DEFAULT_BUFFER_SIZE).isNotEqualTo(bufferSize);
            try (final RandomAccessFileManager manager = new RandomAccessFileManager(
                    null, raf, file.getName(), os, bufferSize, null, null, true)) {
                // check the resulting buffer size is what was requested
                assertThat(manager.getBufferSize()).isEqualTo(bufferSize);
            }
        }
    }

    @Test
    public void testWrite_dataExceedingMinBufferSize() throws IOException {
        final File file = new File(tempDir, "testWrite_dataExceedingMinBufferSize.bin");
        try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final OutputStream os = NullOutputStream.getInstance();
            final int bufferSize = 1;
            final int size = bufferSize * 3 + 1;
            try (final RandomAccessFileManager manager = new RandomAccessFileManager(
                    null, raf, file.getName(), os, bufferSize, null, null, true)) {
                final byte[] data = new byte[size];
                manager.write(data); // no exception
                // all data is written if exceeds buffer size
                assertThat(raf.length()).isEqualTo(bufferSize * 3 + 1);
                assertThat(raf.length()).isEqualTo(size); // all data written to file now
            }
        }
    }

    @Test
    public void testAppendDoesNotOverwriteExistingFile() throws IOException {
        final boolean isAppend = true;
        final File file = new File(tempDir, "testAppendDoesNotOverwriteExistingFile.bin");
        assertThat(file.length()).isEqualTo(0);

        final byte[] bytes = new byte[4 * 1024];

        // create existing file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes, 0, bytes.length);
            fos.flush();
        }
        assertThat(file.length()).describedAs("all flushed to disk").isEqualTo(bytes.length);

        try (final RandomAccessFileManager manager = RandomAccessFileManager.getFileManager(
                file.getAbsolutePath(), isAppend, true,
                RandomAccessFileManager.DEFAULT_BUFFER_SIZE, null, null, null)) {
            manager.write(bytes, 0, bytes.length, true);
        }
        final int expected = bytes.length * 2;
        assertThat(file.length()).describedAs("appended, not overwritten").isEqualTo(expected);
    }

}
