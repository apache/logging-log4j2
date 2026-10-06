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
package org.apache.logging.log4j.core.appender.rolling.action.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.logging.log4j.core.appender.rolling.FileExtension;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompressionLevelAwareActionTest {

    @Test
    void testBzip2PassesConfiguredBlockSizeToCompressor(@TempDir File tempDir) throws IOException {
        File source = writeSource(tempDir, "test.log");
        File destination = new File(tempDir, "test.log.bz2");

        assertTrue(new Bzip2CompressAction(source, destination, true, 1).execute());
        assertTrue(destination.exists());
        assertFalse(source.exists());
        assertEquals('1', Files.readAllBytes(destination.toPath())[3]);
    }

    @Test
    void testDeflatePassesConfiguredCompressionLevelToCompressor(@TempDir File tempDir) throws IOException {
        File noCompressionSource = writeRepeatedSource(tempDir, "no-compression.log");
        File noCompressionDestination = new File(tempDir, "no-compression.log.deflate");
        File maximumCompressionSource = writeRepeatedSource(tempDir, "maximum-compression.log");
        File maximumCompressionDestination = new File(tempDir, "maximum-compression.log.deflate");

        assertTrue(new DeflateCompressAction(noCompressionSource, noCompressionDestination, true, 0).execute());
        assertTrue(
                new DeflateCompressAction(maximumCompressionSource, maximumCompressionDestination, true, 9).execute());
        assertTrue(maximumCompressionDestination.length() < noCompressionDestination.length());
    }

    @Test
    void testBzip2ResolvesDefaultLevels(@TempDir File tempDir) throws IOException {
        File defaultSource = writeSource(tempDir, "default.log");
        File defaultDestination = new File(tempDir, "default.log.bz2");
        File zeroSource = writeSource(tempDir, "zero.log");
        File zeroDestination = new File(tempDir, "zero.log.bz2");

        assertTrue(new Bzip2CompressAction(defaultSource, defaultDestination, true, -1).execute());
        assertTrue(new Bzip2CompressAction(zeroSource, zeroDestination, true, 0).execute());
        assertTrue(defaultDestination.exists());
        assertTrue(zeroDestination.exists());
        assertEquals('9', Files.readAllBytes(defaultDestination.toPath())[3]);
        assertEquals('9', Files.readAllBytes(zeroDestination.toPath())[3]);
    }

    @Test
    void testDeflateAcceptsDefaultAndNoCompressionLevels(@TempDir File tempDir) throws IOException {
        File defaultSource = writeSource(tempDir, "default.log");
        File defaultDestination = new File(tempDir, "default.log.deflate");
        File zeroSource = writeSource(tempDir, "zero.log");
        File zeroDestination = new File(tempDir, "zero.log.deflate");

        assertTrue(new DeflateCompressAction(defaultSource, defaultDestination, true, -1).execute());
        assertTrue(new DeflateCompressAction(zeroSource, zeroDestination, true, 0).execute());
        assertTrue(defaultDestination.exists());
        assertTrue(zeroDestination.exists());
    }

    @Test
    void testFileExtensionSelectsLevelAwareActions(@TempDir File tempDir) {
        final Action bzip2Action = FileExtension.BZIP2.createCompressAction(
                new File(tempDir, "test.log").getPath(), new File(tempDir, "test.log.bz2").getPath(), true, 9);
        final Action deflateAction = FileExtension.DEFLATE.createCompressAction(
                new File(tempDir, "test.log").getPath(), new File(tempDir, "test.log.deflate").getPath(), true, 9);

        assertInstanceOf(Bzip2CompressAction.class, bzip2Action);
        assertInstanceOf(DeflateCompressAction.class, deflateAction);
    }

    @Test
    void testInvalidLevelsFailWhenCompressionExecutes(@TempDir File tempDir) {
        assertInvalidCompressionLevel(FileExtension.BZIP2, tempDir, -2);
        assertInvalidCompressionLevel(FileExtension.BZIP2, tempDir, 10);
        assertInvalidCompressionLevel(FileExtension.DEFLATE, tempDir, -2);
        assertInvalidCompressionLevel(FileExtension.DEFLATE, tempDir, 10);
    }

    private static void assertInvalidCompressionLevel(
            final FileExtension extension, final File tempDir, final int level) {
        final Action action = assertDoesNotThrow(() -> extension.createCompressAction(
                new File(tempDir, "invalid.log").getPath(),
                new File(tempDir, "invalid.log." + extension.getExtension()).getPath(),
                true,
                level));

        assertThrows(IllegalArgumentException.class, action::execute);
    }

    private static File writeSource(final File tempDir, final String name) throws IOException {
        final File source = new File(tempDir, name);
        try (FileWriter writer = new FileWriter(source)) {
            writer.write("compression-level test data");
        }
        return source;
    }

    private static File writeRepeatedSource(final File tempDir, final String name) throws IOException {
        final File source = new File(tempDir, name);
        try (FileWriter writer = new FileWriter(source)) {
            for (int i = 0; i < 1_000; i++) {
                writer.write("compression-level test data");
            }
        }
        return source;
    }
}
