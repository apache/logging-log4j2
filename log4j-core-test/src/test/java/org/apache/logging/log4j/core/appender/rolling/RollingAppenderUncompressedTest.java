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

import java.nio.file.Path;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

class RollingAppenderUncompressedTest {

    private static final String CONFIG =
            "org/apache/logging/log4j/core/appender/rolling/RollingAppenderUncompressedTest.xml";
    private static final String DIR = "target/rolling4";

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    void testAppender(final Logger logger) throws Exception {
        for (int i = 0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }
        assertThat(loggingPath)
                .as("check logging directory")
                .isNotEmptyDirectory()
                .as("check contains not compressed log file")
                .isDirectoryContaining("glob:**/test1*.log");
    }
}
