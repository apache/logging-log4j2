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

class GzCompressActionTest {

    @Test
    void testRejectsCompressionLevelLowerThanDefault(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "invalid-low.log");
        File dest = new File(tempDir, "invalid-low.log.gz");
        writeContent(source, "test data");

        assertThrows(IllegalArgumentException.class, () -> new GzCompressAction(source, dest, true, -2).execute());
    }

    @Test
    void testRejectsCompressionLevelHigherThanBest(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "invalid-high.log");
        File dest = new File(tempDir, "invalid-high.log.gz");
        writeContent(source, "test data");

        assertThrows(IllegalArgumentException.class, () -> new GzCompressAction(source, dest, true, 10).execute());
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
}
