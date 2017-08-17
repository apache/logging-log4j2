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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MemoryMappedFileManagerConcurrentWriteTest {

    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final int TOTAL_WRITE_SIZE = 100 * 1024 * 1014; // 100 MB
    private static final int AVERAGE_DATA_SIZE = 10;
    private static final int ITERATIONS_PER_THREAD = TOTAL_WRITE_SIZE / NCPU / AVERAGE_DATA_SIZE;

    private File file;
    private MemoryMappedFileManager manager;
    private CountDownLatch finishLatch;
    private volatile boolean startFlag;

    @Before
    public void prepareManager() throws IOException {
        file = File.createTempFile("log4j2", "test");
        file.deleteOnExit();
        assertEquals(0, file.length());
        final boolean append = false;
        final boolean immediateFlush = false;
        final int mapSize = 1024;
        manager = MemoryMappedFileManager.getFileManager(file.getAbsolutePath(), append, immediateFlush, mapSize,
                null, null);
    }

    @Test(timeout = 100_000)
    public void testConcurrentWrite() throws InterruptedException, IOException {
        finishLatch = new CountDownLatch(NCPU);
        startFlag = false;
        int byteBufferThreads = NCPU / 2;
        int i = 0;
        for (; i < byteBufferThreads; i++) {
            new Thread(new ByteBufferWriter(i)).start();
        }
        for (; i < NCPU; i++) {
            new Thread(new ByteArrayWriter(i)).start();
        }
        // Wait for all threads to start
        Thread.sleep(100);
        startFlag = true;
        finishLatch.await();
        System.out.println("Finish write");
        manager.close();
        verifyResults();
    }

    private class ByteBufferWriter implements Runnable {
        private final ByteBuffer buffer = ByteBuffer.allocate(12);

        ByteBufferWriter(int i) {
            buffer.putLong(i);
        }

        @Override
        public void run() {
            try {
                doRun();
            } finally {
                finishLatch.countDown();
            }
        }

        private void doRun() {
            awaitStart();
            for (int i = 0; i < ITERATIONS_PER_THREAD;) {
                for (int phase = 0; phase <= 4; phase++, i++) {
                    buffer.clear();
                    for (int j = 0; j < phase; j++) {
                        buffer.put(8 + j, (byte) phase);
                    }
                    buffer.limit(8 + phase);
                    manager.writeBytes(buffer);
                }
            }
        }
    }

    private void awaitStart() {
        // Await starts with spin loop, to test concurrent writers hitting the manager at the same time
        while (!startFlag) {
            ThreadHints.onSpinWait();
        }
    }

    private class ByteArrayWriter implements Runnable {
        private final byte[] buffer = new byte[12];

        ByteArrayWriter(int i) {
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            bb.putLong(i);
        }

        @Override
        public void run() {
            try {
                doRun();
            } finally {
                finishLatch.countDown();
            }
        }

        private void doRun() {
            awaitStart();
            for (int i = 0; i < ITERATIONS_PER_THREAD;) {
                for (int phase = 0; phase <= 4; phase++, i++) {
                    for (int j = 0; j < phase; j++) {
                        buffer[8 + j] = (byte) phase;
                    }
                    manager.writeBytes(buffer, 0, 8 + phase);
                }
            }
        }
    }

    private void verifyResults() throws IOException {
        final long fileLength = file.length();
        long readBytes = 0;
        final int[] threadPhases = new int[NCPU];
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while (readBytes != fileLength) {
                long thread = in.readLong();
                if (thread > Integer.MAX_VALUE || thread < Integer.MIN_VALUE) {
                    Assert.fail("readBytes: " + readBytes + ", thread: " + thread +
                            ", surrounding: " + printFileRegion(Math.max(readBytes - 100, 0), 150));
                }
                int threadPhase = threadPhases[(int) thread];
                for (int i = 0; i < threadPhase; i++) {
                    Assert.assertEquals(in.read(), threadPhase);
                }
                readBytes += 8 + threadPhase;
                threadPhases[(int) thread] = threadPhase < 4 ? threadPhase + 1 : 0;
            }
        }
    }

    private String printFileRegion(long start, int len) throws IOException {
        MappedByteBuffer map = new RandomAccessFile(file, "r").getChannel()
                .map(FileChannel.MapMode.READ_ONLY, start, len);
        StringBuilder sb = new StringBuilder();
        while (map.remaining() > 0) {
            sb.append((char) ('0' + map.get()));
        }
        return sb.toString();
    }
}
