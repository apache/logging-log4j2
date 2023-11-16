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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class RollingAppenderOnStartup2Test {

    private static final String DIR = "target/rollOnStartup";
    private static final String TARGET_FILE = DIR + "/orchestrator.log";
    private static final FastDateFormat formatter = FastDateFormat.getInstance("MM-dd-yy-HH-mm-ss");
    private static final String ROLLED_FILE_PREFIX = DIR + "/orchestrator-";
    private static final String ROLLED_FILE_SUFFIX = "-1.log.gz";
    private static final String TEST_DATA = "Hello world!";

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (Files.exists(Paths.get("target/rollOnStartup"))) {
            try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
                for (final Path path : directoryStream) {
                    Files.delete(path);
                }
                Files.delete(Paths.get(DIR));
            }
        }
        // System.setProperty("log4j2.debug", "true");
        // System.setProperty("log4j2.StatusLogger.level", "trace");
        final Configuration configuration = new DefaultConfiguration();
        final Path target = Paths.get(TARGET_FILE);
        Assert.assertFalse(Files.exists(target));
        target.toFile().getParentFile().mkdirs();
        final long timeStamp = System.currentTimeMillis() - (1000 * 60 * 60 * 24);
        final String expectedDate = formatter.format(timeStamp);
        final String rolledFileName = ROLLED_FILE_PREFIX + expectedDate + ROLLED_FILE_SUFFIX;
        final Path rolled = Paths.get(rolledFileName);
        final long copied;
        try (final InputStream is = new ByteArrayInputStream(TEST_DATA.getBytes("UTF-8"))) {
            copied = Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        final long size = Files.size(target);
        assertTrue(size > 0);
        assertEquals(copied, size);
        final FileTime fileTime = FileTime.fromMillis(timeStamp);
        final BasicFileAttributeView attrs = Files.getFileAttributeView(target, BasicFileAttributeView.class);
        attrs.setTimes(fileTime, fileTime, fileTime);
        System.setProperty("log4j.configurationFile", "log4j-rollOnStartup.json");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        final long size = 0;
        /* try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                if (size == 0) {
                    size = Files.size(path);
                } else {
                    final long fileSize = Files.size(path);
                    assertTrue("Expected size: " + size + " Size of " + path.getFileName() + ": " + fileSize,
                        size == fileSize);
                }
                Files.delete(path);
            }
            Files.delete(Paths.get("target/rollOnStartup"));
        } */
    }

    @Test
    public void testAppender() throws Exception {
        final Logger logger = LogManager.getLogger(RollingAppenderOnStartup2Test.class);
        for (int i = 0; i < 10; ++i) {
            logger.debug("This is test message number " + i);
        }
    }
}
