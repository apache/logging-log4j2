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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.junit.Test;
import org.junitpioneer.jupiter.Issue;

public class RollingFileManagerTest {

    /**
     * Test the RollingFileManager with a custom DirectFileRolloverStrategy
     *
     * @throws IOException
     */
    @Test
    public void testCustomDirectFileRolloverStrategy() throws IOException {
        class CustomDirectFileRolloverStrategy extends AbstractRolloverStrategy implements DirectFileRolloverStrategy {
            final File file;

            CustomDirectFileRolloverStrategy(final File file, final StrSubstitutor strSubstitutor) {
                super(CompressActionFactoryProvider.newInstance(null), strSubstitutor);
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
            final String actualContents = Files.readString(file.toPath(), StandardCharsets.US_ASCII);
            assertEquals(testContent, actualContents);
        }
    }

    /**
     * Test that a synchronous action failure does not cause a rollover. Addresses Issue #1445.
     */
    @Test
    public void testSynchronousActionFailure() throws IOException {
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

        // Get the initialTime of this original log file
        final long initialTime = manager.getFileTime();

        // Log something to ensure that the existing file size is > 0
        final String testContent = "Test";
        manager.writeToDestination(testContent.getBytes(StandardCharsets.US_ASCII), 0, testContent.length());

        // Trigger rollover that will fail
        manager.rollover();

        // If the rollover fails, then the size should not be reset
        assertNotEquals(0, manager.getFileSize());

        // The initialTime should not have changed
        assertEquals(initialTime, manager.getFileTime());
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/1645")
    public void testCreateParentDir() {
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
    public void testRolloverOfDeletedFile() throws IOException {
        final File file = File.createTempFile("testRolloverOfDeletedFile", "log");
        file.deleteOnExit();
        final String testContent = "Test";
        try (final OutputStream os =
                        new ByteArrayOutputStream(); // use a dummy OutputStream so that the real file can be deleted
                final RollingFileManager manager = new RollingFileManager(
                        null,
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
        assertEquals(testContent, Files.readString(file.toPath(), StandardCharsets.US_ASCII));
    }
}
