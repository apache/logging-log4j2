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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.logging.log4j.util.Unbox.box;
import static org.junit.jupiter.api.Assertions.assertEquals;

@LoggerContextSource(value = "log4j-test5.xml", timeout = 10)
public class RandomRollingAppenderOnStartupTest {

    private static final String DIR = "target/onStartup";

    private final Logger logger;

    public RandomRollingAppenderOnStartupTest(final LoggerContext context) {
        logger = context.getLogger(getClass());
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        if (Files.exists(Paths.get("target/onStartup"))) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
                for (final Path path : directoryStream) {
                    Files.delete(path);
                }
                Files.delete(Paths.get(DIR));
            }
        }
    }

    @AfterAll
    public static void afterClass() throws Exception {
        long size = 0;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                if (size == 0) {
                    size = Files.size(path);
                } else {
                    final long fileSize = Files.size(path);
                    assertEquals(fileSize, size, "Expected size: " + size + " Size of " + path.getFileName() + ": " + fileSize);
                }
                Files.delete(path);
            }
            Files.delete(Paths.get(DIR));
        }
    }

    @Test
    public void testAppender() throws Exception {
        for (int i = 0; i < 100; ++i) {
            logger.debug("This is test message number {}", box(i));
        }

    }
}
