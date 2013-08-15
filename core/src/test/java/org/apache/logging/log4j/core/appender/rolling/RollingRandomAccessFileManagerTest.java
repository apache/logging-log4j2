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

package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.LockSupport;

import org.junit.Test;

/**
 * Tests the RollingRandomAccessFileManager class.
 */
public class RollingRandomAccessFileManagerTest {

    /**
     * Test method for
     * {@link org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager#write(byte[], int, int)}
     * .
     */
    @Test
    public void testWrite_multiplesOfBufferSize() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        final OutputStream os = new RollingRandomAccessFileManager.DummyOutputStream();
        final boolean append = false;
        final boolean flushNow = false;
        final long triggerSize = Long.MAX_VALUE;
        final long time = System.currentTimeMillis();
        final TriggeringPolicy triggerPolicy = new SizeBasedTriggeringPolicy(
                triggerSize);
        final RolloverStrategy rolloverStrategy = null;
        final RollingRandomAccessFileManager manager = new RollingRandomAccessFileManager(raf,
                file.getName(), "", os, append, flushNow, triggerSize, time,
                triggerPolicy, rolloverStrategy, null, null);

        final int size = RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3;
        final byte[] data = new byte[size];
        manager.write(data, 0, data.length); // no buffer overflow exception

        // buffer is full but not flushed yet
        assertEquals(RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 2,
                raf.length());
    }

    /**
     * Test method for
     * {@link org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager#write(byte[], int, int)}
     * .
     */
    @Test
    public void testWrite_dataExceedingBufferSize() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        final OutputStream os = new RollingRandomAccessFileManager.DummyOutputStream();
        final boolean append = false;
        final boolean flushNow = false;
        final long triggerSize = 0;
        final long time = System.currentTimeMillis();
        final TriggeringPolicy triggerPolicy = new SizeBasedTriggeringPolicy(
                triggerSize);
        final RolloverStrategy rolloverStrategy = null;
        final RollingRandomAccessFileManager manager = new RollingRandomAccessFileManager(raf,
                file.getName(), "", os, append, flushNow, triggerSize, time,
                triggerPolicy, rolloverStrategy, null, null);

        final int size = RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3 + 1;
        final byte[] data = new byte[size];
        manager.write(data, 0, data.length); // no exception
        assertEquals(RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3,
                raf.length());

        manager.flush();
        assertEquals(size, raf.length()); // all data written to file now
    }

    @Test
    public void testAppendDoesNotOverwriteExistingFile() throws IOException {
        final boolean isAppend = true;
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        assertEquals(0, file.length());

        final byte[] bytes = new byte[4 * 1024];

        // create existing file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
        } finally {
            fos.close();
        }
        assertEquals("all flushed to disk", bytes.length, file.length());

        final RollingRandomAccessFileManager manager = RollingRandomAccessFileManager
                .getRollingRandomAccessFileManager(
                        //
                        file.getAbsolutePath(), "", isAppend, true,
                        new SizeBasedTriggeringPolicy(Long.MAX_VALUE), //
                        null, null, null);
        manager.write(bytes, 0, bytes.length);
        final int expected = bytes.length * 2;
        assertEquals("appended, not overwritten", expected, file.length());
    }

    @Test
    public void testFileTimeBasedOnSystemClockWhenAppendIsFalse()
            throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        LockSupport.parkNanos(1000000); // 1 millisec

        // append is false deletes the file if it exists
        final boolean isAppend = false;
        final long expectedMin = System.currentTimeMillis();
        final long expectedMax = expectedMin + 50;
        assertTrue(file.lastModified() < expectedMin);

        final RollingRandomAccessFileManager manager = RollingRandomAccessFileManager
                .getRollingRandomAccessFileManager(
                        //
                        file.getAbsolutePath(), "", isAppend, true,
                        new SizeBasedTriggeringPolicy(Long.MAX_VALUE), //
                        null, null, null);
        assertTrue(manager.getFileTime() < expectedMax);
        assertTrue(manager.getFileTime() >= expectedMin);
    }

    @Test
    public void testFileTimeBasedOnFileModifiedTimeWhenAppendIsTrue()
            throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        LockSupport.parkNanos(1000000); // 1 millisec

        final boolean isAppend = true;
        assertTrue(file.lastModified() < System.currentTimeMillis());

        final RollingRandomAccessFileManager manager = RollingRandomAccessFileManager
                .getRollingRandomAccessFileManager(
                        //
                        file.getAbsolutePath(), "", isAppend, true,
                        new SizeBasedTriggeringPolicy(Long.MAX_VALUE), //
                        null, null, null);
        assertEquals(file.lastModified(), manager.getFileTime());
    }

}
