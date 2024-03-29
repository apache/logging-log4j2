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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.CleanFolders;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test LOG4J2-2485.
 */
public class RollingAppenderDirectWriteStartupSizeTest {

    private static final String CONFIG = "log4j-rolling-direct-startup-size.xml";

    private static final String DIR = "target/rolling-direct-startup-size";

    private static final String FILE = "size-test.log";

    private static final String MESSAGE = "test message";

    @Rule
    public LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public CleanFolders cleanFolders = new CleanFolders(false, true, 10, DIR);

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Path log = Paths.get(DIR, FILE);
        if (Files.exists(log)) {
            Files.delete(log);
        }

        Files.createDirectories(log.getParent());
        Files.createFile(log);
        Files.write(log, MESSAGE.getBytes());
    }

    @Test
    public void testRollingFileAppenderWithReconfigure() throws Exception {
        final RollingFileAppender rfAppender =
                loggerContextRule.getRequiredAppender("RollingFile", RollingFileAppender.class);
        final RollingFileManager manager = rfAppender.getManager();

        Assert.assertNotNull(manager);
        Assert.assertEquals("Existing file size not preserved on startup", MESSAGE.getBytes().length, manager.size);
    }
}
