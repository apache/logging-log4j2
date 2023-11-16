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

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;

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
                    .withFilePattern("FilePattern")
                    .setName("RollingFileAppender")
                    .setConfiguration(config)
                    .withStrategy(new CustomDirectFileRolloverStrategy(file, config.getConfigurationStrSubstitutor()))
                    .withPolicy(new SizeBasedTriggeringPolicy(100))
                    .build();

            Assert.assertNotNull(appender);
            final String testContent = "Test";
            try (final RollingFileManager manager = appender.getManager()) {
                Assert.assertEquals(file.getAbsolutePath(), manager.getFileName());
                manager.writeToDestination(testContent.getBytes(StandardCharsets.US_ASCII), 0, testContent.length());
            }
            try (final Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.US_ASCII)) {
                Assert.assertEquals(testContent, IOUtils.toString(reader));
            }
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
        Assert.assertNotNull(manager);
        manager.initialize();

        // Get the initialTime of this original log file
        final long initialTime = manager.getFileTime();

        // Log something to ensure that the existing file size is > 0
        final String testContent = "Test";
        manager.writeToDestination(testContent.getBytes(StandardCharsets.US_ASCII), 0, testContent.length());

        // Trigger rollover that will fail
        manager.rollover();

        // If the rollover fails, then the size should not be reset
        Assert.assertNotEquals(0, manager.getFileSize());

        // The initialTime should not have changed
        Assert.assertEquals(initialTime, manager.getFileTime());
    }
}
