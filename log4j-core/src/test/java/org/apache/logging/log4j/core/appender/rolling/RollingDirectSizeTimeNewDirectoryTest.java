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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class RollingDirectSizeTimeNewDirectoryTest {

    private static final String CONFIG = "log4j-rolling-size-time-new-directory.xml";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-size-time-new-directory";

    public static LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @Test
    public void streamClosedError() throws Exception {

        final Logger logger =
                loggerContextRule.getLogger(RollingDirectSizeTimeNewDirectoryTest.class);

        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }
        Thread.sleep(1500);
        for (int i = 0; i < 1000; i++) {
            logger.info("nHq6p9kgfvWfjzDRYbZp");
        }

        final File logDir = new File(DIR);
        final File[] logFolders = logDir.listFiles();
        assertThat(logFolders).isNotNull();
        Arrays.sort(logFolders);

        try {

            final int minExpectedLogFolderCount = 2;
            assertThat(logFolders.length >= minExpectedLogFolderCount).describedAs("was expecting at least " + minExpectedLogFolderCount + " folders, " +
                            "found " + logFolders.length).isTrue();

            for (int logFolderIndex = 0; logFolderIndex < logFolders.length; ++logFolderIndex) {

                File logFolder = logFolders[logFolderIndex];
                File[] logFiles = logFolder.listFiles();
                assertThat(logFiles != null && logFiles.length > 0).describedAs("no files found in folder: " + logFolder).isTrue();

                final int minExpectedLogFileCount = 2;
                if (logFolderIndex > 0
                        && logFolderIndex < logFolders.length - 1) {
                    assertThat(logFiles.length >= minExpectedLogFileCount).describedAs("was expecting at least " + minExpectedLogFileCount + " files, " +
                                    "found " + logFiles.length + ": " + Arrays.toString(logFiles)).isTrue();
                }
            }

        } catch (AssertionError error) {
            System.out.format("log directory (%s) contents:%n", DIR);
            final Iterator<File> fileIterator =
                    FileUtils.iterateFilesAndDirs(
                            logDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
            int totalFileCount = 0;
            while (fileIterator.hasNext()) {
                totalFileCount++;
                final File file = fileIterator.next();
                System.out.format("-> %s (%d)%n", file, file.length());
            }
            System.out.format("total file count: %d%n", totalFileCount);
            throw new AssertionError("check failure", error);
        }

    }

}
