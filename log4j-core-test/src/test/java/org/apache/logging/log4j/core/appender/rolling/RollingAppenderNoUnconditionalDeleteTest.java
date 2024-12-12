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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class RollingAppenderNoUnconditionalDeleteTest {

    private File directory;
    private Logger logger;

    @BeforeEach
    public void setUp(final LoggerContext loggerContext) {
        this.logger = loggerContext.getLogger(RollingAppenderNoUnconditionalDeleteTest.class.getName());
    }

    void testAppender() throws Exception {
        final int LINECOUNT = 18; // config has max="100"
        for (int i = 0; i < LINECOUNT; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        assertTrue(directory.exists(), "Dir " + directory + " should exist");
        assertTrue(directory.listFiles().length > 0, "Dir " + directory + " should contain files");

        int total = 0;
        for (final File file : directory.listFiles()) {
            final List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
            total += lines.size();
        }
        assertEquals(LINECOUNT - 1, total, "rolled over lines");

        cleanUp();
    }

    void cleanUp() {
        deleteDir();
        deleteDirParent();
    }

    @Test
    @LoggerContextSource(value = "log4j-rolling-with-custom-delete-unconditional1.xml", timeout = 10)
    public void RollingWithCustomUnconditionalDeleteTest1() throws Exception {
        final String dir = "target/rolling-unconditional-delete1/test";
        this.directory = new File(dir);
        testAppender();
    }

    @Test
    @LoggerContextSource(value = "log4j-rolling-with-custom-delete-unconditional2.xml", timeout = 10)
    public void RollingWithCustomUnconditionalDeleteTest2() throws Exception {
        final String dir = "target/rolling-unconditional-delete2/test";
        this.directory = new File(dir);
        testAppender();
    }

    @Test
    @LoggerContextSource(value = "log4j-rolling-with-custom-delete-unconditional3.xml", timeout = 10)
    public void RollingWithCustomUnconditionalDeleteTest3() throws Exception {
        final String dir = "target/rolling-unconditional-delete3/test";
        this.directory = new File(dir);
        testAppender();
    }

    private void deleteDir() {
        deleteDir(directory);
    }

    private void deleteDirParent() {
        deleteDir(directory.getParentFile());
    }

    private void deleteDir(final File dir) {
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            for (final File file : files) {
                file.delete();
            }
            dir.delete();
        }
    }
}
