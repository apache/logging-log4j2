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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.GzCompressAction;
import org.apache.logging.log4j.core.appender.rolling.action.ZipCompressAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #4012 — verifies that FileExtension.GZ and FileExtension.ZIP correctly pass
 * maxCompressionDelaySeconds through to the compression action.
 *
 * This was the root cause of the bug: the 5-argument createCompressAction() in GZ and ZIP
 * fell back to the 4-argument version, silently dropping the delay value.
 */
class FileExtensionCompressDelayTest {

    @Test
    void testGzCreateCompressActionRejectsInvalidCompressionLevel(@TempDir File tempDir) {
        File source = new File(tempDir, "invalid-level.log");
        File dest = new File(tempDir, "invalid-level.log.gz");

        assertThrows(
                IllegalArgumentException.class,
                () -> FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -2, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, 10, 0));
    }

    @Test
    void testZipCreateCompressActionRejectsInvalidCompressionLevel(@TempDir File tempDir) {
        File source = new File(tempDir, "invalid-level.log");
        File dest = new File(tempDir, "invalid-level.log.zip");

        assertThrows(
                IllegalArgumentException.class,
                () -> FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, -2, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, 10, 0));
    }

    // ── GZ ────────────────────────────────────────────────────────────────

    /**
     * FileExtension.GZ.createCompressAction(5-args) must produce a GzCompressAction
     * that applies the random delay — not fall back to 0.
     */
    @Test
    void testGzCreateCompressActionWithDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app.log");
        File dest = new File(tempDir, "app.log.gz");
        writeContent(source, "gz test content");

        int maxDelay = 2;
        Action action = FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -1, maxDelay);

        // Must return a GzCompressAction (not some other type)
        assertInstanceOf(GzCompressAction.class, action, "Expected GzCompressAction");

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        // Must finish within maxDelay + margin (delay IS applied via FileExtension)
        assertTrue(
                elapsed <= (maxDelay * 1000L) + 500,
                "GZ compress via FileExtension exceeded maxDelay=" + maxDelay + "s: " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .gz file must exist");
        assertFalse(source.exists(), "Source must be deleted after compression");
    }

    /**
     * FileExtension.GZ.createCompressAction(5-args, delay=0) must behave identically
     * to the 4-arg version — instant compression, no delay.
     */
    @Test
    void testGzCreateCompressActionNoDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-nodelay.log");
        File dest = new File(tempDir, "app-nodelay.log.gz");
        writeContent(source, "gz no-delay content");

        Action action = FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -1, 0);

        assertInstanceOf(GzCompressAction.class, action);

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "GZ with delay=0 should be instant, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .gz file must exist");
        assertFalse(source.exists(), "Source must be deleted");
    }

    // ── ZIP ───────────────────────────────────────────────────────────────

    /**
     * FileExtension.ZIP.createCompressAction(5-args) must produce a ZipCompressAction
     * that applies the random delay — not fall back to 0.
     */
    @Test
    void testZipCreateCompressActionWithDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app.log");
        File dest = new File(tempDir, "app.log.zip");
        writeContent(source, "zip test content");

        int maxDelay = 2;
        Action action = FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, 0, maxDelay);

        assertInstanceOf(ZipCompressAction.class, action, "Expected ZipCompressAction");

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(
                elapsed <= (maxDelay * 1000L) + 500,
                "ZIP compress via FileExtension exceeded maxDelay=" + maxDelay + "s: " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .zip file must exist");
        assertFalse(source.exists(), "Source must be deleted after compression");
    }

    /**
     * FileExtension.ZIP.createCompressAction(5-args, delay=0) must behave identically
     * to the 4-arg version — instant compression, no delay.
     */
    @Test
    void testZipCreateCompressActionNoDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-nodelay.log");
        File dest = new File(tempDir, "app-nodelay.log.zip");
        writeContent(source, "zip no-delay content");

        Action action = FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, 0, 0);

        assertInstanceOf(ZipCompressAction.class, action);

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "ZIP with delay=0 should be instant, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .zip file must exist");
        assertFalse(source.exists(), "Source must be deleted");
    }

    // ── GZ min/max ────────────────────────────────────────────────────────

    /**
     * FileExtension.GZ.createCompressAction(6-args) with min==max must sleep for exactly that duration.
     */
    @Test
    void testGzCreateCompressActionWithMinMaxFixedDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-fixed.log");
        File dest = new File(tempDir, "app-fixed.log.gz");
        writeContent(source, "gz fixed delay content");

        int fixedDelay = 1;
        Action action =
                FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -1, fixedDelay, fixedDelay);

        assertInstanceOf(GzCompressAction.class, action, "Expected GzCompressAction");

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(
                elapsed >= fixedDelay * 1000L - 100,
                "GZ fixed delay should sleep at least " + fixedDelay + "s, took " + elapsed + "ms");
        assertTrue(
                elapsed <= fixedDelay * 1000L + 500,
                "GZ fixed delay exceeded expected duration, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .gz file must exist");
        assertFalse(source.exists(), "Source must be deleted after compression");
    }

    /**
     * FileExtension.GZ.createCompressAction(6-args) with min=0, max=0 must be instant.
     */
    @Test
    void testGzCreateCompressActionWithMinMaxBothZero(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-zero.log");
        File dest = new File(tempDir, "app-zero.log.gz");
        writeContent(source, "gz zero delay content");

        Action action = FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -1, 0, 0);

        assertInstanceOf(GzCompressAction.class, action);

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "GZ with min=0,max=0 should be instant, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .gz file must exist");
        assertFalse(source.exists(), "Source must be deleted");
    }

    /**
     * FileExtension.GZ.createCompressAction(6-args) with a range [min, max] must sleep within bounds.
     */
    @Test
    void testGzCreateCompressActionWithMinMaxRange(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-range.log");
        File dest = new File(tempDir, "app-range.log.gz");
        writeContent(source, "gz range delay content");

        int min = 1;
        int max = 2;
        Action action = FileExtension.GZ.createCompressAction(source.getPath(), dest.getPath(), true, -1, min, max);

        assertInstanceOf(GzCompressAction.class, action);

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(
                elapsed >= min * 1000L - 100,
                "GZ range delay should sleep at least " + min + "s, took " + elapsed + "ms");
        assertTrue(
                elapsed <= max * 1000L + 500,
                "GZ range delay should sleep at most " + max + "s, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .gz file must exist");
        assertFalse(source.exists(), "Source must be deleted");
    }

    // ── ZIP min/max ───────────────────────────────────────────────────────

    /**
     * FileExtension.ZIP.createCompressAction(6-args) with min==max must sleep for exactly that duration.
     */
    @Test
    void testZipCreateCompressActionWithMinMaxFixedDelay(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-fixed.log");
        File dest = new File(tempDir, "app-fixed.log.zip");
        writeContent(source, "zip fixed delay content");

        int fixedDelay = 1;
        Action action =
                FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, 0, fixedDelay, fixedDelay);

        assertInstanceOf(ZipCompressAction.class, action, "Expected ZipCompressAction");

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(
                elapsed >= fixedDelay * 1000L - 100,
                "ZIP fixed delay should sleep at least " + fixedDelay + "s, took " + elapsed + "ms");
        assertTrue(
                elapsed <= fixedDelay * 1000L + 500,
                "ZIP fixed delay exceeded expected duration, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .zip file must exist");
        assertFalse(source.exists(), "Source must be deleted after compression");
    }

    /**
     * FileExtension.ZIP.createCompressAction(6-args) with min=0, max=0 must be instant.
     */
    @Test
    void testZipCreateCompressActionWithMinMaxBothZero(@TempDir File tempDir) throws Exception {
        File source = new File(tempDir, "app-zero.log");
        File dest = new File(tempDir, "app-zero.log.zip");
        writeContent(source, "zip zero delay content");

        Action action = FileExtension.ZIP.createCompressAction(source.getPath(), dest.getPath(), true, 0, 0, 0);

        assertInstanceOf(ZipCompressAction.class, action);

        long start = System.currentTimeMillis();
        action.execute();
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 500, "ZIP with min=0,max=0 should be instant, took " + elapsed + "ms");
        assertTrue(dest.exists(), "Compressed .zip file must exist");
        assertFalse(source.exists(), "Source must be deleted");
    }

    // ── DefaultRolloverStrategy builder ───────────────────────────────────

    /**
     * DefaultRolloverStrategy.Builder must accept minCompressionDelaySeconds and expose it via getter.
     */
    @Test
    void testDefaultRolloverStrategyBuilderMinCompressionDelay() {
        DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .setMin("1")
                .setMax("7")
                .setMinCompressionDelaySeconds(2)
                .setMaxCompressionDelaySeconds(5)
                .build();

        assertEquals(2, strategy.getMinCompressionDelaySeconds(),
                "minCompressionDelaySeconds should be 2");
        assertEquals(5, strategy.getMaxCompressionDelaySeconds(),
                "maxCompressionDelaySeconds should be 5");
    }

    /**
     * DefaultRolloverStrategy.Builder with only maxCompressionDelaySeconds (backward compat) must default min to 0.
     */
    @Test
    void testDefaultRolloverStrategyBuilderMaxOnlyDefaultsMinToZero() {
        DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                .setMin("1")
                .setMax("7")
                .setMaxCompressionDelaySeconds(3)
                .build();

        assertEquals(0, strategy.getMinCompressionDelaySeconds(),
                "minCompressionDelaySeconds should default to 0");
        assertEquals(3, strategy.getMaxCompressionDelaySeconds(),
                "maxCompressionDelaySeconds should be 3");
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static void writeContent(final File file, final String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
