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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GzCompressActionTest {

    @Test
    void testFallsBackFromUnsupportedCompressionLevels(@TempDir File tempDir) throws IOException {
        assertFallsBackToDefault(tempDir, "invalid-low.log", -2);
        assertFallsBackToDefault(tempDir, "invalid-high.log", 10);
    }

    @Test
    void testAcceptsDeflaterRangeBounds(@TempDir File tempDir) throws IOException {
        File defaultSource = new File(tempDir, "default.log");
        File defaultDestination = new File(tempDir, "default.log.gz");
        File maximumSource = new File(tempDir, "maximum.log");
        File maximumDestination = new File(tempDir, "maximum.log.gz");
        writeContent(defaultSource, "default compression level");
        writeContent(maximumSource, "maximum compression level");

        assertTrue(
                new GzCompressAction(defaultSource, defaultDestination, true, Deflater.DEFAULT_COMPRESSION).execute());
        assertTrue(new GzCompressAction(maximumSource, maximumDestination, true, Deflater.BEST_COMPRESSION).execute());
    }

    @Test
    void testCompression(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test.log");
        File dest = new File(tempDir, "test.log.gz");
        writeContent(source, "test data");

        GzCompressAction action = new GzCompressAction(source, dest, true, Deflater.DEFAULT_COMPRESSION);

        assertTrue(action.execute());
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
        assertFalse(source.exists(), "Source file must be deleted after compression");
    }

    @Test
    void testLegacyConstructor(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test-legacy.log");
        File dest = new File(tempDir, "test-legacy.log.gz");
        writeContent(source, "legacy test data");

        GzCompressAction action = new GzCompressAction(source, dest, true);

        assertTrue(action.execute());
        assertTrue(dest.exists(), "Compressed file must exist");
        assertFalse(source.exists(), "Source file must be deleted");
    }

    private static void writeContent(final File file, final String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    private static void assertFallsBackToDefault(
            final File tempDir, final String sourceName, final int compressionLevel) throws IOException {
        final File source = new File(tempDir, sourceName);
        final File destination = new File(tempDir, sourceName + ".gz");
        final File defaultDestination = new File(tempDir, sourceName + ".default.gz");
        final byte[] content = new byte[65536];
        final Random random = new Random(0);
        random.nextBytes(content);
        // Repeated blocks with varying distances distinguish the compression levels.
        for (int i = 1024; i < content.length; i += 128) {
            System.arraycopy(content, random.nextInt(i - 128), content, i, 128);
        }
        Files.write(source.toPath(), content);

        assertTrue(new GzCompressAction(source, defaultDestination, false, Deflater.DEFAULT_COMPRESSION).execute());
        final byte[] defaultBytes = Files.readAllBytes(defaultDestination.toPath());
        for (int level = Deflater.NO_COMPRESSION; level <= Deflater.BEST_COMPRESSION; level++) {
            if (level == 6) { // The Deflater default is level 6.
                continue;
            }
            assertTrue(new GzCompressAction(source, destination, false, level).execute());
            assertFalse(
                    Arrays.equals(defaultBytes, Files.readAllBytes(destination.toPath())),
                    "Test data must distinguish the default from level " + level);
        }
        assertTrue(new GzCompressAction(source, destination, true, compressionLevel).execute());
        assertTrue(destination.exists());
        assertFalse(source.exists());
        assertArrayEquals(defaultBytes, Files.readAllBytes(destination.toPath()));
    }

    @Test
    void testDeprecatedClassKeepsPublicApi(@TempDir File tempDir) {
        final File source = new File(tempDir, "source.log");
        final File destination = new File(tempDir, "source.log.gz");

        final GzCompressAction action = new GzCompressAction(source, destination, true, 5);

        assertEquals(source, action.getSource());
        assertEquals(destination, action.getDestination());
        assertTrue(action.isDeleteSource());
        assertEquals("GzCompressAction[" + source + " to " + destination + ", deleteSource=true]", action.toString());
    }
}
