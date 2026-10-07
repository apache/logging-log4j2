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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZipCompressActionTest {

    @Test
    void testRejectsCompressionLevelBelowRange(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "invalid-low.log");
        File dest = new File(tempDir, "invalid-low.log.zip");
        writeContent(source, "test data");

        assertThrows(IllegalArgumentException.class, () -> new ZipCompressAction(source, dest, true, -2).execute());
    }

    @Test
    void testRejectsCompressionLevelAboveRange(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "invalid-high.log");
        File dest = new File(tempDir, "invalid-high.log.zip");
        writeContent(source, "test data");

        assertThrows(IllegalArgumentException.class, () -> new ZipCompressAction(source, dest, true, 10).execute());
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
    void testStaticExecuteReturnsFalseForMissingSourceBeforeValidatingLevel(@TempDir File tempDir) throws IOException {
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
}
