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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.compress.compressors.zstandard.ZstdConstants;
import org.apache.logging.log4j.core.appender.rolling.FileExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZstdCompressActionTest {

    @Test
    void testRejectsCompressionLevelZero(@TempDir File tempDir) {
        // Level 0 is below the minimum supported level (1)
        File source = new File(tempDir, "invalid-zero.log");
        File dest = new File(tempDir, "invalid-zero.log.zst");

        assertThrows(IllegalArgumentException.class, () -> new ZstdCompressAction(source, dest, true, 0));
    }

    /**
     * Negative (fast-compression) Zstd levels are intentionally out of scope for this change.
     * Support may be added in a future release; see the discussion at
     * https://github.com/apache/logging-log4j2/discussions/2950.
     */
    @Test
    void testRejectsUnsupportedNegativeLevel_NotYetImplemented(@TempDir File tempDir) {
        File source = new File(tempDir, "invalid-neg.log");
        File dest = new File(tempDir, "invalid-neg.log.zst");

        assertThrows(IllegalArgumentException.class, () -> new ZstdCompressAction(source, dest, true, -1));
    }

    @Test
    void testRejectsCompressionLevelAboveMax(@TempDir File tempDir) {
        // Level 23 is above the currently supported maximum level (22)
        File source = new File(tempDir, "invalid-high.log");
        File dest = new File(tempDir, "invalid-high.log.zst");

        assertThrows(IllegalArgumentException.class, () -> new ZstdCompressAction(source, dest, true, 23));
    }

    /**
     * Pins the level bounds this test class (and the {@code rolling-file.adoc} documentation) assume.
     * A zstd-jni/commons-compress upgrade that changes these values won't be caught by the other tests here,
     * since they exercise the range relative to {@link ZstdConstants}, not against a fixed expectation.
     * If this fails, update the documented range and this test's hardcoded boundary values accordingly.
     */
    @Test
    void testAssumedZstdLevelBoundsHaveNotChanged() {
        assertEquals(22, ZstdConstants.ZSTD_CLEVEL_MAX);
        assertEquals(3, ZstdConstants.ZSTD_CLEVEL_DEFAULT);
    }

    /**
     * Uses hardcoded boundary values (not {@link ZstdConstants} or {@link ZstdCompressAction#MIN_COMPRESSION_LEVEL})
     * on purpose: referencing the same constants the code under test derives its bounds from would make this
     * test trivially pass regardless of what those constants actually resolve to. If a zstd-jni/commons-compress
     * upgrade shifts the actual default/max, or {@code MIN_COMPRESSION_LEVEL} is changed, this test should fail
     * alongside {@link #testAssumedZstdLevelBoundsHaveNotChanged()} rather than silently re-deriving new bounds
     * and asserting nothing meaningful.
     */
    @Test
    void testAcceptsZstdRangeBounds(@TempDir File tempDir) {
        File source = new File(tempDir, "valid.log");
        File dest = new File(tempDir, "valid.log.zst");

        new ZstdCompressAction(source, dest, true, 1);
        new ZstdCompressAction(source, dest, true, 22);
    }

    @Test
    void testCompression(@TempDir File tempDir) throws IOException {
        File source = new File(tempDir, "test.log");
        File dest = new File(tempDir, "test.log.zst");
        writeContent(source, "test data");

        ZstdCompressAction action = new ZstdCompressAction(source, dest, true, ZstdConstants.ZSTD_CLEVEL_DEFAULT);

        assertTrue(action.execute());
        assertTrue(dest.exists(), "Compressed file must exist after execute()");
        assertFalse(source.exists(), "Source file must be deleted after compression");
    }

    @Test
    void testFileExtensionUnspecifiedLevelMapping() {
        // Verify FileExtension.ZSTD maps log4j2's framework-wide "unspecified compression level" sentinel (-1)
        // to ZSTD_CLEVEL_DEFAULT (3). Passing the literal -1 here, not Deflater.DEFAULT_COMPRESSION: the mapping
        // in FileExtension.ZSTD compares against the literal -1 sentinel value, not against that JDK constant.
        ZstdCompressAction action =
                (ZstdCompressAction) FileExtension.ZSTD.createCompressAction("source.log", "target.log.zst", true, -1);

        // Hardcoded, not ZstdConstants.ZSTD_CLEVEL_DEFAULT: both sides would drift together otherwise,
        // making this assertion trivially true regardless of what the constant actually resolves to.
        assertEquals(3, action.getCompressionLevel());
    }

    private static void writeContent(final File file, final String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
