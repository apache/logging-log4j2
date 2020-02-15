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

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(Parameterized.class)
public class RollingAppenderOnStartupTest {

    private static final String DIR = "target/onStartup";

    private Logger logger;

    @Parameterized.Parameters(name = "{0} \u2192 {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                // @formatter:off
                {"log4j-test4.xml"},
                {"log4j-test4.xml"},});
                // @formatter:on
    }

    @Rule
    public LoggerContextRule loggerContextRule;

    public RollingAppenderOnStartupTest(final String configFile) {
        this.loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(configFile);
    }

    @Before
    public void setUp() throws Exception {
        this.logger = this.loggerContextRule.getLogger(RollingAppenderOnStartupTest.class.getName());
    }

    @BeforeClass
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

    @AfterClass
    public static void afterClass() throws Exception {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                List<String> lines = Files.lines(path).collect(Collectors.toList());
                assertTrue("No header present for " + path.toFile().getName(), lines.get(0).startsWith("<!DOCTYPE HTML"));
                Files.delete(path);
            }
            Files.delete(Paths.get("target/onStartup"));
        }
    }

    @Test
    public void testAppender() throws Exception {
        for (int i = 0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }

    }
}
