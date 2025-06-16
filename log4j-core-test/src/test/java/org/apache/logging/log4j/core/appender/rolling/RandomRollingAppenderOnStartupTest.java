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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 */
@RunWith(Parameterized.class)
public class RandomRollingAppenderOnStartupTest {

    private static final String DIR = "target/onStartup";

    private Logger logger;

    @Parameterized.Parameters(name = "{0} \u2192 {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
            // @formatter:off
            {"log4j-test5.xml"}, {"log4j-test5.xml"},
        });
        // @formatter:on
    }

    @Rule
    public LoggerContextRule loggerContextRule;

    public RandomRollingAppenderOnStartupTest(final String configFile) {
        this.loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(configFile);
    }

    @Before
    public void setUp() {
        this.logger = this.loggerContextRule.getLogger(RandomRollingAppenderOnStartupTest.class.getName());
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (Files.exists(Paths.get("target/onStartup"))) {
            try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
                for (final Path path : directoryStream) {
                    Files.delete(path);
                }
                Files.delete(Paths.get(DIR));
            }
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        long size = 0;
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                if (size == 0) {
                    size = Files.size(path);
                } else {
                    final long fileSize = Files.size(path);
                    assertEquals(
                            "Expected size: " + size + " Size of " + path.getFileName() + ": " + fileSize,
                            size,
                            fileSize);
                }
                Files.delete(path);
            }
            Files.delete(Paths.get(DIR));
        }
    }

    @Test
    public void testAppender() {
        for (int i = 0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }
    }
}
