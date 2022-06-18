/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RollingDirectTimeNewDirectoryTest extends AbstractRollingListenerTest {

    private static final String CONFIG = "log4j-rolling-folder-direct.xml";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-folder-direct";
    private final CountDownLatch rollover = new CountDownLatch(2);

    @Test
    @CleanUpDirectories(DIR)
    @LoggerContextSource(value = CONFIG, timeout = 15)
    public void streamClosedError(final LoggerContext context, @Named("RollingFile") final RollingFileManager manager) throws Exception {
        manager.addRolloverListener(this);

        final Logger logger = context.getLogger(RollingDirectTimeNewDirectoryTest.class.getName());

        for (int i = 0; i < 1000; i++) {
            currentTimeMillis.incrementAndGet();
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        currentTimeMillis.addAndGet(500);
        for (int i = 0; i < 1000; i++) {
            currentTimeMillis.incrementAndGet();
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        rollover.await();

        File logDir = new File(DIR);
        assertThat(logDir).isNotEmptyDirectory();
        File[] logFolders = logDir.listFiles();
        assertNotNull(logFolders);
        Arrays.sort(logFolders);

        try {

            final int minExpectedLogFolderCount = 2;
            assertThat(logFolders).hasSizeGreaterThanOrEqualTo(minExpectedLogFolderCount);

            for (File logFolder : logFolders) {
                File[] logFiles = logFolder.listFiles();
                if (logFiles != null) {
                    Arrays.sort(logFiles);
                }
                assertThat(logFiles).isNotEmpty();
            }

        } catch (AssertionError error) {
            StringBuilder sb = new StringBuilder(error.getMessage()).append(" log directory (").append(DIR).append(") contents: [");
            final Iterator<File> fileIterator =
                    FileUtils.iterateFilesAndDirs(
                            logDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
            int totalFileCount = 0;
            while (fileIterator.hasNext()) {
                totalFileCount++;
                final File file = fileIterator.next();
                sb.append("-> ").append(file).append(" (").append(file.length()).append(')');
                if (fileIterator.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("] total file count: ").append(totalFileCount);
            throw new AssertionFailedError(sb.toString(), error);
        }

    }

    @Override
    public void rolloverComplete(final String fileName) {
        rollover.countDown();
    }
}
