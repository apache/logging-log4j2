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
package org.apache.logging.log4j.core.appender.rolling.action;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZipCompressActionTest {

    @Test
    void testFallsBackFromUnsupportedCompressionLevels(@TempDir File tempDir) throws IOException {
        assertFallsBackToDefault(tempDir, "invalid-low.log", -2);
        assertFallsBackToDefault(tempDir, "invalid-high.log", 10);
    }

    @Test
    void testAcceptsCompressionLevelRangeBounds(@TempDir File tempDir) throws IOException {
        File defaultSource = new File(tempDir, "default.log");
        File defaultDestination = new File(tempDir, "default.log.zip");
        File noCompressionSource = new File(tempDir, "no-compression.log");
        File noCompressionDestination = new File(tempDir, "no-compression.log.zip");
        File maximumSource = new File(tempDir, "maximum.log");
        File maximumDestination = new File(tempDir, "maximum.log.zip");
        writeContent(defaultSource, "default compression level");
        writeContent(noCompressionSource, "no compression level");
        writeContent(maximumSource, "maximum compression level");

        assertTrue(
                new ZipCompressAction(defaultSource, defaultDestination, true, Deflater.DEFAULT_COMPRESSION).execute());
        assertTrue(new ZipCompressAction(noCompressionSource, noCompressionDestination, true, Deflater.NO_COMPRESSION)
                .execute());
        assertTrue(new ZipCompressAction(maximumSource, maximumDestination, true, Deflater.BEST_COMPRESSION).execute());
    }

    @Test
    void testStaticExecuteReturnsFalseForMissingSourceBeforeResolvingLevel(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "missing.log");
        File destination = new File(tempDir, "missing.log.zip");

        assertFalse(ZipCompressAction.execute(source, destination, true, 10));
        assertFalse(destination.exists());
    }

    @Test
    void testCompression(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test.log");
        File dest = new File(tempDir, "test.log.zip");
        writeContent(source, "test data");

        ZipCompressAction action = new ZipCompressAction(source, dest, true, Deflater.DEFAULT_COMPRESSION);

        assertTrue(action.execute());
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
        assertFalse(source.exists(), "Source file must be deleted after compression");
    }

    private static void writeContent(final File file, final String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    private static void assertFallsBackToDefault(
            final File tempDir, final String sourceName, final int compressionLevel) throws IOException {
        final File source = new File(tempDir, sourceName);
        final File destination = new File(tempDir, sourceName + ".zip");
        final File defaultDestination = new File(tempDir, sourceName + ".default.zip");
        final byte[] content = new byte[65536];
        final Random random = new Random(0);
        random.nextBytes(content);
        // Repeated blocks with varying distances distinguish the compression levels.
        for (int i = 1024; i < content.length; i += 128) {
            System.arraycopy(content, random.nextInt(i - 128), content, i, 128);
        }
        Files.write(source.toPath(), content);

        assertTrue(new ZipCompressAction(source, defaultDestination, false, Deflater.DEFAULT_COMPRESSION).execute());
        final byte[] defaultBytes = readDeflatedEntry(defaultDestination);
        for (int level = Deflater.NO_COMPRESSION; level <= Deflater.BEST_COMPRESSION; level++) {
            if (level == 6) { // The Deflater default is level 6.
                continue;
            }
            assertTrue(new ZipCompressAction(source, destination, false, level).execute());
            assertFalse(
                    Arrays.equals(defaultBytes, readDeflatedEntry(destination)),
                    "Test data must distinguish the default from level " + level);
        }
        assertTrue(new ZipCompressAction(source, destination, true, compressionLevel).execute());
        assertTrue(destination.exists());
        assertFalse(source.exists());
        assertArrayEquals(defaultBytes, readDeflatedEntry(destination));
    }

    private static byte[] readDeflatedEntry(final File archive) throws IOException {
        final byte[] bytes = Files.readAllBytes(archive.toPath());
        final ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final int offset = 30 + (header.getShort(26) & 0xffff) + (header.getShort(28) & 0xffff);
        try (ZipFile zip = new ZipFile(archive)) {
            final int size = Math.toIntExact(zip.entries().nextElement().getCompressedSize());
            // Compare the compressed payload, excluding timestamps in the ZIP headers.
            return Arrays.copyOfRange(bytes, offset, offset + size);
        }
    }

    @Test
    void testDeprecatedClassKeepsPublicApi(@TempDir File tempDir) {
        final File source = new File(tempDir, "source.log");
        final File destination = new File(tempDir, "source.log.zip");

        final ZipCompressAction action = new ZipCompressAction(source, destination, false, 5);

        assertEquals(source, action.getSource());
        assertEquals(destination, action.getDestination());
        assertFalse(action.isDeleteSource());
        assertEquals(5, action.getLevel());
        assertEquals(
                "ZipCompressAction[" + source + " to " + destination + ", level=5, deleteSource=false]",
                action.toString());
    }
}
