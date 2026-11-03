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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.appender.rolling.action.internal.CompressActionSupport;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.IOUtils;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.Issue;

class RollingFileManagerTest {

    @ParameterizedTest
    @CsvSource({
        "ZIP, -1, -1",
        "ZIP, 0, 0",
        "GZ, -1, -1",
        "GZ, 0, 0",
        "DEFLATE, -1, -1",
        "DEFLATE, 0, 0",
        "BZIP2, 1, 1",
        "BZIP2, 9, 9",
        "BZIP2, -1, 9",
        "BZIP2, 0, 9",
        "ZSTD, -1, -1",
        "ZSTD, -131072, -131072",
        "ZSTD, 0, 0",
        "ZSTD, 22, 22",
        "ZIP, -2147483648, -1",
        "GZ, -2147483648, -1",
        "DEFLATE, -2147483648, -1",
        "BZIP2, -2147483648, 9",
        "ZSTD, -2147483648, 0"
    })
    @UsingStatusListener
    void testFileExtensionResolvesNativeLevels(
            final FileExtension extension,
            final int configured,
            final int expected,
            final ListStatusListener listener) {
        listener.clear();
        assertEquals(expected, extension.resolveCompressionLevel(configured));
        assertEquals(
                configured != CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL && configured != expected ? 1 : 0,
                listener.findStatusData(Level.WARN).count());
    }

    @ParameterizedTest
    @CsvSource({"false", "true"})
    @UsingStatusListener
    void testSharedStrategyResolvesEachFormatOnce(final boolean directWrite, final ListStatusListener listener) {
        final Configuration configuration = new NullConfiguration();
        final AbstractRolloverStrategy strategy = directWrite
                ? DirectWriteRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .setCompressionLevelStr("0")
                        .build()
                : DefaultRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .setCompressionLevelStr("0")
                        .build();
        listener.clear();
        strategy.initializeCompressionLevel(FileExtension.BZIP2);
        strategy.initializeCompressionLevel(FileExtension.GZ);
        strategy.initializeCompressionLevel(FileExtension.BZIP2);
        assertEquals(9, strategy.resolveCompressionLevel(FileExtension.BZIP2, 0));
        assertEquals(0, strategy.resolveCompressionLevel(FileExtension.GZ, 0));
        assertEquals(1, listener.findStatusData(Level.WARN).count());
    }

    @Test
    void testStrategiesDistinguishOmittedAndExplicitLevels() {
        final Configuration configuration = new NullConfiguration();
        assertEquals(
                CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL,
                DefaultRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .build()
                        .getCompressionLevel());
        assertEquals(
                -1,
                DefaultRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .setCompressionLevelStr("-1")
                        .build()
                        .getCompressionLevel());
        assertEquals(
                CompressActionSupport.UNSPECIFIED_COMPRESSION_LEVEL,
                DirectWriteRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .build()
                        .getCompressionLevel());
        assertEquals(
                -1,
                DirectWriteRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .setCompressionLevelStr("-1")
                        .build()
                        .getCompressionLevel());
    }

    @Test
    @UsingStatusListener
    void testReconfigurationResolvesLevelAgainstNewPattern(
            @TempDir final File tempDir, final ListStatusListener listener) {
        final Configuration configuration = new NullConfiguration();
        final RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("compression-reconfiguration")
                .setConfiguration(configuration)
                .setFileName(new File(tempDir, "active.log").getPath())
                .setFilePattern(new File(tempDir, "archive-%i.gz").getPath())
                .setPolicy(NoOpTriggeringPolicy.INSTANCE)
                .setStrategy(DefaultRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .build())
                .build();
        assertNotNull(appender);
        appender.start();
        try {
            final DefaultRolloverStrategy strategy = DefaultRolloverStrategy.newBuilder()
                    .setConfig(configuration)
                    .setCompressionLevelStr("-2")
                    .build();
            listener.clear();
            appender.getManager().setRolloverStrategy(strategy);
            appender.getManager()
                    .setPatternProcessor(new PatternProcessor(new File(tempDir, "archive-%i.zst").getPath()));
            assertEquals(-2, strategy.resolveCompressionLevel(FileExtension.ZSTD, -2));
            assertEquals(0, listener.findStatusData(Level.WARN).count());
        } finally {
            appender.stop();
        }
    }

