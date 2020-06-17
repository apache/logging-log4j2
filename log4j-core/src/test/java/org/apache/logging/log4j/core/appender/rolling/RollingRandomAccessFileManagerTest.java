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

import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

import static org.apache.logging.log4j.hamcrest.FileMatchers.beforeNow;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasLength;
import static org.apache.logging.log4j.hamcrest.FileMatchers.isEmpty;
import static org.apache.logging.log4j.hamcrest.FileMatchers.lastModified;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests the RollingRandomAccessFileManager class.
 */
public class RollingRandomAccessFileManagerTest {

    /**
     * Test method for
     * {@link org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager#writeBytes(byte[], int, int)}
     */
    @Test
    public void testWrite_multiplesOfBufferSize() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final OutputStream os = NullOutputStream.getInstance();
            final boolean append = false;
            final boolean flushNow = false;
            final long triggerSize = Long.MAX_VALUE;
            final long time = System.currentTimeMillis();
            final TriggeringPolicy triggerPolicy = new SizeBasedTriggeringPolicy(triggerSize);
            final RolloverStrategy rolloverStrategy = null;
            final RollingRandomAccessFileManager manager = new RollingRandomAccessFileManager(null, raf,
                    file.getName(), Strings.EMPTY, os, append, flushNow,
                    RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE, triggerSize, time, triggerPolicy, rolloverStrategy,
                    null, null, null, null, null, true);

            final int size = RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3;
            final byte[] data = new byte[size];
            manager.write(data, 0, data.length, flushNow); // no buffer overflow exception

