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

package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests {@link FileAppender}.
 */
public class FileAppenderPermissionsXmlConfigTest {

    private static final String DIR = "target/permissions1";

    private static final String CONFIG = "log4j-posix.xml";

    public static LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeTrue(FileUtils.isFilePosixAttributeViewSupported());
    }

    @Test
    public void testFilePermissions() throws Exception {
        final Logger logger = loggerContextRule.getLogger(FileAppenderPermissionsTest.class);
        for (int i = 0; i < 1000; ++i) {
            final String message = "This is test message number " + i;
            logger.debug(message);
        }
        assertEquals("rw-------", PosixFilePermissions.toString(
                    Files.getPosixFilePermissions(Paths.get("target/permissions1/AppenderTest-1.log"))));
    }


}