    /**
     * Test the RollingFileManager with a custom DirectFileRolloverStrategy
     *
     * @throws IOException
     */
    @Test
    void testCustomDirectFileRolloverStrategy() throws IOException {
        class CustomDirectFileRolloverStrategy extends AbstractRolloverStrategy implements DirectFileRolloverStrategy {
            final File file;

            CustomDirectFileRolloverStrategy(final File file, final StrSubstitutor strSubstitutor) {
                super(strSubstitutor);
                this.file = file;
            }

            @Override
            public String getCurrentFileName(final RollingFileManager manager) {
                return file.getAbsolutePath();
            }

            @Override
            public void clearCurrentFileName() {
                // do nothing
            }

            @Override
            public RolloverDescription rollover(final RollingFileManager manager) throws SecurityException {
                return null; // do nothing
            }
        }

        try (final LoggerContext ctx = LoggerContext.getContext(false)) {
            final Configuration config = ctx.getConfiguration();
            final File file = File.createTempFile("RollingFileAppenderAccessTest", ".tmp");
            file.deleteOnExit();

            final RollingFileAppender appender = RollingFileAppender.newBuilder()
                    .setFilePattern("FilePattern")
                    .setName("RollingFileAppender")
                    .setConfiguration(config)
                    .setStrategy(new CustomDirectFileRolloverStrategy(file, config.getConfigurationStrSubstitutor()))
                    .setPolicy(new SizeBasedTriggeringPolicy(100))
                    .build();

            assertNotNull(appender);
            final String testContent = "Test";
            try (final RollingFileManager manager = appender.getManager()) {
                assertEquals(file.getAbsolutePath(), manager.getFileName());
                manager.writeToDestination(testContent.getBytes(StandardCharsets.US_ASCII), 0, testContent.length());
            }
            try (final Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.US_ASCII)) {
                assertEquals(testContent, IOUtils.toString(reader));
            }
        }
    }

    /**
     * Test that a synchronous action failure does not cause a rollover. Addresses Issue #1445.
     */
    @Test
    void testSynchronousActionFailure() throws IOException {
        class FailingSynchronousAction extends AbstractAction {
            @Override
            public boolean execute() {
                return false;
            }
        }
        class FailingSynchronousStrategy implements RolloverStrategy {
            @Override
            public RolloverDescription rollover(final RollingFileManager manager) throws SecurityException {
                return new RolloverDescriptionImpl(manager.getFileName(), false, new FailingSynchronousAction(), null);
            }
        }

        final Configuration configuration = new NullConfiguration();

        // Create the manager.
        final File file = File.createTempFile("testSynchronousActionFailure", "log");
        final RollingFileManager manager = RollingFileManager.getFileManager(
                file.getAbsolutePath(),
                "testSynchronousActionFailure.log.%d{yyyy-MM-dd}",
                true,
                false,
                OnStartupTriggeringPolicy.createPolicy(1),
                new FailingSynchronousStrategy(),
                null,
                PatternLayout.createDefaultLayout(),
                0,
                true,
                false,
                null,
                null,
                null,
                configuration);
        assertNotNull(manager);
        manager.initialize();

        // Log something to ensure that the existing file size is > 0
        final String testContent = "Test";
        manager.writeToDestination(testContent.getBytes(StandardCharsets.US_ASCII), 0, testContent.length());

        // Trigger rollover that will fail
        manager.rollover();

        // If the rollover fails, then the log file should be unchanged
        assertEquals(file.getAbsolutePath(), manager.getFileName());

        // The logged content should be unchanged
        assertEquals(testContent, new String(Files.readAllBytes(file.toPath()), StandardCharsets.US_ASCII));
    }

    @UsingStatusListener
    @ParameterizedTest
    @CsvSource({
        "bz2, -2, false", "bz2, 10, false",
        "deflate, -2, false", "deflate, 10, false",
        "gz, -2, false", "gz, 10, false",
        "zip, -2, false", "zip, 10, false",
        "zst, -2147483647, false", "zst, 23, false",
        "bz2, -2, true", "bz2, 10, true",
        "deflate, -2, true", "deflate, 10, true",
        "gz, -2, true", "gz, 10, true",
        "zip, -2, true", "zip, 10, true",
        "zst, -2147483647, true", "zst, 23, true"
    })
    void testUnsupportedCompressionLevelIsResolvedBeforeRollover(
            final String extension,
            final String compressionLevel,
            final boolean directWrite,
            @TempDir final File tempDir,
            final ListStatusListener statusListener)
            throws Exception {
        final File activeFile = new File(tempDir, "active.log");
        final File renamedFile = new File(tempDir, "archive-1");
        final File compressedFile = new File(tempDir, "archive-1." + extension);
        final Configuration configuration = new NullConfiguration();
        final RolloverStrategy strategy = directWrite
                ? DirectWriteRolloverStrategy.newBuilder()
                        .setMaxFiles("3")
                        .setCompressionLevelStr(compressionLevel)
                        .setConfig(configuration)
                        .build()
                : DefaultRolloverStrategy.newBuilder()
                        .setMax("2")
                        .setCompressionLevelStr(compressionLevel)
                        .setConfig(configuration)
                        .build();
        statusListener.clear();
        final RollingFileManager manager = RollingFileManager.getFileManager(
                directWrite ? null : activeFile.getPath(),
                new File(tempDir, "archive-%i." + extension).getPath(),
                true,
                false,
                NoOpTriggeringPolicy.INSTANCE,
                strategy,
                null,
                PatternLayout.createDefaultLayout(),
                0,
                true,
                false,
                null,
                null,
                null,
                configuration);
        assertNotNull(manager);
        assertEquals(
                1,
                statusListener
                        .findStatusData(Level.WARN)
                        .filter(data -> data.getFormattedStatus().contains("Unsupported")
                                && data.getFormattedStatus().contains("compression level " + compressionLevel + ";"))
                        .count());
        statusListener.clear();
        final File currentFile;
        try {
            manager.initialize();
            manager.writeToDestination("Test".getBytes(), 0, 4);
            manager.rollover();
            manager.writeToDestination("Test".getBytes(), 0, 4);
            manager.rollover();
            manager.writeToDestination("Next".getBytes(), 0, 4);
            currentFile = new File(manager.getFileName());
        } finally {
            manager.close();
        }

        assertTrue(currentFile.exists());
        assertEquals("Next", new String(Files.readAllBytes(currentFile.toPath())));
        assertFalse(renamedFile.exists());
        assertTrue(compressedFile.exists());
        assertTrue(new File(tempDir, "archive-2." + extension).exists());
        assertEquals(
                0,
                statusListener
                        .findStatusData(Level.WARN)
                        .filter(data -> data.getFormattedStatus().contains("Unsupported")
                                && data.getFormattedStatus().contains("compression level " + compressionLevel + ";")
                                && data.getFormattedStatus().contains("using default level"))
                        .count());
        try (final InputStream input = new FileInputStream(compressedFile)) {
            if ("zip".equals(extension)) {
                try (final ZipInputStream zipInput = new ZipInputStream(input)) {
                    assertNotNull(zipInput.getNextEntry());
                    assertEquals("Test", IOUtils.toString(new InputStreamReader(zipInput)));
                }
            } else {
                final String format = "bz2".equals(extension) ? "bzip2" : "zst".equals(extension) ? "zstd" : extension;
                try (final InputStream compressedInput =
                        new CompressorStreamFactory().createCompressorInputStream(format, input)) {
                    assertEquals("Test", IOUtils.toString(new InputStreamReader(compressedInput)));
                }
            }
        }
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/1645")
    void testCreateParentDir() {
        final Configuration configuration = new NullConfiguration();
        final RollingFileManager manager = RollingFileManager.getFileManager(
                null,
                "testCreateParentDir.log.%d{yyyy-MM-dd}",
                true,
                false,
                NoOpTriggeringPolicy.INSTANCE,
                DirectWriteRolloverStrategy.newBuilder()
                        .setConfig(configuration)
                        .build(),
                null,
                PatternLayout.createDefaultLayout(configuration),
                0,
                true,
                true,
                null,
                null,
                null,
                configuration);
        assertNotNull(manager);
        try {
            final File file = new File("file_in_current_dir.log");
            assertNull(file.getParentFile());
            manager.createParentDir(file);
        } catch (final Throwable t) {
            fail("createParentDir failed: " + t.getMessage());
        } finally {
            manager.close();
        }
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/2592")
    void testRolloverOfDeletedFile() throws IOException {
        final File file = File.createTempFile("testRolloverOfDeletedFile", "log");
        file.deleteOnExit();
        final String testContent = "Test";
        try (final OutputStream os =
                        new ByteArrayOutputStream(); // use a dummy OutputStream so that the real file can be deleted
                final RollingFileManager manager = new RollingFileManager(
                        null,
                        file.getAbsolutePath(),
                        "testRolloverOfDeletedFile.log.%d{yyyy-MM-dd}",
                        os,
                        true,
                        false,
                        0,
                        System.currentTimeMillis(),
                        OnStartupTriggeringPolicy.createPolicy(1),
                        DefaultRolloverStrategy.newBuilder().build(),
                        file.getName(),
                        null,
                        null,
                        null,
                        null,
                        false,
                        ByteBuffer.allocate(256))) {
            assertTrue(file.delete());
            manager.setRenameEmptyFiles(true);
            manager.rollover();
            assertEquals(file.getAbsolutePath(), manager.getFileName());
            manager.writeBytes(testContent.getBytes(StandardCharsets.US_ASCII), 0, testContent.length());
        }
        assertEquals(testContent, new String(Files.readAllBytes(file.toPath()), StandardCharsets.US_ASCII));
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3068")
    void testInitialTimeRounded() throws IOException {
        assertEquals(1755031147000L, RollingFileManager.alignMillisToSecond(1755031147000L));
        assertEquals(1755031147000L, RollingFileManager.alignMillisToSecond(1755031147123L));
        assertEquals(1755031147000L, RollingFileManager.alignMillisToSecond(1755031147499L));
        assertEquals(1755031148000L, RollingFileManager.alignMillisToSecond(1755031147500L));
        assertEquals(1755031148000L, RollingFileManager.alignMillisToSecond(1755031147999L));
        assertEquals(1755031148000L, RollingFileManager.alignMillisToSecond(1755031148000L));

        final File file = File.createTempFile("testFile", "log");
        file.deleteOnExit();

        assertEquals(0, RollingFileManager.initialFileTime(file) % 1000);
    }
}