            // buffer is full but not flushed yet
            assertEquals(RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3, raf.length());
        }
    }

    /**
     * Test method for
     * {@link org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager#writeBytes(byte[], int, int)} .
     */
    @Test
    public void testWrite_dataExceedingBufferSize() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final OutputStream os = NullOutputStream.getInstance();
            final boolean append = false;
            final boolean flushNow = false;
            final long triggerSize = 0;
            final long time = System.currentTimeMillis();
            final TriggeringPolicy triggerPolicy = new SizeBasedTriggeringPolicy(triggerSize);
            final RolloverStrategy rolloverStrategy = null;
            final RollingRandomAccessFileManager manager = new RollingRandomAccessFileManager(null, raf,
                    file.getName(), Strings.EMPTY, os, append, flushNow,
                    RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE, triggerSize, time, triggerPolicy, rolloverStrategy,
                    null, null, null, null, null, true);

            final int size = RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3 + 1;
            final byte[] data = new byte[size];
            manager.write(data, 0, data.length, flushNow); // no exception
            assertEquals(RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE * 3 + 1, raf.length());

            manager.flush();
            assertEquals(size, raf.length()); // all data written to file now
        }
    }

    @Test
    public void testConfigurableBufferSize() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final OutputStream os = NullOutputStream.getInstance();
            final boolean append = false;
            final boolean flushNow = false;
            final long triggerSize = 0;
            final long time = System.currentTimeMillis();
            final TriggeringPolicy triggerPolicy = new SizeBasedTriggeringPolicy(triggerSize);
            final int bufferSize = 4 * 1024;
            assertNotEquals(bufferSize, RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE);
            final RolloverStrategy rolloverStrategy = null;
            final RollingRandomAccessFileManager manager = new RollingRandomAccessFileManager(null, raf,
                    file.getName(), Strings.EMPTY, os, append, flushNow, bufferSize, triggerSize, time, triggerPolicy,
                    rolloverStrategy, null, null, null, null, null, true);

            // check the resulting buffer size is what was requested
            assertEquals(bufferSize, manager.getBufferSize());
        }
    }

    @Test
    public void testAppendDoesNotOverwriteExistingFile() throws IOException {
        final boolean isAppend = true;
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        assertThat(file, isEmpty());

        final byte[] bytes = new byte[4 * 1024];

        // create existing file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
        } finally {
            Closer.closeSilently(fos);
        }
        assertThat("all flushed to disk", file, hasLength(bytes.length));

        final boolean immediateFlush = true;
        final RollingRandomAccessFileManager manager = RollingRandomAccessFileManager.getRollingRandomAccessFileManager(
                //
                file.getAbsolutePath(), Strings.EMPTY, isAppend, immediateFlush,
                RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE, new SizeBasedTriggeringPolicy(Long.MAX_VALUE), //
                null, null, null, null, null, null, null);
        manager.write(bytes, 0, bytes.length, immediateFlush);
        final int expected = bytes.length * 2;
        assertThat("appended, not overwritten", file, hasLength(expected));
    }

    @Test
    public void testFileTimeBasedOnSystemClockWhenAppendIsFalse() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        LockSupport.parkNanos(1000000); // 1 millisec

        // append is false deletes the file if it exists
        final boolean isAppend = false;
        final long expectedMin = System.currentTimeMillis();
        final long expectedMax = expectedMin + 500;
        assertThat(file, lastModified(lessThanOrEqualTo(expectedMin)));

        final RollingRandomAccessFileManager manager = RollingRandomAccessFileManager.getRollingRandomAccessFileManager(
                //
                file.getAbsolutePath(), Strings.EMPTY, isAppend, true,
                RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE, new SizeBasedTriggeringPolicy(Long.MAX_VALUE), //
                null, null, null, null, null, null, null);
        assertTrue(manager.getFileTime() < expectedMax);
        assertTrue(manager.getFileTime() >= expectedMin);
    }

    @Test
    public void testFileTimeBasedOnFileModifiedTimeWhenAppendIsTrue() throws IOException {
        final File file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        LockSupport.parkNanos(1000000); // 1 millisec

        final boolean isAppend = true;
        assertThat(file, lastModified(beforeNow()));

        final RollingRandomAccessFileManager manager = RollingRandomAccessFileManager.getRollingRandomAccessFileManager(
                //
                file.getAbsolutePath(), Strings.EMPTY, isAppend, true,
                RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE, new SizeBasedTriggeringPolicy(Long.MAX_VALUE), //
                null, null, null, null, null, null, null);
        assertThat(file, lastModified(equalTo(manager.getFileTime())));
    }

    @Test
    public void testRolloverRetainsFileAttributes() throws Exception {

        // Short-circuit if host doesn't support file attributes.
        if (!FileUtils.isFilePosixAttributeViewSupported()) {
            return;
        }

        // Create the initial file.
        final File file = File.createTempFile("log4j2", "test");
        LockSupport.parkNanos(1000000); // 1 millisec

        // Set the initial file attributes.
        final String filePermissionsString = "rwxrwxrwx";
        final Set<PosixFilePermission> filePermissions =
                PosixFilePermissions.fromString(filePermissionsString);
        FileUtils.defineFilePosixAttributeView(file.toPath(), filePermissions, null, null);

        // Create the manager.
        final RolloverStrategy rolloverStrategy = DefaultRolloverStrategy
                .newBuilder()
                .setMax("7")
                .setMin("1")
                .setFileIndex("max")
                .setStopCustomActionsOnError(false)
                .setConfig(new DefaultConfiguration())
                .build();
        final RollingRandomAccessFileManager manager =
                RollingRandomAccessFileManager.getRollingRandomAccessFileManager(
                        file.getAbsolutePath(),
                        Strings.EMPTY,
                        true,
                        true,
                        RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE,
                        new SizeBasedTriggeringPolicy(Long.MAX_VALUE),
                        rolloverStrategy,
                        null,
                        null,
                        filePermissionsString,
                        null,
                        null,
                        null);
        assertNotNull(manager);
        manager.initialize();

        // Trigger a rollover.
        manager.rollover();

        // Verify the rolled over file attributes.
        final Set<PosixFilePermission> actualFilePermissions = Files
                .getFileAttributeView(
                        Paths.get(manager.getFileName()),
                        PosixFileAttributeView.class)
                .readAttributes()
                .permissions();
        assertEquals(filePermissions, actualFilePermissions);

    }

}
