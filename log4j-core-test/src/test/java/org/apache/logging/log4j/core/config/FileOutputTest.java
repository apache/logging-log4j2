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
package org.apache.logging.log4j.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

/**
 * Tests the possibility to redirect status logger output to a file.
 */
public class FileOutputTest {

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testConfig() throws IOException {
        final Path logFile = loggingPath.resolve("status.log");
        assertThat(logFile).exists().isNotEmptyFile();
        // Closes the current listeners
        StatusLogger.getLogger().reset();
    }
}
