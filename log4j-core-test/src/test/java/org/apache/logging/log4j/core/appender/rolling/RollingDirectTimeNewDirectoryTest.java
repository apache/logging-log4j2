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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

class RollingDirectTimeNewDirectoryTest implements RolloverListener {

    private final CountDownLatch rollover = new CountDownLatch(2);
    private boolean isFirst = true;
    private String ignored = null;

    @TempLoggingDir
    private Path loggingPath;

    @Test
    @LoggerContextSource(timeout = 15)
    void streamClosedError(final LoggerContext context, @Named("RollingFile") final RollingFileManager manager)
            throws Exception {
        manager.addRolloverListener(this);

        final Logger logger = context.getLogger(RollingDirectTimeNewDirectoryTest.class.getName());

        for (int count = 0; count < 2; ++count) {
            final long start = System.currentTimeMillis();
            for (int i = 0; i < 50; i++) {
                logger.info("nHq6p9kgfvWfjzDRYbZp");
            }
            final long end = System.currentTimeMillis();
            if (end < start + 1000) {
                Thread.sleep(start + 1000 - end);
            }
            for (int i = 0; i < 50; i++) {
                logger.info("nHq6p9kgfvWfjzDRYbZp");
            }
        }

        rollover.await();

        assertThat(loggingPath).isNotEmptyDirectory();
        final List<Path> logFolders;
        try (final Stream<Path> files = Files.list(loggingPath)) {
            logFolders = files.sorted().toList();
        }

        final int minExpectedLogFolderCount = 2;
        assertThat(logFolders).hasSizeGreaterThanOrEqualTo(minExpectedLogFolderCount);

        for (final Path logFolder : logFolders) {
            // It is possible a log file is created at startup and by he time we get here it
            // has rolled but has no data and so was deleted.
            if (logFolder.toAbsolutePath().toString().equals(ignored)) {
                continue;
            }
            assertThat(logFolder).isNotEmptyDirectory();
        }
    }

    @Override
    public void rolloverTriggered(final String fileName) {}

    @Override
    public void rolloverComplete(final String fileName) {
        final File file = new File(fileName);
        if (isFirst && file.length() == 0) {
            isFirst = false;
            ignored = file.getParentFile().getAbsolutePath();
            return;
        }
        isFirst = false;
        rollover.countDown();
    }
}
